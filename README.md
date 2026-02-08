# Blueprint Iceberg CDC

Blueprint architecture for Change Data Capture from Iceberg materialized views streamed to Kafka as a compacted change-log topic. Built with Spark, Iceberg, and Kafka.

`blueprint-iceberg-cdc` provides a Spark-based CDC blueprint for Apache Iceberg, exposing a `create_changelog_view` procedure to generate INSERT/UPDATE/DELETE change feeds between snapshots or timestamps (with computed updates via identifier columns) for publishing ticket-level changes to Kafka.

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

**Package Structure**

This project follows feature/vertical-slice packaging for a CDC component (organized by CDC responsibilities)

- `edu.jmaycon.cdcapp.model`: domain identifiers, records, and CDC result types.
- `edu.jmaycon.cdcapp.trigger`: snapshot CDC triggers (e.g.; listener or scheduled jobs)
- `edu.jmaycon.cdcapp.source`: Iceberg source tables from which the changes will be read.
- `edu.jmaycon.cdcapp.mapping`: row-to-change mapping and schema evolution handling.
- `edu.jmaycon.cdcapp.sink`: Kafka publishing and serialization wiring.
- `edu.jmaycon.cdcapp.state`: cursor persistence (JDBC/S3).
- `edu.jmaycon.cdcapp.runtime`: orchestration, runners, and app wiring.

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

**Local UIs**
- Hue (Trino query UI): `http://localhost:8888`

**SQL Examples**
Use the following queries in Apache Hue or DBeaver by connecting to the local UI endpoint above.

```sql
SELECT * FROM iceberg.default.flight_tickets;

SELECT * FROM iceberg.default."flight_tickets$snapshots" ORDER BY committed_at DESC;

DESCRIBE iceberg.default.flight_tickets;

SHOW COLUMNS FROM iceberg.default.flight_tickets;
```

**Configuration**
- See `src/main/resources/application.yaml` for baseline settings.
- Configure Kafka brokers, topic name, and Iceberg catalog settings.

**Topic Semantics**
- Output topic is compacted.
- Records represent the latest state per key.

**Notes**
- This repository is a blueprint and may require environment-specific wiring.
