package org.fpj.exportimport.application;

import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.Setter;
import org.fpj.exceptions.CsvRowRejectedException;
import org.fpj.util.UiHelpers;
import org.fpj.exceptions.DataNotPresentException;
import org.fpj.exportimport.domain.CsvError;
import org.fpj.exportimport.domain.CsvImportResult;
import org.fpj.exportimport.domain.CsvReader;
import org.fpj.payments.domain.MassTransfer;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@Setter
public class MassTransferCsvReader implements CsvReader<MassTransfer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MassTransferCsvReader.class);

    UserService userService;
    private boolean isRunningB = false;
    User currentUser;

    @Override
    public CsvImportResult<MassTransfer> parse(InputStream in) {
        this.isRunningB = true;
        CsvParserSettings settings = createBaseSettings();
        CsvParser parser = new CsvParser(settings);

        List<CsvError> errors = new ArrayList<>();
        List<MassTransfer> records = new ArrayList<>();

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            parser.beginParsing(reader);

            long line = 1L;

            String[] actualHeaders = parser.getContext().headers();
            String[] expectedHeaders = new String[]{"Empfänger", "Betrag", "Beschreibung"};

            if (!Arrays.equals(actualHeaders, expectedHeaders)) {
                String msg = "Unerwarteter Header. Erwartet: " + String.join(";", expectedHeaders) + " aber gefunden: " + String.join(";", actualHeaders != null ? actualHeaders : new String[0]);
                LOGGER.error(msg);
                CsvError fatal = new CsvError(line, null, null, null, msg, CsvError.Severity.FATAL);
                return new CsvImportResult<>(List.of(), List.of(fatal), true);
            }

            Record record;
            while ((record = parser.parseNextRecord()) != null) {
                line++;
                try {
                    MassTransfer eintrag = mapMassenUeberweisung(record, line, errors);
                    records.add(eintrag);
                }
                catch (CsvRowRejectedException e){
                    LOGGER.warn("Fehler in Zeile {} erkannt und protokolliert.", line);
                }
                catch (Exception ex) {
                    LOGGER.error("Unerwarteter Fehler in Zeile {}.", line);
                    errors.add(new CsvError(line, null, null, null, "Unerwarteter Fehler in dieser Zeile: " + ex.getMessage(), CsvError.Severity.ERROR));
                }
            }
            boolean fatal = false;
            return new CsvImportResult<>(records, errors, fatal);
        } catch (TextParsingException tpe) {
            String message = "CSV-Strukturfehler: " + tpe.getMessage();
            CsvError fatalError = new CsvError(tpe.getLineIndex() + 1L, null, null, null, message, CsvError.Severity.FATAL);
            LOGGER.error(message);
            return new CsvImportResult<>(List.of(), List.of(fatalError), true);
        } catch (IOException ioe) {
            String message = "CSV konnte nicht gelesen werden: " + ioe.getMessage();
            CsvError fatalError = new CsvError(0, null, null, null, message, CsvError.Severity.FATAL);
            LOGGER.error(message);
            return new CsvImportResult<>(List.of(), List.of(fatalError), true);
        } finally {
            this.isRunningB = false;
            parser.stopParsing();
        }
    }

    @Override
    public boolean isRunning() {
        return isRunningB;
    }

    private CsvParserSettings createBaseSettings() {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        settings.setLineSeparatorDetectionEnabled(true);
        settings.getFormat().setDelimiter(';');
        settings.setSkipEmptyLines(true);
        settings.setAutoConfigurationEnabled(false);
        return settings;
    }

    private MassTransfer mapMassenUeberweisung(Record r, long line, List<CsvError> errors) {
        String empfaenger = r.getString("Empfänger");
        String rawBetrag = r.getString("Betrag");
        String beschreibung = r.getString("Beschreibung");

        validateEmpfaenger(empfaenger, line, errors);

        BigDecimal betrag = parseBigDecimal(rawBetrag, "Betrag", line, errors);
        if (betrag == null) {
            throw new CsvRowRejectedException("Ungültiger Betrag: " + rawBetrag);
        }else if (betrag.compareTo(BigDecimal.ZERO) <= 0) {
            String msg = "Betrag muss größer als 0 sein";
            LOGGER.warn(msg);
            errors.add(new CsvError(line, "Betrag", null, rawBetrag, msg, CsvError.Severity.ERROR));
            throw new CsvRowRejectedException(msg);
        }

        return new MassTransfer(empfaenger, betrag, beschreibung);
    }

    private void validateEmpfaenger(String empfaenger, long line, List<CsvError> errors) {
        try {
            if (empfaenger == null || empfaenger.isBlank()) {
                throw new IllegalArgumentException("Empfänger darf nicht leer sein");
            }
            empfaenger = empfaenger.trim();
            UiHelpers.isValidEmail(empfaenger);
            if (this.currentUser.getUsername().equals(empfaenger)) {
                throw new IllegalArgumentException("Du kannst keine Überweisungen an dich selbst tätigen");
            }
            userService.findByUsername(empfaenger);
        } catch (IllegalArgumentException | DataNotPresentException e) {
            errors.add(new CsvError(line, "Empfänger", null, empfaenger, e.getMessage(), CsvError.Severity.ERROR));
            throw new CsvRowRejectedException("Ungültiger Empfänger: " + empfaenger);
        }
    }

    private BigDecimal parseBigDecimal(String raw, String column, long line, List<CsvError> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add(new CsvError(line, column, null, raw, "Betrag darf nicht leer sein", CsvError.Severity.ERROR));
            return null;
        }
        try {
            return UiHelpers.parseAmountTolerant(raw);
        } catch (NumberFormatException ex) {
            errors.add(new CsvError(line, column, null, raw, "Ungültiger Betrag: " + raw, CsvError.Severity.ERROR));
            return null;
        }
    }
}
