package tetoandeggens.seeyouagainbatch.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
    basePackages = {
        "tetoandeggens.seeyouagainbatch.domain",
        "tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.repository"
    },
    entityManagerFactoryRef = "businessEntityManagerFactory",
    transactionManagerRef = "businessTransactionManager"
)
public class DataSourceConfig {

    @Bean(name = "businessDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.business")
    public DataSource businessDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }

    @Primary
    @Bean(name = "batchDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.batch")
    public DataSource batchDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }

    @Primary
    @Bean(name = "businessEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean businessEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("businessDataSource") DataSource dataSource
    ) {
        return builder
            .dataSource(dataSource)
            .packages("tetoandeggens.seeyouagainbatch.domain")
            .persistenceUnit("business")
            .build();
    }

    @Primary
    @Bean(name = "businessTransactionManager")
    public PlatformTransactionManager businessTransactionManager(
        @Qualifier("businessEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager(
        @Qualifier("batchDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }


    @Bean(name = "businessNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate businessNamedParameterJdbcTemplate(
        @Qualifier("businessDataSource") DataSource dataSource
    ) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}