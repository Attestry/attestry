package io.attestry.product.application.service;

import io.attestry.product.application.port.BrandAccessValidationPort;
import io.attestry.product.application.port.LedgerOutboxPort;
import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.MintProductInput;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.repository.PassportRepository;
import io.attestry.product.domain.service.QrPublicCodeGenerator;
import io.attestry.product.domain.service.UuidV7Generator;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductMintService implements ProductMintUseCase {
    private static final Set<String> PLATFORM_ADMIN_SCOPES = Set.of(
        "SCOPE_PLATFORM_ADMIN",
        "PLATFORM_ADMIN"
    );

    private final PassportRepository passportRepository;
    private final BrandAccessValidationPort brandAccessValidationPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final UuidV7Generator uuidV7Generator;
    private final QrPublicCodeGenerator qrPublicCodeGenerator;
    private final Clock clock;
    @PersistenceContext
    private EntityManager entityManager;

    public ProductMintService(
        PassportRepository passportRepository,
        BrandAccessValidationPort brandAccessValidationPort,
        LedgerOutboxPort ledgerOutboxPort,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        UuidV7Generator uuidV7Generator,
        QrPublicCodeGenerator qrPublicCodeGenerator,
        Clock clock
    ) {
        this.passportRepository = passportRepository;
        this.brandAccessValidationPort = brandAccessValidationPort;
        this.ledgerOutboxPort = ledgerOutboxPort;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.uuidV7Generator = uuidV7Generator;
        this.qrPublicCodeGenerator = qrPublicCodeGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MintedProductResult mint(ActorContext actor, MintProductCommand command) {
        MintProductInput input = MintProductInput.of(
            command.tenantId(),
            command.serialNumber(),
            command.modelId(),
            command.modelName(),
            command.manufacturedAt(),
            command.productionBatch(),
            command.factoryCode(),
            command.componentRootHash()
        );

        if (!isPlatformAdmin(actor)) {
            brandAccessValidationPort.assertActiveBrandMembership(actor.userId(), input.tenantId());
            assertBrandMintScope(actor, input.tenantId(), input.serialNumber());
        }

        if (passportRepository.existsByTenantAndSerial(input.tenantId(), input.serialNumber())) {
            throw new ProductDomainException(
                ProductErrorCode.GENESIS_ALREADY_EXISTS,
                "MINTED genesis already exists for tenant/serial"
            );
        }

        Instant now = Instant.now(clock);
        ProductPassport passport = ProductPassport.mint(
            uuidV7Generator.nextId(),
            qrPublicCodeGenerator.nextCode(),
            uuidV7Generator.nextId(),
            input,
            now
        );
        LedgerEventEnvelope event = LedgerEventEnvelope.minted(passport, now);

        try {
            passport = passportRepository.save(passport);
            // Ensure FK-dependent JDBC writes in caller can see the minted passport immediately.
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProductDomainException(ProductErrorCode.DUPLICATE_SERIAL_NUMBER, "Duplicate serial number in tenant");
        }

        String outboxEventId;
        try {
            outboxEventId = ledgerOutboxPort.enqueue(event);
        } catch (RuntimeException ex) {
            throw new ProductDomainException(ProductErrorCode.OUTBOX_ENQUEUE_FAILED, "Failed to enqueue ledger outbox event");
        }

        return new MintedProductResult(
            passport.getAsset().getAssetId(),
            passport.getPassportId(),
            passport.getQrPublicCode(),
            outboxEventId,
            event.eventCategory(),
            event.eventAction()
        );
    }

    @Override
    @Transactional
    public BatchMintResult batchMint(ActorContext actor, String tenantId, List<MintProductCommand> commands) {
        brandAccessValidationPort.assertActiveBrandMembership(actor.userId(), tenantId);
        assertBrandMintScope(actor, tenantId, "batch");

        List<BatchMintError> errors = new ArrayList<>();
        int minted = 0;

        for (int i = 0; i < commands.size(); i++) {
            MintProductCommand cmd = commands.get(i);
            int row = i + 1;
            try {
                mintSingle(tenantId, cmd);
                minted++;
            } catch (Exception ex) {
                errors.add(new BatchMintError(row, cmd.serialNumber(), ex.getMessage()));
            }
        }

        return new BatchMintResult(commands.size(), minted, errors.size(), errors);
    }

    private void mintSingle(String tenantId, MintProductCommand command) {
        MintProductInput input = MintProductInput.of(
            tenantId,
            command.serialNumber(),
            command.modelId(),
            command.modelName(),
            command.manufacturedAt(),
            command.productionBatch(),
            command.factoryCode(),
            command.componentRootHash()
        );

        if (passportRepository.existsByTenantAndSerial(tenantId, input.serialNumber())) {
            throw new ProductDomainException(
                ProductErrorCode.GENESIS_ALREADY_EXISTS,
                "Duplicate serial number: " + input.serialNumber()
            );
        }

        Instant now = Instant.now(clock);
        ProductPassport passport = ProductPassport.mint(
            uuidV7Generator.nextId(),
            qrPublicCodeGenerator.nextCode(),
            uuidV7Generator.nextId(),
            input,
            now
        );
        LedgerEventEnvelope event = LedgerEventEnvelope.minted(passport, now);

        try {
            passportRepository.save(passport);
        } catch (DataIntegrityViolationException ex) {
            throw new ProductDomainException(ProductErrorCode.DUPLICATE_SERIAL_NUMBER, "Duplicate serial number: " + input.serialNumber());
        }

        ledgerOutboxPort.enqueue(event);
    }

    private void assertBrandMintScope(ActorContext actor, String tenantId, String serialNumber) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.BRAND_MINT,
                "mint:" + tenantId + ":" + serialNumber,
                PolicyDecisionMode.LIVE_RECHECK
            )
        );
        if (!decision.allowed()) {
            throw new ProductDomainException(ProductErrorCode.FORBIDDEN_MINT, "BRAND_MINT scope is required");
        }
    }

    private boolean isPlatformAdmin(ActorContext actor) {
        return actor.scopes() != null && actor.scopes().stream().anyMatch(PLATFORM_ADMIN_SCOPES::contains);
    }
}
