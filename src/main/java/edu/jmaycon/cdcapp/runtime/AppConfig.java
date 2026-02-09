package edu.jmaycon.cdcapp.runtime;

import edu.jmaycon.cdcapp.application.CdcChangeProcessor;
import edu.jmaycon.cdcapp.application.CdcOrchestrator;
import edu.jmaycon.cdcapp.config.CdcAppProperties;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import edu.jmaycon.cdcapp.source.IcebergChangelogReader;
import edu.jmaycon.cdcapp.source.IcebergSnapshotReader;
import edu.jmaycon.cdcapp.source.IcebergTableClient;
import edu.jmaycon.cdcapp.source.SnapshotPlanner;
import edu.jmaycon.cdcapp.state.CursorStore;
import edu.jmaycon.cdcapp.trigger.SnapshotMessageParser;
import edu.jmaycon.cdcapp.trigger.SqsSnapshotListener;
import edu.playground.avro.FlightTicketAvro;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CdcAppProperties.class)
public class AppConfig {

    @Bean
    public SparkSession sparkSession(CdcAppProperties properties) {
        CdcAppProperties.Iceberg iceberg = properties.iceberg();
        return SparkSession.builder()
                .appName("cdcapp")
                .master("local[*]")
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
                .config("spark.sql.catalog.rest", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.rest.catalog-impl", "org.apache.iceberg.rest.RESTCatalog")
                .config("spark.sql.catalog.rest.uri", iceberg.catalogUri())
                .config("spark.sql.catalog.rest.warehouse", iceberg.warehouse())
                .config("spark.sql.catalog.rest.io-impl", "org.apache.iceberg.aws.s3.S3FileIO")
                .config("spark.sql.catalog.rest.s3.endpoint", iceberg.s3Endpoint())
                .config("spark.sql.catalog.rest.s3.path-style-access", "true")
                .config("spark.sql.catalog.rest.s3.access-key-id", iceberg.s3AccessKey())
                .config("spark.sql.catalog.rest.s3.secret-access-key", iceberg.s3SecretKey())
                .config("spark.sql.catalog.rest.s3.region", iceberg.s3Region())
                .config("spark.hadoop.fs.s3a.access.key", iceberg.s3AccessKey())
                .config("spark.hadoop.fs.s3a.secret.key", iceberg.s3SecretKey())
                .config("spark.hadoop.fs.s3a.endpoint", iceberg.s3Endpoint())
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                .config("spark.hadoop.fs.s3a.region", iceberg.s3Region())
                .getOrCreate();
    }

    @Bean
    public SqsClient sqsClient(CdcAppProperties properties) {
        CdcAppProperties.Aws aws = properties.aws();
        return SqsClient.builder()
                .endpointOverride(URI.create(aws.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(aws.accessKeyId(), aws.secretAccessKey())))
                .region(Region.of(aws.region()))
                .build();
    }

    @Bean
    public String sqsQueueUrl(SqsClient sqsClient, CdcAppProperties properties) {
        return sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(properties.sqs().queueName())
                        .build())
                .queueUrl();
    }

    @Bean
    public SnapshotMessageParser snapshotMessageParser() {
        return new SnapshotMessageParser();
    }

    @Bean
    public SnapshotPlanner snapshotPlanner() {
        return new SnapshotPlanner();
    }

    @Bean
    public FlightTicketRowMapper flightTicketRowMapper() {
        return new FlightTicketRowMapper();
    }

    @Bean
    public IcebergTableClient icebergTableClient(SparkSession sparkSession, FlightTicketRowMapper rowMapper) {
        return new IcebergTableClient(sparkSession, rowMapper);
    }

    @Bean
    public IcebergChangelogReader icebergChangelogReader(
            IcebergTableClient icebergTableClient, CdcAppProperties properties) {
        return new IcebergChangelogReader(
                icebergTableClient, properties.iceberg().table());
    }

    @Bean
    public IcebergSnapshotReader icebergSnapshotReader(
            SnapshotPlanner snapshotPlanner, IcebergChangelogReader icebergChangelogReader) {
        return new IcebergSnapshotReader(snapshotPlanner, icebergChangelogReader);
    }

    @Bean
    public ProducerFactory<String, FlightTicketAvro> kafkaProducerFactory(CdcAppProperties properties) {
        Map<String, Object> config = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.kafka().bootstrapServers(),
                ProducerConfig.ACKS_CONFIG,
                "all",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class,
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                properties.kafka().schemaRegistryUrl());
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, FlightTicketAvro> kafkaTemplate(
            ProducerFactory<String, FlightTicketAvro> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaChangePublisher kafkaChangePublisher(
            KafkaTemplate<String, FlightTicketAvro> kafkaTemplate, CdcAppProperties properties) {
        return new KafkaChangePublisher(kafkaTemplate, properties.kafka().topic());
    }

    @Bean
    public CursorStore cursorStore(CdcAppProperties properties) {
        return new CursorStore(Path.of(properties.state().cursorFile()));
    }

    @Bean
    @Profile("changelog")
    public CdcChangeProcessor changelogCdcProcessor(
            SparkSession sparkSession,
            FlightTicketRowMapper rowMapper,
            KafkaChangePublisher kafkaChangePublisher,
            CursorStore cursorStore,
            CdcAppProperties properties) {
        return ChangelogCdcProcessor.builder()
                .sparkSession(sparkSession)
                .rowMapper(rowMapper)
                .changePublisher(kafkaChangePublisher)
                .cursorStore(cursorStore)
                .iceberg(properties.iceberg())
                .build();
    }

    @Bean
    @Profile("streaming")
    public CdcChangeProcessor streamingCdcProcessor(
            SparkSession sparkSession,
            FlightTicketRowMapper rowMapper,
            KafkaChangePublisher kafkaChangePublisher,
            CdcAppProperties properties) {
        return StreamingCdcProcessor.builder()
                .sparkSession(sparkSession)
                .rowMapper(rowMapper)
                .changePublisher(kafkaChangePublisher)
                .iceberg(properties.iceberg())
                .build();
    }

    @Bean
    public CdcOrchestrator cdcOrchestrator(CdcChangeProcessor changeProcessor) {
        return new CdcOrchestrator(changeProcessor);
    }

    @Bean
    public SqsSnapshotListener sqsSnapshotListener(
            SqsClient sqsClient,
            String sqsQueueUrl,
            SnapshotMessageParser snapshotMessageParser,
            CdcOrchestrator orchestrator,
            CdcAppProperties properties) {
        return SqsSnapshotListener.builder()
                .sqsClient(sqsClient)
                .queueUrl(sqsQueueUrl)
                .messageParser(snapshotMessageParser)
                .orchestrator(orchestrator)
                .properties(properties.sqs())
                .build();
    }

    static {
        System.setProperty("aws.region", "eu-central-1");
        System.setProperty("aws.accessKeyId", "admin");
        System.setProperty("aws.secretAccessKey", "admin123");
    }
}
