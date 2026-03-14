package io.attestry.product.application.command;

import io.attestry.product.application.dto.command.MintProductCommand;
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

    private static final int EXPECTED_COLUMN_COUNT = 7;

    private static final String[] EXPECTED_HEADERS = {
        "serial_number", "model_id", "model_name", "manufactured_at",
        "production_batch", "factory_code", "component_root_hash"
    };

    public List<MintProductCommand> parse(String tenantId, InputStream csvStream) {
        List<MintProductCommand> commands = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            validateHeader(readHeader(reader));
            readCommands(tenantId, reader, commands);
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

    private String readHeader(BufferedReader reader) throws Exception {
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "CSV file has no header");
        }
        return headerLine;
    }

    private void readCommands(String tenantId, BufferedReader reader, List<MintProductCommand> commands) throws Exception {
        String line;
        int row = 1;
        while ((line = reader.readLine()) != null) {
            row++;
            if (line.isBlank()) {
                continue;
            }
            commands.add(parseRow(tenantId, row, line));
        }
    }

    private MintProductCommand parseRow(String tenantId, int row, String line) {
        String[] columns = splitColumns(row, line);
        RowValues rowValues = toRowValues(columns);
        validateRequiredFields(row, rowValues);

        return new MintProductCommand(
            tenantId,
            rowValues.serialNumber(),
            rowValues.modelId(),
            rowValues.modelName(),
            parseManufacturedAt(row, rowValues.manufacturedAt()),
            rowValues.productionBatch(),
            rowValues.factoryCode(),
            rowValues.componentRootHash()
        );
    }

    private String[] splitColumns(int row, String line) {
        String[] columns = line.split(",", -1);
        if (columns.length < EXPECTED_COLUMN_COUNT) {
            throw new ProductDomainException(
                ProductErrorCode.INVALID_REQUEST,
                "Row " + row + ": expected 7 columns, got " + columns.length
            );
        }
        return columns;
    }

    private RowValues toRowValues(String[] columns) {
        return new RowValues(
            requiredTrim(columns[0]),
            blankToNull(trim(columns[1])),
            requiredTrim(columns[2]),
            requiredTrim(columns[3]),
            blankToNull(trim(columns[4])),
            blankToNull(trim(columns[5])),
            blankToNull(trim(columns[6]))
        );
    }

    private void validateRequiredFields(int row, RowValues rowValues) {
        if (rowValues.serialNumber().isEmpty() || rowValues.modelName().isEmpty() || rowValues.manufacturedAt().isEmpty()) {
            throw new ProductDomainException(
                ProductErrorCode.INVALID_REQUEST,
                "Row " + row + ": serial_number, model_name, manufactured_at are required"
            );
        }
    }

    private Instant parseManufacturedAt(int row, String manufacturedAt) {
        try {
            return Instant.parse(manufacturedAt);
        } catch (Exception e) {
            throw new ProductDomainException(
                ProductErrorCode.INVALID_REQUEST,
                "Row " + row + ": invalid manufactured_at format (expected ISO-8601, e.g. 2026-01-15T00:00:00Z)"
            );
        }
    }

    private void validateHeader(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        if (headers.length < EXPECTED_COLUMN_COUNT) {
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

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requiredTrim(String value) {
        return trim(value);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    private record RowValues(
        String serialNumber,
        String modelId,
        String modelName,
        String manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash
    ) {
    }
}
