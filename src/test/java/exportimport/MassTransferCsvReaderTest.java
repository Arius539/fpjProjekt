package exportimport;

import org.fpj.exportimport.application.MassTransferCsvReader;
import org.fpj.exportimport.domain.CsvImportResult;
import org.fpj.payments.domain.MassTransfer;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void testParse(){
        final String filePath = "C:\\Users\\colin\\OneDrive - Hochschule Weserbergland\\Semester\\Semester3\\MassenueberweisungTest.csv";
        InputStream in;
        try {
            in = Files.newInputStream(Path.of(filePath));
        } catch (IOException e){
            System.out.println("Test mit den angegebenen Ressourcen nicht möglich");
            return;
        }

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
