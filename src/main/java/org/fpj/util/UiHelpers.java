package org.fpj.util;

import javafx.concurrent.Task;
import org.fpj.users.application.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class UiHelpers {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiHelpers.class);

    public static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public static final DateTimeFormatter INSTANT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(LOCAL_ZONE);

    public static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(LOCAL_ZONE);

    public static void isValidEmail(String email) {
        if (email == null) {
            String msg = "E-Mail bzw. der Benutzername darf nicht null sein.";
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }

        if (email.isBlank()) {
            String msg = "E-Mail bzw. der Benutzername darf nicht leer sein.";
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!email.matches(LoginService.REGEX_USERNAME_VALIDATOR)) {
            String msg = "E-Mail bzw. der Benutzername hat kein gültiges Format (z.B. firstname.lastname@domain.de).";
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    public static boolean isValidEmailBool(String email) {
        if (email == null) {
            return false;
        }

        if (email.isBlank()) {
            return false;
        }

        return email.matches(LoginService.REGEX_USERNAME_VALIDATOR);
    }

    public static String usernameFromEmail(String email) {
        if (email == null || email.isBlank()) {
           return  "Unbekannter Benutzer";
        }
       return email.split("@")[0];
    }

    /** Entfernt alle leeren Zeilen und reduziert die Länge des Strings*/
    public static String truncate(String s, int max) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] lines = s.split("\\R");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                if (!sb.isEmpty()) sb.append(System.lineSeparator());
                sb.append(line);
            }
        }
        String result = sb.toString();
        return result.length() <= max ? result : result.substring(0, max) + "…";
    }

    /** Entfernt alle Zeilenumbrüche und reduziert die Länge des Strings*/
    public static String truncateFull(String s, int max) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] lines = s.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (!sb.isEmpty()) sb.append(' ');  // statt lineSeparator
                sb.append(trimmed);
            }
        }

        String result = sb.toString();
        return result.length() <= max ? result : result.substring(0, max) + "…";
    }

    /** überprüft ob amount in mit einer der möglichen / logischen Kombinationen mit formatAmound übereinstimmt*/
    public static boolean amountCheck(String amountIn, BigDecimal amountOut) {
        if (amountIn == null) return false;
        BigDecimal v = (amountOut != null ? amountOut : BigDecimal.ZERO);

        String[] candidates = {
                formatAmount(v, false, true,  false, ',', true,  '\0', false), // 1.234,57
                formatAmount(v, false, true,  false, ',', false, '\0', false), // 1234,57
                formatAmount(v, false, true,  false, ',', false, '\0', true), // 1234

                formatAmount(v, false, true,  false, '.', false, '\0', false), // 1234.57
                formatAmount(v, false, true,  false, '.', true,  '\0', false), // 1,234.57

                formatAmount(v, false, true,  true,  ',', true,  '\0', false), // 1.234,57 €
                formatAmount(v, false, true,  true,  '.', true,  '\0', false), // 1,234.57 €
                formatAmount(v, false, true,  true,  ',', false,  '\0', true), // 1234 €

                formatAmount(v, true,  true,  true,  ',', true,  '\0', false), // +1.234,57 €
                formatAmount(v, true,  true,  true,  '.', true,  '\0', false), // +1,234.57 €

                formatAmount(v, false, true,  false, ',', true,  '.',  true),  // 1.235
                formatAmount(v, false, true,  false, '.', true,  ',',  true),  // 1,235
                formatAmount(v, false, true,  true,  ',', true,  '.',  true),  // 1.235 €
                formatAmount(v, false, true,  true,  '.', true,  ',',  true)   // 1,235 €
        };

        for (String c : candidates) {
            if (amountIn.equals(c)) return true;
        }
        return false;
    }

    /**
     * Formatiert einen Betrag mit optionalem Währungszeichen sowie konfigurierbarem Vorzeichen,
     * Dezimal- und Tausendertrennzeichen.
     */
    public static String formatAmount(BigDecimal amount, boolean positiveSigned, boolean negativSigned, boolean currencySymbol, char decimalSeparatorC, boolean thousandsSeparator, char thousandsSeparatorC, boolean ignoreFractionalDigits
    ) {
        BigDecimal v = (amount != null ? amount : BigDecimal.ZERO);

        int scale = ignoreFractionalDigits ? 0 : 2;
        v = v.setScale(scale, java.math.RoundingMode.HALF_UP);

        boolean isNeg = v.signum() < 0;
        if (!negativSigned && isNeg) {
            v = v.abs();
            isNeg = false;
        }

        // Dezimalzeichen ist nur relevant, wenn Nachkommastellen ausgegeben werden.
        char decSep = (ignoreFractionalDigits ? '.' : decimalSeparatorC);
        if (decSep == '\0') decSep = ','; // Default

        char grpSepDefault = (decSep == ',') ? '.' : ',';
        char grpSep = (thousandsSeparatorC != '\0') ? thousandsSeparatorC : grpSepDefault;

        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
        sym.setDecimalSeparator(decSep);
        sym.setGroupingSeparator(grpSep);

        String pattern = ignoreFractionalDigits ? "#,##0" : "#,##0.00";
        java.text.DecimalFormat df = new java.text.DecimalFormat(pattern, sym);
        df.setRoundingMode(java.math.RoundingMode.HALF_UP);
        df.setGroupingUsed(thousandsSeparator);

        String s = df.format(v.abs());

        if (currencySymbol) {
            s = s + " €";
        }

        if (isNeg) return "-" + s;
        return (positiveSigned ? "+" : "") + s;
    }

    public static BigDecimal parseAmountTolerant(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Betrag ist erforderlich.");
        }

        String s = raw.replace("€","").replaceAll("[\\s\\u00A0]", "").trim();
        int lastComma = s.lastIndexOf(',');
        int lastDot   = s.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.');
        }

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Betrag konnte nicht gelesen werden.");
        }
    }

    public static LocalDate parseDateTolerant(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Eingabefeld darf nicht leer sein");
        }
        text = text.trim();

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd.MM.yy"),
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("d.M.yy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Bitte ein gültiges Datum eingeben, z.B. 16.11.2025 oder 2025-11-16");
            }
        }
        throw new IllegalArgumentException("Bitte ein gültiges Datum eingeben, z.B. 16.11.2025 oder 2025-11-16");
    }

    public static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public static String formatInstant(Instant t) {
        if (t == null) {
            return "";
        }
        return INSTANT_FMT.format(t);
    }

    public static String formatInstantToDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FMT.format(instant);
    }

    public static void startBackgroundTask(Task<?> task, String threadName) {
        Thread t = new Thread(task, threadName);
        t.setDaemon(true);
        t.start();
    }
}
