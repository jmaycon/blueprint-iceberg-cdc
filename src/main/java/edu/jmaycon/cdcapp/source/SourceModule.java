package edu.jmaycon.cdcapp.source;

import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SourceModule.Properties.class)
@RequiredArgsConstructor
public class SourceModule {

    private final Properties properties;

    @Bean
    SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("cdcapp")
                .master("local[*]")
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
                .config("spark.sql.catalog.rest", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.rest.catalog-impl", "org.apache.iceberg.rest.RESTCatalog")
                .config("spark.sql.catalog.rest.uri", properties.catalogUri())
                .config("spark.sql.catalog.rest.warehouse", properties.warehouse())
                .config("spark.sql.catalog.rest.io-impl", "org.apache.iceberg.aws.s3.S3FileIO")
                .config("spark.sql.catalog.rest.s3.endpoint", properties.s3().endpoint())
                .config("spark.sql.catalog.rest.s3.path-style-access", "true")
                .config(
                        "spark.sql.catalog.rest.s3.access-key-id",
                        properties.s3().accessKey())
                .config(
                        "spark.sql.catalog.rest.s3.secret-access-key",
                        properties.s3().secretKey())
                .config("spark.sql.catalog.rest.s3.region", properties.s3().region())
                .config("spark.hadoop.fs.s3a.access.key", properties.s3().accessKey())
                .config("spark.hadoop.fs.s3a.secret.key", properties.s3().secretKey())
                .config("spark.hadoop.fs.s3a.endpoint", properties.s3().endpoint())
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                .config("spark.hadoop.fs.s3a.region", properties.s3().region())
                .getOrCreate();
    }

    @Bean
    SnapshotPlanner snapshotPlanner() {
        return new SnapshotPlanner();
    }

    @Bean
    FlightTicketRowMapper flightTicketRowMapper() {
        return new FlightTicketRowMapper();
    }

    @Bean
    IcebergTableClient icebergTableClient(SparkSession sparkSession, FlightTicketRowMapper rowMapper) {
        return new IcebergTableClient(sparkSession, rowMapper);
    }

    @Bean
    IcebergChangelogReader icebergChangelogReader(IcebergTableClient icebergTableClient) {
        return new IcebergChangelogReader(icebergTableClient, properties.table());
    }

    @Bean
    IcebergSnapshotReader icebergSnapshotReader(
            SnapshotPlanner snapshotPlanner, IcebergChangelogReader icebergChangelogReader) {
        return new IcebergSnapshotReader(snapshotPlanner, icebergChangelogReader);
    }

    @ConfigurationProperties(prefix = "cdcapp.source")
    record Properties(String catalogUri, String warehouse, String table, String changelogView, S3Config s3) {

        record S3Config(String endpoint, String region, String accessKey, String secretKey) {}
    }
}
