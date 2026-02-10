package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import edu.jmaycon.cdcapp.state.CursorStore;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
class ApplicationModule {

    @Bean
    @Profile("changelog")
    CdcChangeProcessor changelogCdcProcessor(
            SparkSession sparkSession,
            FlightTicketRowMapper rowMapper,
            KafkaChangePublisher changePublisher,
            CursorStore cursorStore,
            @Value("${cdcapp.source.table}") String table,
            @Value("${cdcapp.source.changelog-view}") String changelogView) {
        return ChangelogCdcProcessor.builder()
                .sparkSession(sparkSession)
                .rowMapper(rowMapper)
                .changePublisher(changePublisher)
                .cursorStore(cursorStore)
                .table(table)
                .changelogView(changelogView)
                .build();
    }

    @Bean
    @Profile("streaming")
    CdcChangeProcessor streamingCdcProcessor(
            SparkSession sparkSession,
            FlightTicketRowMapper rowMapper,
            KafkaChangePublisher changePublisher,
            @Value("${cdcapp.source.table}") String table) {
        return StreamingCdcProcessor.builder()
                .sparkSession(sparkSession)
                .rowMapper(rowMapper)
                .changePublisher(changePublisher)
                .table(table)
                .build();
    }

    @Bean
    CdcOrchestrator cdcOrchestrator(CdcChangeProcessor changeProcessor) {
        return new CdcOrchestrator(changeProcessor);
    }
}
