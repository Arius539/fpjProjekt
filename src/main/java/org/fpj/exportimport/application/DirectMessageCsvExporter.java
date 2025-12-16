package org.fpj.exportimport.application;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.Getter;
import org.fpj.util.UiHelpers;
import org.fpj.messaging.domain.DirectMessage;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
@Getter
public class DirectMessageCsvExporter {
    boolean isRunning = false;

    public void export(Iterator<DirectMessage> messages, OutputStream out) {
        isRunning = true;
        CsvWriterSettings settings = new CsvWriterSettings();
        settings.getFormat().setDelimiter(';');
        settings.setHeaders("Zeitpunkt der Nachricht", "Sender", "Empfänger", "Nachricht");
        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            CsvWriter writer = new CsvWriter(osw, settings);

            writer.writeHeaders();
            Iterable<DirectMessage> iterable = () -> messages;
            for (DirectMessage n : iterable) {
                String description = UiHelpers.safe(n.getContent());
                writer.writeRow(
                        formatInstant(n.getCreatedAt()),
                        n.getSender().getUsername(),
                        n.getRecipient().getUsername(),
                        UiHelpers.truncateFull(description, description.length()));
            }
            writer.flush();
            isRunning = false;
        } catch (Exception e) {
            isRunning = false;
            throw new RuntimeException("Fehler beim Export der Direktnachrichten als CSV", e);
        }
    }

    private String formatInstant(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(UiHelpers.LOCAL_ZONE);
        return formatter.format(instant);
    }
}
