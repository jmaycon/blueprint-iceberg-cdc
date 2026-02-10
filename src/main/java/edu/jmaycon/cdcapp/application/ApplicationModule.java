package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import edu.jmaycon.cdcapp.state.CursorStore;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApplicationModule.Properties.class)
@RequiredArgsConstructor
class ApplicationModule {

    private final Properties properties;

    @Bean
    ChangelogCdcProcessor changelogCdcProcessor(
            SparkSession sparkSession,
            FlightTicketRowMapper rowMapper,
            KafkaChangePublisher changePublisher,
            CursorStore cursorStore) {
        return ChangelogCdcProcessor.builder()
                .sparkSession(sparkSession)
                .rowMapper(rowMapper)
                .changePublisher(changePublisher)
                .cursorStore(cursorStore)
                .table(properties.table())
                .changelogView(properties.changelogView())
                .build();
    }

    @Bean
    CdcOrchestrator cdcOrchestrator(ChangelogCdcProcessor changeProcessor) {
        return new CdcOrchestrator(changeProcessor);
    }

    @ConfigurationProperties(prefix = "cdcapp.source")
    record Properties(String table, String changelogView) {}
}
