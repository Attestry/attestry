package io.attestry.product.interfaces.http;

import io.attestry.product.application.service.ProductMintCsvParser;
import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.application.usecase.ProductQueryUseCase;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.security.CurrentActor;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/products")
public class ProductMintHttp {

    private final ProductMintUseCase mintUseCase;
    private final ProductQueryUseCase queryUseCase;
    private final ProductMintCsvParser csvParser;

    public ProductMintHttp(
            ProductMintUseCase mintUseCase,
            ProductQueryUseCase queryUseCase,
            ProductMintCsvParser csvParser) {
        this.mintUseCase = mintUseCase;
        this.queryUseCase = queryUseCase;
        this.csvParser = csvParser;
    }

    @PostMapping("/minted")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_MINT')")
    public MintedProductResponse mint(
            @CurrentActor ActorContext actor,
            @RequestBody MintedProductRequest request) {
        ProductMintUseCase.MintedProductResult result = mintUseCase.mint(
                actor,
                new ProductMintUseCase.MintProductCommand(
                        actor.tenantId(),
                        request.serialNumber(),
                        request.modelId(),
                        request.modelName(),
                        request.manufacturedAt(),
                        request.productionBatch(),
                        request.factoryCode(),
                        request.componentRootHash()));
        return MintedProductResponse.from(result);
    }

    @PostMapping(value = "/minted/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_MINT')")
    public BatchMintResponse batchUpload(
            @CurrentActor ActorContext actor,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "CSV file is empty");
        }
        try {
            List<ProductMintUseCase.MintProductCommand> commands = csvParser.parse(actor.tenantId(),
                    file.getInputStream());
            ProductMintUseCase.BatchMintResult result = mintUseCase.batchMint(actor, actor.tenantId(), commands);
            return BatchMintResponse.from(result);
        } catch (IOException e) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "Failed to read uploaded file");
        }
    }

    @GetMapping("/minted/passports")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public PagedMintedPassportResponse listMintedPassports(
            @CurrentActor ActorContext actor,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        ProductQueryUseCase.PagedMintedPassportResponse result = queryUseCase.listMintedPassports(actor.tenantId(),
                page, size);
        List<MintedPassportResponse> content = result.content().stream()
                .map(r -> new MintedPassportResponse(
                        r.passportId(), r.qrPublicCode(),
                        r.assetId(), r.serialNumber(), r.modelId(), r.modelName(),
                        r.manufacturedAt(), r.assetState(), r.riskFlag(),
                        r.ownerId(), r.createdAt()))
                .toList();
        return new PagedMintedPassportResponse(content, result.page(), result.size(), result.totalElements(),
                result.totalPages());
    }

    public record MintedProductRequest(
            String serialNumber,
            String modelId,
            String modelName,
            Instant manufacturedAt,
            String productionBatch,
            String factoryCode,
            String componentRootHash) {
    }

    public record MintedProductResponse(
            String assetId,
            String passportId,
            String qrPublicCode,
            String outboxEventId,
            String ledgerEventCategory,
            String ledgerEventAction) {
        static MintedProductResponse from(ProductMintUseCase.MintedProductResult result) {
            return new MintedProductResponse(
                    result.assetId(),
                    result.passportId(),
                    result.qrPublicCode(),
                    result.outboxEventId(),
                    result.ledgerEventCategory(),
                    result.ledgerEventAction());
        }
    }

    public record BatchMintResponse(
            int totalRequested,
            int totalMinted,
            int totalFailed,
            List<BatchMintErrorResponse> errors) {
        static BatchMintResponse from(ProductMintUseCase.BatchMintResult result) {
            List<BatchMintErrorResponse> errors = result.errors().stream()
                    .map(e -> new BatchMintErrorResponse(e.row(), e.serialNumber(), e.reason()))
                    .toList();
            return new BatchMintResponse(result.totalRequested(), result.totalMinted(), result.totalFailed(), errors);
        }
    }

    public record BatchMintErrorResponse(int row, String serialNumber, String reason) {
    }

    public record MintedPassportResponse(
            String passportId,
            String qrPublicCode,
            String assetId,
            String serialNumber,
            String modelId,
            String modelName,
            Instant manufacturedAt,
            String assetState,
            String riskFlag,
            String ownerId,
            Instant createdAt) {
    }

    public record PagedMintedPassportResponse(
            List<MintedPassportResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}
