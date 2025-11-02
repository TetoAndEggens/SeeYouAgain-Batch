package tetoandeggens.seeyouagainbatch.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class BatchTestConfig {

    private static final String MYSQL_CONTAINER_IMAGE = "mysql:8.0.26";

    protected static final MySQLContainer<?> MYSQL_CONTAINER;

    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse(MYSQL_CONTAINER_IMAGE))
            .withDatabaseName("seeyouagain_test")
            .withUsername("test")
            .withPassword("testpass")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
                "--sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION"
            )
            .withInitScript("sql/schema-mysql.sql")
            .withReuse(true);

        MYSQL_CONTAINER.start();
    }

    @Autowired
    public void setBusinessDataSource(@Qualifier("businessDataSource") DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @DynamicPropertySource
    public static void setDatabaseProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/seeyouagain_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
            MYSQL_CONTAINER.getHost(),
            MYSQL_CONTAINER.getMappedPort(3306)
        );

        registry.add("spring.datasource.business.jdbc-url", () -> jdbcUrl);
        registry.add("spring.datasource.batch.jdbc-url", () -> jdbcUrl);
    }

    protected void cleanupTestData() {
        namedParameterJdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 0");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM abandoned_animal_s3_profile");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_by_keyword");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM abandoned_animal_profile");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM abandoned_animal");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM center_location");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM breed_type");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
