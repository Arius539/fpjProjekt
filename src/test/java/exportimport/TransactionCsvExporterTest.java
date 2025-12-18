package exportimport;

import org.fpj.exportimport.application.TransactionCsvExporter;
import org.fpj.payments.domain.TransactionRow;
import org.fpj.payments.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParser;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParserSettings;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class TransactionCsvExporterTest {

    @TempDir
    Path tempDir;

    private final TransactionCsvExporter underTest = new TransactionCsvExporter();

    @Test
    public void testExport() throws IOException {
        TransactionRow transaction1 = new TransactionRow(1L, BigDecimal.valueOf(30), Instant.ofEpochSecond(43218756), TransactionType.UEBERWEISUNG,
                1L, "testUsername1@web.de", 2L, "testUsername2@web.de", "Pizza Fr");
        TransactionRow transaction2 = new TransactionRow(2L, BigDecimal.valueOf(20), Instant.ofEpochSecond(43218756), TransactionType.UEBERWEISUNG,
                3L, "testUsername3@web.de", 4L, "testUsername4@web.de", "Kino");
        TransactionRow transaction3 = new TransactionRow(3L, BigDecimal.valueOf(10), Instant.ofEpochSecond(43218756), TransactionType.EINZAHLUNG,
                null, null, 1L, "testUsername1@web.de", "");
        List<TransactionRow> transactionList = List.of(transaction1, transaction2, transaction3);
        Iterator<TransactionRow> transactions = transactionList.iterator();

        Path path = tempDir.resolve("TransactionCsvExport.csv");
        try (OutputStream out = Files.newOutputStream(path)){
            underTest.export(transactions, out);
        }

        String csv = Files.readString(path, StandardCharsets.UTF_8);
        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.getFormat().setDelimiter(";");
        parserSettings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(parserSettings);

        List<String[]> rows = parser.parseAll(new StringReader(csv));
        String[] headers = parser.getContext().headers();

        assertEquals("Sender", headers[2]);
        assertEquals("UEBERWEISUNG", rows.getFirst()[5]);
        assertEquals("testUsername1@web.de", rows.get(2)[1]);
        assertNull(rows.get(2)[2]);
    }

}
