#!/bin/sh
set -e

echo "[init-resources] Creating MinIO S3 bucket: analytics-warehouse"
aws --endpoint-url http://minio:9000 s3 mb s3://analytics-warehouse --region eu-central-1 || echo "Bucket already exists"

echo "[init-resources] Waiting for services to be ready..."
sleep 10

echo "[init-resources] Creating Kafka topic: flight_tickets"
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:9092 \
  --create \
  --if-not-exists \
  --topic flight_tickets \
  --partitions 1 \
  --replication-factor 1 \
  --config cleanup.policy=compact

echo "[init-resources] Creating SQS FIFO queue: flight_tickets.fifo"
aws sqs create-queue \
  --endpoint-url http://localstack:4566 \
  --queue-name flight_tickets.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true

echo "[init-resources] Waiting for Karapace to be ready..."
until curl -sf http://kafka-schema-registry:8081/subjects > /dev/null 2>&1; do
  echo "[init-resources] Karapace not ready, retrying..."
  sleep 3
done

echo "[init-resources] Uploading Avro schema to Karapace"
SCHEMA_JSON=$(cat /schemas/flight-tickets.avsc | jq -Rs '{schema: .}')
curl -X POST http://kafka-schema-registry:8081/subjects/flight_tickets-value/versions \
  -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
  -d "$SCHEMA_JSON"

echo "[init-resources] Verifying schema registration"
curl -sf http://kafka-schema-registry:8081/subjects | grep -q "flight_tickets-value"

echo "[init-resources] Resource initialization complete"
