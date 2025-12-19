package org.fpj.UnitTests.exportimport;

import org.fpj.exportimport.application.MassTransferCsvReader;
import org.fpj.exportimport.domain.CsvError;
import org.fpj.exportimport.domain.CsvImportResult;
import org.fpj.payments.domain.MassTransfer;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MassTransferCsvReaderTest {


    @Mock
    UserService userService;
    @Mock
    User currentUser;

    @InjectMocks
    MassTransferCsvReader underTest;

    private static final String USERNAME = "testUser@web.de";

    @BeforeEach
    public void setUp(){
        underTest.setCurrentUser(currentUser);
    }

    @Test
    public void testParse() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("csv/MassenueberweisungTest.csv")){

            assertNotNull(in, "Test-CSV nicht gefunden (Classpath): csv/MassenueberweisungTest.csv");

            when(userService.findByUsername(any())).thenReturn(null);
            when(currentUser.getUsername()).thenReturn(USERNAME);

            CsvImportResult<MassTransfer> result = underTest.parse(in);

            List<MassTransfer> massTransfers = result.getRecords();
            MassTransfer firstMassTransfer = massTransfers.getFirst();
            MassTransfer lastMassTransfer = massTransfers.getLast();


            MassTransfer expectedFirst = new MassTransfer("anna.mueller@example.com", BigDecimal.valueOf(24.9), "Kaffee & Kuchen");
            MassTransfer expectedLast = new MassTransfer("oskar.mayer@example.net", BigDecimal.valueOf(199), "Kursgebühr");

            assertEquals(expectedFirst, firstMassTransfer);
            assertEquals(expectedLast, lastMassTransfer);
        }
    }

    @Test
    public void testParseWithErrors() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("csv/MassenueberweisungTest3errors.csv")){

            assertNotNull(in, "Test-CSV nicht gefunden (Classpath): csv/MassenueberweisungTest.csv");

            when(userService.findByUsername(any())).thenReturn(null);
            when(currentUser.getUsername()).thenReturn(USERNAME);

            CsvImportResult<MassTransfer> result = underTest.parse(in);

            List<CsvError> errors = result.getErrors();
            CsvError firstError = errors.getFirst();
            CsvError secondError = errors.get(1);
            CsvError thirdError = errors.get(2);
            List<MassTransfer> massTransfers = result.getRecords();
            MassTransfer firstMassTransfer = massTransfers.getFirst();
            MassTransfer lastMassTransfer = massTransfers.getLast();

            MassTransfer expectedFirst = new MassTransfer("anna.mueller@example.com", BigDecimal.valueOf(24.9), "Kaffee & Kuchen");
            MassTransfer expectedLast = new MassTransfer("oskar.mayer@example.net", BigDecimal.valueOf(199), "Kursgebühr");

            assertEquals(expectedFirst, firstMassTransfer);
            assertEquals(expectedLast, lastMassTransfer);
            assertEquals(13L, firstError.getLine());
            assertEquals(14L, secondError.getLine());
            assertEquals(20L, thirdError.getLine());
        }
    }

    @Test
    public void testParseWrongHeader() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("csv/MassenueberweisungTestWrongHeader.csv")){

            assertNotNull(in, "Test-CSV nicht gefunden (Classpath): csv/MassenueberweisungTest.csv");

            CsvImportResult<MassTransfer> result = underTest.parse(in);

            List<MassTransfer> massTransfers = result.getRecords();
            CsvError error = result.getErrors().getFirst();

            assertTrue(massTransfers.isEmpty());
            assertTrue(result.isFatal());
            assertEquals(1L, error.getLine());
        }
    }
}
