package io.attestry.product.application.service;

import io.attestry.product.application.usecase.ProductMintUseCase.MintProductCommand;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductMintCsvParser {

    private static final String[] EXPECTED_HEADERS = {
        "serial_number", "model_id", "model_name", "manufactured_at",
        "production_batch", "factory_code", "component_root_hash"
    };

    public List<MintProductCommand> parse(String tenantId, InputStream csvStream) {
        List<MintProductCommand> commands = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "CSV file has no header");
            }
            validateHeader(headerLine);

            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 7) {
                    throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST,
                        "Row " + row + ": expected 7 columns, got " + cols.length);
                }

                String serialNumber = cols[0].trim();
                String modelId = blankToNull(cols[1].trim());
                String modelName = cols[2].trim();
                String manufacturedAtStr = cols[3].trim();
                String productionBatch = blankToNull(cols[4].trim());
                String factoryCode = blankToNull(cols[5].trim());
                String componentRootHash = blankToNull(cols[6].trim());

                if (serialNumber.isEmpty() || modelName.isEmpty() || manufacturedAtStr.isEmpty()) {
                    throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST,
                        "Row " + row + ": serial_number, model_name, manufactured_at are required");
                }

                Instant manufacturedAt;
                try {
                    manufacturedAt = Instant.parse(manufacturedAtStr);
                } catch (Exception e) {
                    throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST,
                        "Row " + row + ": invalid manufactured_at format (expected ISO-8601, e.g. 2026-01-15T00:00:00Z)");
                }

                commands.add(new MintProductCommand(
                    tenantId, serialNumber, modelId, modelName,
                    manufacturedAt, productionBatch, factoryCode, componentRootHash, null
                ));
            }

        } catch (ProductDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "Failed to parse CSV: " + e.getMessage());
        }

        if (commands.isEmpty()) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "CSV file has no data rows");
        }

        return commands;
    }

    private void validateHeader(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        if (headers.length < 7) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST,
                "Invalid CSV header: expected columns " + String.join(",", EXPECTED_HEADERS));
        }
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(headers[i].trim())) {
                throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST,
                    "Invalid CSV header at column " + (i + 1) + ": expected '" + EXPECTED_HEADERS[i] + "', got '" + headers[i].trim() + "'");
            }
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
