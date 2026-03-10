package io.attestry.product.application.service;

import io.attestry.product.application.dto.command.MintProductCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.command.ProductTenantType;
import io.attestry.product.application.dto.result.BatchMintError;
import io.attestry.product.application.dto.result.BatchMintResult;
import io.attestry.product.application.dto.result.MintedProductResult;
import io.attestry.product.application.port.LedgerOutboxPort;
import io.attestry.product.application.port.PassportPort;
import io.attestry.product.application.port.ProductAuthorizationPort;
import io.attestry.product.application.port.TenantContextAccessPort;
import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.MintProductInput;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.service.QrPublicCodeGenerator;
import io.attestry.product.domain.service.UuidV7Generator;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
@Service
public class ProductMintService implements ProductMintUseCase {

    private static final String PLATFORM_ADMIN_ROLE = "ADMIN";
    private static final String BRAND_ROLE = "BRAND";
    private static final String BATCH_RESOURCE_REF = "batch";

    private final PassportPort passportPort;
    private final TenantContextAccessPort tenantContextAccessPort;
    private final ProductAuthorizationPort productAuthorizationPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final UuidV7Generator uuidV7Generator;
    private final QrPublicCodeGenerator qrPublicCodeGenerator;
    private final ProductMintCsvParser csvParser;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public MintedProductResult mint(ProductActor actor, MintProductCommand command) {
        MintProductInput input = toInput(command.tenantId(), command);
        assertMintAccess(actor, input.tenantId(), input.serialNumber());

        MintExecution execution = mintAndEnqueue(input, resolveLedgerActor(actor, input.tenantId()));
        return toMintedProductResult(execution);
    }

    @Override
    public BatchMintResult batchMintFromCsv(ProductActor actor, String tenantId, InputStream csvStream) {
        List<MintProductCommand> commands = csvParser.parse(tenantId, csvStream);
        return batchMint(actor, tenantId, commands);
    }

    @Override
    public BatchMintResult batchMint(ProductActor actor, String tenantId, List<MintProductCommand> commands) {
        assertBatchMintAccess(actor, tenantId);
        LedgerActor ledgerActor = resolveLedgerActor(actor, tenantId);

        List<BatchMintError> errors = new ArrayList<>();
        int minted = 0;

        for (int i = 0; i < commands.size(); i++) {
            MintProductCommand cmd = commands.get(i);
            int row = i + 1;
            try {
                transactionTemplate.executeWithoutResult(status -> mintSingle(tenantId, cmd, ledgerActor));
                minted++;
            } catch (Exception ex) {
                errors.add(new BatchMintError(row, cmd.serialNumber(), ex.getMessage()));
            }
        }

        return new BatchMintResult(commands.size(), minted, errors.size(), errors);
    }

    private void assertMintAccess(ProductActor actor, String tenantId, String serialNumber) {
        if (actor.platformAdmin()) {
            return;
        }
        tenantContextAccessPort.assertActiveTenantMembership(actor.userId(), tenantId, ProductTenantType.BRAND);
        productAuthorizationPort.assertBrandMintAllowed(actor, tenantId, serialNumber);
    }

    private void assertBatchMintAccess(ProductActor actor, String tenantId) {
        tenantContextAccessPort.assertActiveTenantMembership(actor.userId(), tenantId, ProductTenantType.BRAND);
        productAuthorizationPort.assertBrandMintAllowed(actor, tenantId, BATCH_RESOURCE_REF);
    }

    private void mintSingle(String tenantId, MintProductCommand command, LedgerActor ledgerActor) {
        MintProductInput input = toInput(tenantId, command);
        mintAndEnqueue(input, ledgerActor);
    }

    private MintExecution mintAndEnqueue(MintProductInput input, LedgerActor ledgerActor) {
        assertNotDuplicate(input.tenantId(), input.serialNumber());
        ProductPassport passport = createAndSavePassport(input);
        LedgerEventEnvelope event = LedgerEventEnvelope.minted(
            passport,
            ledgerActor.actorRole(),
            ledgerActor.actorId(),
            passport.getCreatedAt()
        );
        String outboxEventId = ledgerOutboxPort.enqueue(event);
        return new MintExecution(passport, event, outboxEventId);
    }

    private ProductPassport createAndSavePassport(MintProductInput input) {
        Instant now = Instant.now(clock);
        ProductPassport passport = ProductPassport.mint(
            uuidV7Generator.nextId(),
            qrPublicCodeGenerator.nextCode(),
            uuidV7Generator.nextId(),
            input,
            now
        );
        try {
            passport = passportPort.save(passport);
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProductDomainException(
                ProductErrorCode.DUPLICATE_SERIAL_NUMBER,
                "Duplicate serial number: " + input.serialNumber()
            );
        }
        return passport;
    }

    private MintedProductResult toMintedProductResult(MintExecution execution) {
        ProductPassport passport = execution.passport();
        LedgerEventEnvelope event = execution.event();
        return new MintedProductResult(
            passport.getAsset().getAssetId(),
            passport.getPassportId(),
            passport.getQrPublicCode(),
            execution.outboxEventId(),
            event.eventCategory(),
            event.eventAction()
        );
    }

    private LedgerActor resolveLedgerActor(ProductActor actor, String tenantId) {
        if (actor.platformAdmin()) {
            return new LedgerActor(PLATFORM_ADMIN_ROLE, actor.userId());
        }
        return new LedgerActor(BRAND_ROLE, tenantId);
    }

    private MintProductInput toInput(String tenantId, MintProductCommand command) {
        return MintProductInput.of(
            tenantId,
            command.serialNumber(),
            command.modelId(),
            command.modelName(),
            command.manufacturedAt(),
            command.productionBatch(),
            command.factoryCode(),
            command.componentRootHash()
        );
    }

    private void assertNotDuplicate(String tenantId, String serialNumber) {
        if (passportPort.existsByTenantAndSerial(tenantId, serialNumber)) {
            throw new ProductDomainException(
                ProductErrorCode.GENESIS_ALREADY_EXISTS,
                "MINTED genesis already exists for tenant/serial"
            );
        }
    }

    private record LedgerActor(String actorRole, String actorId) {
    }

    private record MintExecution(ProductPassport passport, LedgerEventEnvelope event, String outboxEventId) {
    }
}
