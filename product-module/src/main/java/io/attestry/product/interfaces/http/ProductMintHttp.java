package io.attestry.product.interfaces.http;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.dto.command.MintProductCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.interfaces.http.dto.request.MintedProductRequest;
import io.attestry.product.interfaces.http.dto.response.BatchMintResponse;
import io.attestry.product.interfaces.http.dto.response.MintedProductResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/products")
public class ProductMintHttp {

    private final ProductMintUseCase mintUseCase;

    @PostMapping("/minted")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_MINT')")
    public MintedProductResponse mint(
            @CurrentActor ProductActor actor,
            @RequestBody MintedProductRequest request) {
        return MintedProductResponse.from(mintUseCase.mint(
                actor,
                new MintProductCommand(
                        actor.tenantId(),
                        request.serialNumber(),
                        request.modelId(),
                        request.modelName(),
                        request.manufacturedAt(),
                        request.productionBatch(),
                        request.factoryCode(),
                        request.componentRootHash()
                )));
    }

    @PostMapping(value = "/minted/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_BRAND_MINT')")
    public BatchMintResponse batchUpload(
            @CurrentActor ProductActor actor,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "CSV file is empty");
        }
        try {
            return BatchMintResponse.from(
                mintUseCase.batchMintFromCsv(actor, actor.tenantId(), file.getInputStream())
            );
        } catch (IOException e) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "Failed to read uploaded file");
        }
    }
}
