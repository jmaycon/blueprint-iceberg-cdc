# Blueprint Iceberg CDC

Blueprint architecture for Change Data Capture from Iceberg materialized views streamed to Kafka as a compacted change-log topic. Built with Spark, Iceberg, and Kafka.

**What This Is**
- Reference design for producing CDC events from Iceberg tables.
- Focused on a materialized view to change-log pipeline.

**Core Components**
- Spark: reads Iceberg tables and computes changes.
- Iceberg: storage format and table metadata.
- Kafka: compacted topic for change-log events.

**High-Level Flow**
1. Spark reads Iceberg materialized view snapshots.
2. Spark derives row-level changes.
3. Changes are published to a Kafka compacted topic.

**Prerequisites**
- Java 17+
- Maven 3.9+
- Apache Spark 3.x
- Apache Iceberg
- Apache Kafka

**Quick Start**
1. Build:
   ```bash
   mvn clean package
   ```
2. Run (example):
   ```bash
   java -jar target/blueprint-iceberg-cdc.jar
   ```

**Configuration**
- See `src/main/resources/application.yaml` for baseline settings.
- Configure Kafka brokers, topic name, and Iceberg catalog settings.

**Topic Semantics**
- Output topic is compacted.
- Records represent the latest state per key.

**Notes**
- This repository is a blueprint and may require environment-specific wiring.
