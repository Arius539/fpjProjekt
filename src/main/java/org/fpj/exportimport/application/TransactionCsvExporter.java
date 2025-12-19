package org.fpj.exportimport.application;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.Getter;
import org.fpj.util.UiHelpers;
import org.fpj.payments.domain.TransactionRow;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Getter
public class TransactionCsvExporter {
    boolean isRunning = false;

    public void export(Iterator<TransactionRow> transactions, OutputStream out) {
        this.isRunning = true;
        CsvWriterSettings settings = new CsvWriterSettings();
        settings.getFormat().setDelimiter(';');
        settings.setHeaders("Transaktionsdatum", "Empfänger", "Sender", "Beschreibung", "Betrag", "Transaktionstyp");

        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            CsvWriter writer = new CsvWriter(osw, settings);

            writer.writeHeaders();
            Iterable<TransactionRow> iterable = () -> transactions;
            for (TransactionRow t : iterable) {
                String description =  UiHelpers.safe(t.description());
                writer.writeRow(
                        UiHelpers.formatInstantToDate(t.createdAt()),
                        t.recipientUsername(),
                        t.senderUsername(),
                        UiHelpers.truncateFull(description, description.length()),
                        UiHelpers.formatAmount(t.amount(), false, false, false, ',', false, ',', false),
                        t.type().name()
                );
            }
            writer.flush();
            this.isRunning = false;
        } catch (Exception e) {
            this.isRunning = false;
            throw new RuntimeException("Fehler beim Export der Transaktionen als CSV", e);
        }
    }
}



