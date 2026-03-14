package io.attestry.ledgerservice.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerServiceFlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(
        DataSource dataSource,
        @Value("${spring.flyway.locations:classpath:db/ledger/migration}") String[] locations,
        @Value("${spring.flyway.table:ledger_flyway_schema_history}") String historyTable,
        @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
        @Value("${spring.flyway.schemas:ledger}") String[] schemas,
        @Value("${spring.flyway.default-schema:ledger}") String defaultSchema
    ) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .table(historyTable)
            .baselineOnMigrate(baselineOnMigrate)
            .schemas(schemas)
            .defaultSchema(defaultSchema)
            .load();
    }

    @Bean
    public static BeanDefinitionRegistryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                if (!registry.containsBeanDefinition("entityManagerFactory")
                    || !registry.containsBeanDefinition("flyway")) {
                    return;
                }

                String[] dependsOn = registry.getBeanDefinition("entityManagerFactory").getDependsOn();
                if (dependsOn == null || dependsOn.length == 0) {
                    registry.getBeanDefinition("entityManagerFactory").setDependsOn("flyway");
                    return;
                }

                for (String dependency : dependsOn) {
                    if ("flyway".equals(dependency)) {
                        return;
                    }
                }

                String[] merged = new String[dependsOn.length + 1];
                System.arraycopy(dependsOn, 0, merged, 0, dependsOn.length);
                merged[dependsOn.length] = "flyway";
                registry.getBeanDefinition("entityManagerFactory").setDependsOn(merged);
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            }
        };
    }
}
