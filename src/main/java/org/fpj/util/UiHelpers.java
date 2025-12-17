package org.fpj.util;

import javafx.concurrent.Task;
import org.fpj.users.application.LoginService;

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

    public static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public static final DateTimeFormatter INSTANT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(LOCAL_ZONE);

    public static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(LOCAL_ZONE);

    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private static final int MAX_LENGTH_EMAIL = 320;

    public static void isValidEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("E-Mail bzw. der Benutzername darf nicht null sein.");
        }

        if (email.isBlank()) {
            throw new IllegalArgumentException("E-Mail bzw. der Benutzername darf nicht leer sein.");
        }

        if (!email.matches(LoginService.REGEX_USERNAME_VALIDATOR)) {
            throw new IllegalArgumentException("E-Mail bzw. der Benutzername hat kein gültiges Format (z.B. firstname.lastname@domain.de).");
        }
    }

    public static boolean isValidEmailBool(String email) {
        if (email == null) {
            return false;
        }

        if (email.isBlank()) {
            return false;
        }

        if (!email.matches(LoginService.REGEX_USERNAME_VALIDATOR)) {
            return false;
        }
        return true;
    }

    public static String usernameFromEmail(String email) {
        if (email == null || email.isBlank()) {
           return  "Unbekannter Benutzer";
        }
       return   email.split("@")[0];
    }

    /** Entfernt alle leeren Zeilen und reduziert die Länge des Strings*/
    public static String truncate(String s, int max) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] lines = s.split("\\R");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(System.lineSeparator());
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
                if (sb.length() > 0) sb.append(' ');  // statt lineSeparator
                sb.append(trimmed);
            }
        }

        String result = sb.toString();
        return result.length() <= max ? result : result.substring(0, max) + "…";
    }


    /**Punkt als Decimal Trennzeichen, kein Währungszeichen und kein Vorzeichen*/
    public static String formatBigDecimal(BigDecimal amt) {
        BigDecimal v = (amt != null ? amt : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return v.toPlainString();
    }


    /**Komma als Decimal Trennzeichen und kein Währungszeichen und kein Vorzeichen*/
    public static String formatEuro(BigDecimal amt) {
        BigDecimal v = (amt != null ? amt : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return EUR.format(v);
    }

    /**Komma als Decimal Trennzeichen und Währungszeichen und Vorzeichen*/
    public static String formatSignedEuro(BigDecimal amt) {
        BigDecimal v = (amt != null ? amt : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String s = EUR.format(v.abs());
        return (v.signum() < 0 ? "-" : "+") + " " + s;
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
