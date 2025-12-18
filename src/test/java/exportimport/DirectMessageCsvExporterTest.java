package exportimport;

import org.fpj.exportimport.application.DirectMessageCsvExporter;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.users.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParser;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParserSettings;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DirectMessageCsvExporterTest {

    @TempDir
    Path tempDir;

    @Mock
    User currentUser;
    @Mock
    User contact1;
    @Mock
    User contact2;
    @Mock
    User contact3;

    private final DirectMessageCsvExporter underTest = new DirectMessageCsvExporter();

    private static final String USERNAME_CURRENT_USER = "testUser1@web.de";
    private static final String USERNAME_C1 = "testUser2@gmail.com";
    private static final String USERNAME_C2 = "testUser3@web.de";
    private static final String USERNAME_C3 = "testUser4@gmx.de";

    @Test
    public void testExport() throws IOException {
        final String text1 = "Hallo wie geht es dir?";
        final String text2 = "Hallo wo bleibt mein Geld!?";
        final String text3 = "Kannst du mir bitte das Geld für Kino schicken";
        final String text4 = "Achso ja mache ich sofort";
        final DirectMessage messageWithC1 = new DirectMessage(1L, currentUser, contact1, text1, Instant.ofEpochSecond(2348756));
        final DirectMessage messageWithC2 = new DirectMessage(2L, contact2, currentUser, text2, Instant.ofEpochSecond(43785));
        final DirectMessage messageWithC3 = new DirectMessage(3L, currentUser, contact3, text3, Instant.ofEpochSecond(437856));
        final DirectMessage secondMessageWithC3 = new DirectMessage(4L, contact3, currentUser, text4, Instant.ofEpochSecond(598642));
        Iterator<DirectMessage> messages = List.of(messageWithC1, messageWithC2, messageWithC3, secondMessageWithC3).iterator();

        Path path = tempDir.resolve("MessagesCsvExport.csv");

        when(currentUser.getUsername()).thenReturn(USERNAME_CURRENT_USER);
        when(contact1.getUsername()).thenReturn(USERNAME_C1);
        when(contact2.getUsername()).thenReturn(USERNAME_C2);
        when(contact3.getUsername()).thenReturn(USERNAME_C3);

        try (OutputStream out = Files.newOutputStream(path)){
            underTest.export(messages, out);
        }

        String csv = Files.readString(path, StandardCharsets.UTF_8);
        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.getFormat().setDelimiter(";");
        parserSettings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(parserSettings);

        List<String[]> rows = parser.parseAll(new StringReader(csv));
        String[] headers = parser.getContext().headers();

        assertEquals("Empfänger", headers[2]);
        assertEquals(text3, rows.get(2)[3]);
        assertEquals(USERNAME_C1, rows.getFirst()[2]);
        assertEquals(text4, rows.getLast()[3]);
    }

}
