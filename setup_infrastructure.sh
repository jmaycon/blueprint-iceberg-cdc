#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/.infra/docker-compose.yml"
SCHEMA_PATH="$ROOT_DIR/src/main/resources/kafka-schemas/flight-tickets.avsc"
KAFKA_HOST="localhost"
KAFKA_PORT="9092"
KARAPACE_HOST="localhost"
KARAPACE_PORT="8081"
LOCALSTACK_HOST="localhost"
LOCALSTACK_PORT="4566"

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
  until curl -sSf "http://${KARAPACE_HOST}:${KARAPACE_PORT}/subjects" >/dev/null 2>&1; do
    tries=$((tries + 1))
    if [ "$tries" -gt 40 ]; then
      log "Karapace did not become ready in time."
      return 1
    fi
    sleep 3
  done
  log "Karapace is ready."
}

wait_for_karapace_master() {
  log "Waiting for Karapace to settle master election..."
  sleep 6
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
  local subject="flight_tickets-value"
  if curl -sS "http://${KARAPACE_HOST}:${KARAPACE_PORT}/subjects" | grep -q "\"${subject}\""; then
    log "Schema subject already exists, skipping upload: ${subject}"
    return 0
  fi
  log "Uploading schema to Karapace subject: $subject"

  if [ ! -f "$SCHEMA_PATH" ]; then
    log "Schema file not found: $SCHEMA_PATH"
    return 1
  fi

  local payload
  payload=$(jq -Rs '{schema: .}' "$SCHEMA_PATH")

  curl -sS -X POST "http://${KARAPACE_HOST}:${KARAPACE_PORT}/subjects/${subject}/versions" \
    -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
    --data-binary "$payload"
}



verify_schema_registered() {
  local subject="flight_tickets-value"
  log "Verifying schema is registered (subject: ${subject})"
  local subjects
  subjects=$(curl -sS "http://${KARAPACE_HOST}:${KARAPACE_PORT}/subjects")
  if echo "$subjects" | grep -q "\"${subject}\""; then
    log "Schema subject found: ${subject}"
    return 0
  fi
  log "Schema subject not found after upload."
  echo "$subjects"
  return 1
}

log "Undeploying infrastructure..."
#docker compose -f "$COMPOSE_FILE" down -v

log "Deploying infrastructure..."
docker compose -f "$COMPOSE_FILE" up -d

wait_for_kafka
wait_for_sqs
wait_for_karapace
wait_for_karapace_master

create_kafka_topic
create_sqs_queue
upload_schema
verify_schema_registered

log "Infrastructure setup complete."
