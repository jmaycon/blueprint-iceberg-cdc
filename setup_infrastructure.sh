#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/.infra/docker-compose.yml"
SCHEMA_PATH="$ROOT_DIR/src/main/resources/kafka-schemas/flight-tickets.avsc"

log() {
  echo "[setup_infrastructure] $*"
}

wait_for_kafka() {
  log "Waiting for Kafka to be ready..."
  local tries=0
  until docker compose -f "$COMPOSE_FILE" exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list >/dev/null 2>&1; do
    tries=$((tries + 1))
    if [ "$tries" -gt 40 ]; then
      log "Kafka did not become ready in time."
      return 1
    fi
    sleep 3
  done
  log "Kafka is ready."
}

wait_for_sqs() {
  log "Waiting for LocalStack SQS to be ready..."
  local tries=0
  until docker compose -f "$COMPOSE_FILE" exec -T localstack awslocal sqs list-queues >/dev/null 2>&1; do
    tries=$((tries + 1))
    if [ "$tries" -gt 40 ]; then
      log "LocalStack SQS did not become ready in time."
      return 1
    fi
    sleep 3
  done
  log "LocalStack SQS is ready."
}

wait_for_karapace() {
  log "Waiting for Karapace to be ready..."
  local tries=0
  until curl -sS http://localhost:8082/subjects >/dev/null 2>&1; do
    tries=$((tries + 1))
    if [ "$tries" -gt 40 ]; then
      log "Karapace did not become ready in time."
      return 1
    fi
    sleep 3
  done
  log "Karapace is ready."
}

create_sqs_queue() {
  local queue_name="flight_tickets.fifo"
  log "Creating SQS FIFO queue: $queue_name"
  docker compose -f "$COMPOSE_FILE" exec -T localstack awslocal sqs create-queue \
    --queue-name "$queue_name" \
    --attributes FifoQueue=true,ContentBasedDeduplication=true >/dev/null
}

create_kafka_topic() {
  local topic_name="flight_tickets"
  log "Creating Kafka topic: $topic_name"
  docker compose -f "$COMPOSE_FILE" exec -T kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka:9092 \
    --create \
    --if-not-exists \
    --topic "$topic_name" \
    --partitions 1 \
    --replication-factor 1 \
    --config cleanup.policy=compact >/dev/null
}

upload_schema() {
  if [ ! -f "$SCHEMA_PATH" ]; then
    log "Schema file not found: $SCHEMA_PATH"
    return 1
  fi

  local subject="flight_tickets-value"
  log "Uploading schema to Karapace subject: $subject"
  local schema
  schema=$(tr -d '\n' < "$SCHEMA_PATH" | sed 's/\\/\\\\/g; s/"/\\"/g')

  curl -sS -X POST "http://localhost:8082/subjects/${subject}/versions" \
    -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
    -d "{\"schema\":\"${schema}\"}" >/dev/null
}

log "Undeploying infrastructure..."
docker compose -f "$COMPOSE_FILE" down -v

log "Deploying infrastructure..."
docker compose -f "$COMPOSE_FILE" up -d

wait_for_kafka
wait_for_sqs
wait_for_karapace

create_kafka_topic
create_sqs_queue
upload_schema

log "Infrastructure setup complete."
