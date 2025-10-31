package tetoandeggens.seeyouagainbatch.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class BatchTestConfig {

    private static final String MYSQL_CONTAINER_IMAGE = "mysql:8.0.26";

    protected static final MySQLContainer<?> MYSQL_CONTAINER;

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
}
