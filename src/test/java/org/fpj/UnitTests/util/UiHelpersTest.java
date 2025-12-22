package org.fpj.UnitTests.util;

import org.fpj.util.UiHelpers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.fpj.util.UiHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

public class UiHelpersTest {

    public static String EMAIL = "testUser@web.de";

    //Der Regex wird hier nicht explizit getestet, da er bereits im LoginServiceTest getestet wird
    @Test
    public void testIsValidEMail(){
        UiHelpers.isValidEmail(EMAIL);
        assertThrows(IllegalArgumentException.class, () -> UiHelpers.isValidEmail(""));
        assertThrows(IllegalArgumentException.class, () -> isValidEmail(null));
        assertTrue(isValidEmailBool(EMAIL));
        assertFalse(isValidEmailBool(""));
        assertFalse(isValidEmailBool(null));
    }
    @Test
    public void testUsernameFromEmail(){
        final String unknownUser = "Unbekannter Benutzer";
        assertEquals("testUser", UiHelpers.usernameFromEmail(EMAIL));
        assertEquals(unknownUser, UiHelpers.usernameFromEmail(""));
        assertEquals(unknownUser, UiHelpers.usernameFromEmail(null));
    }

    @Test
    public void testTruncate(){
        final String emailToTruncate = """
                                 
                \ttestUser@web.de 
                \t               
                """;
        assertEquals("\t" + EMAIL, UiHelpers.truncate(emailToTruncate, 20));
        assertEquals("\ttestU" + "…", UiHelpers.truncate(emailToTruncate, 6));
        assertEquals(EMAIL, UiHelpers.truncateFull(emailToTruncate, 20));
        assertEquals("testUs" + "…", UiHelpers.truncateFull(emailToTruncate, 6));
    }

    @Test
    public void testFormatAmount(){
        BigDecimal v = BigDecimal.valueOf(1234.57);

        assertEquals("1.234,57", formatAmount(v, false, true, false, ',', true, '\0', false));
        assertEquals("1234,57", formatAmount(v, false, true,  false, ',', false, '\0', false));
        assertEquals("1235", formatAmount(v, false, true,  false, ',', false, '\0', true));//anderes Ergebnis
        assertEquals("1234.57", formatAmount(v, false, true,  false, '.', false, '\0', false));
        assertEquals("1,234.57", formatAmount(v, false, true,  false, '.', true,  '\0', false));
        assertEquals("1234,57 €", formatAmount(v, false, true, true, ',', false, '.', false));
        assertEquals("1,234.57 €", formatAmount(v, false, true,  true,  '.', true,  '\0', false));
        assertEquals("1235 €", formatAmount(v, false, true,  true,  ',', false,  '\0', true));//anderes Ergebnis
        assertEquals("+1.234,57 €", formatAmount(v, true,  true,  true,  ',', true,  '\0', false));
        assertEquals("+1,234.57 €", formatAmount(v, true,  true,  true,  '.', true,  '\0', false));
        assertEquals("1.235", formatAmount(v, false, true,  false, ',', true,  '.',  true));
        assertEquals("1,235", formatAmount(v, false, true,  false, '.', true,  ',',  true));
        assertEquals("1.235 €", formatAmount(v, false, true,  true,  ',', true,  '.',  true));
        assertEquals("1,235 €", formatAmount(v, false, true,  true,  '.', true,  ',',  true));
    }

    @Test
    public void testParseAmountTolerant(){
        assertEquals(0, UiHelpers.parseAmountTolerant("31,42€").compareTo(BigDecimal.valueOf(31.42)));
        assertEquals(0, UiHelpers.parseAmountTolerant("3.100,45€").compareTo(BigDecimal.valueOf(3100.45)));
        assertEquals(0, UiHelpers.parseAmountTolerant("2,456.3000€").compareTo(BigDecimal.valueOf(2456.3)));
        assertEquals(0, UiHelpers.parseAmountTolerant("01€").compareTo(BigDecimal.valueOf(1)));
    }

    @Test
    public void testAmountCheck(){
        assertTrue(UiHelpers.amountCheck("31,42 €", UiHelpers.parseAmountTolerant("31,42 €")));
        assertTrue(UiHelpers.amountCheck("3.100,42", UiHelpers.parseAmountTolerant("3.100,42")));
        assertTrue(UiHelpers.amountCheck("34 €", BigDecimal.valueOf(34.23)));
        assertFalse(UiHelpers.amountCheck("34 €", BigDecimal.valueOf(34.57)));
    }

    @Test
    public void testParseDateTolerant(){
        LocalDate date = LocalDate.of(2025, 12, 18);
        LocalDate date2 = LocalDate.of(2025, 1, 9);
        assertEquals(date, UiHelpers.parseDateTolerant("18.12.2025"));
        assertEquals(date, UiHelpers.parseDateTolerant("18.12.25"));
        assertEquals(date2, UiHelpers.parseDateTolerant("9.1.2025"));
        assertEquals(date2, UiHelpers.parseDateTolerant("9.1.25"));
        assertThrows(IllegalArgumentException.class, () -> UiHelpers.parseDateTolerant("2025/1/9"));
    }
}
