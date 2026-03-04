package io.attestry.ledger.infrastructure.config;

import io.attestry.ledger.domain.ledger.service.LedgerAppendDomainService;
import io.attestry.ledger.domain.ledger.service.LedgerCanonicalizer;
import io.attestry.ledger.domain.ledger.service.LedgerChainVerifier;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerDomainServiceConfig {

    @Bean
    public LedgerAppendDomainService ledgerAppendDomainService(
        LedgerCanonicalizer canonicalizer, LedgerHashService hashService) {
        return new LedgerAppendDomainService(canonicalizer, hashService);
    }

    @Bean
    public LedgerChainVerifier ledgerChainVerifier(LedgerHashService hashService) {
        return new LedgerChainVerifier(hashService);
    }
}
