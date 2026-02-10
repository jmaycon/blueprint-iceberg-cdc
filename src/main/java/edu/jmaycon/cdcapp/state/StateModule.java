package edu.jmaycon.cdcapp.state;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StateModule.Properties.class)
@RequiredArgsConstructor
class StateModule {

    private final Properties properties;

    @Bean
    CursorStore cursorStore() {
        return new CursorStore(Path.of(properties.cursorFile()));
    }

    @ConfigurationProperties(prefix = "cdcapp.state")
    static record Properties(String cursorFile) {}
}
