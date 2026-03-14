package io.attestry.ledgerservice;

import io.attestry.commonlib.CommonLibraryMarker;
import io.attestry.ledger.application.LedgerApplicationMarker;
import io.attestry.ledger.infrastructure.LedgerInfrastructureMarker;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerChainJpaEntity;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerChainJpaRepository;
import io.attestry.ledger.interfaces.LedgerInterfacesMarker;
import io.attestry.ledgerservice.outbox.persistence.LedgerOutboxEventJpaEntity;
import io.attestry.ledgerservice.outbox.persistence.LedgerOutboxEventJpaRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EntityScan(basePackageClasses = {
    LedgerChainJpaEntity.class,
    LedgerOutboxEventJpaEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
    LedgerChainJpaRepository.class,
    LedgerOutboxEventJpaRepository.class
})
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackageClasses = {
    CommonLibraryMarker.class,
    LedgerApplicationMarker.class,
    LedgerInfrastructureMarker.class,
    LedgerInterfacesMarker.class,
    LedgerServiceApplication.class
})
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
