package com.github.curiousoddman.curious_tunes.config;

import com.github.curiousoddman.curious_tunes.backend.ExceptionTranslator;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {
    private final Environment env;

    @Bean
    public DataSource getDataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public DataSourceConnectionProvider connectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider
                (new TransactionAwareDataSourceProxy(dataSource));
    }

    @Bean
    public DefaultDSLContext dsl(DataSource dataSource, ExceptionTranslator exceptionTranslator) {
        return new DefaultDSLContext(configuration(dataSource, exceptionTranslator));
    }

    public DefaultConfiguration configuration(DataSource dataSource, ExceptionTranslator exceptionTranslator) {
        DefaultConfiguration jooqConfiguration = new DefaultConfiguration();
        jooqConfiguration.set(connectionProvider(dataSource));
        jooqConfiguration.set(new DefaultExecuteListenerProvider(exceptionTranslator));
        return jooqConfiguration;
    }
}
