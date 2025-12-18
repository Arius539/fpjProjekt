package org.fpj.UnitTests.exportimport;

import org.fpj.exportimport.application.WallCommentCsvExporter;
import org.fpj.users.domain.User;
import org.fpj.wall.domain.WallComment;
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

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class WallCommentCsvExporterTest {

    @TempDir
    Path tempDir;

    @Mock
    User currentUser;
    @Mock
    User user2;
    @Mock
    User user3;
    @Mock
    User user4;

    private final WallCommentCsvExporter underTest = new WallCommentCsvExporter();

    private static final String USERNAME_CURRENT_USER = "testUser1@web.de";
    private static final String USERNAME_U2 = "testUser2@gmail.com";
    private static final String USERNAME_U3 = "testUser3@web.de";
    private static final String USERNAME_U4 = "testUser4@gmx.de";

    @Test
    public void testExport() throws IOException {
        final String text1 = "Das sieht richtig gut aus";
        final String text2 = "Wo hast du das her?";
        final String text3 = "Dieser User ist eine Katastrope! Ich warte seit 5 Wochen auf mein Geld!";
        WallComment wallComment1 = new WallComment(1L, currentUser, user2, text1, Instant.ofEpochSecond(342785610));
        WallComment wallComment2 = new WallComment(2L, currentUser, user3, text2, Instant.ofEpochSecond(3425879620346L));
        WallComment wallComment3 = new WallComment(3L, currentUser, user4, text3, Instant.ofEpochSecond(324587623084L));
        Iterator<WallComment> messages = List.of(wallComment1, wallComment2, wallComment3).iterator();

        Path path = tempDir.resolve("WallCommentCsvExport.csv");

        when(currentUser.getUsername()).thenReturn(USERNAME_CURRENT_USER);
        when(user2.getUsername()).thenReturn(USERNAME_U2);
        when(user3.getUsername()).thenReturn(USERNAME_U3);
        when(user4.getUsername()).thenReturn(USERNAME_U4);

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
        assertEquals(text2, rows.get(1)[3]);
        assertEquals(USERNAME_U3, rows.get(1)[1]);
        assertEquals(text3, rows.getLast()[3]);
    }
}
