package org.fpj.DBTests.messaging;

import org.fpj.messaging.application.DirectMessageService;
import org.fpj.messaging.domain.ChatPreview;
import org.fpj.messaging.domain.DirectMessageRepository;
import org.fpj.messaging.domain.DirectMessageRow;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class DirectMessageTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16").withDatabaseName("testdb")
                    .withUsername("test").withPassword("test").withInitScript("db/init.sql");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r){
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    UserRepository userRepo;
    @Autowired
    DirectMessageRepository directMessageRepo;
    @Autowired
    DirectMessageService directMessageService;

    private Long currentUserId;
    private Long contact1Id;
    private Long contact2Id;
    private Long contact3Id;

    private static final String USERNAME_CURRENT_USER = "testuser1@web.de";
    private static final String USERNAMEC1 = "testuser2@gmx.de";
    private static final String USERNAMEC2 = "random@gmail.com";
    private static final String USERNAMEC3 = "somename@web.de";

    private static final String TEXT1 = "Hallo wie geht es dir?";
    private static final String TEXT2 = "Bitte überweise mein Geld bis morgen";
    private static final String TEXT3 = "Hallo. Sende mir bitte deine Adresse, damit ich das Paket versenden kann";
    private static final String TEXT4 = "Mir geht es gut. Wie geht's dir?";
    private static final String TEXT5 = "Kein Problem bekommst du noch heute Abend";

    private final User currentUser = new User(USERNAME_CURRENT_USER, "password123");
    private final User contact1 = new User(USERNAMEC1, "password456");
    private final User contact2 = new User(USERNAMEC2, "wordpass123");
    private final User contact3 = new User(USERNAMEC3, "wordpass456");

    @BeforeEach
    public void setUp(){
        directMessageRepo.deleteAll();
        userRepo.deleteAll();

        userRepo.save(currentUser);
        userRepo.save(contact1);
        userRepo.save(contact2);
        userRepo.save(contact3);

        currentUserId = currentUser.getId();
        contact1Id = contact1.getId();
        contact2Id = contact2.getId();
        contact3Id = contact3.getId();
    }

    @Test
    public void testGetChatPreviews(){
        makeContacts();
        PageRequest pageRequest = PageRequest.of(0, 5);

        Page<ChatPreview> chatPreviewPage = directMessageService.getChatPreviews(currentUser, pageRequest);
        long size = chatPreviewPage.getTotalElements();
        ChatPreview first = chatPreviewPage.getContent().getFirst();
        ChatPreview second = chatPreviewPage.getContent().get(1);

        assertEquals(3L, size);
        assertEquals(USERNAMEC2, first.name());
        assertEquals(TEXT5, first.lastMessage());
        assertEquals(USERNAME_CURRENT_USER, first.lastMessageUsername());
        assertEquals(USERNAMEC1, second.name());
        assertEquals(TEXT4, second.lastMessage());
        assertEquals(USERNAMEC1, second.lastMessageUsername());
    }

    private void makeContacts(){
        DirectMessageRow dm1 = new DirectMessageRow(currentUser, contact1, TEXT1);
        DirectMessageRow dm2 = new DirectMessageRow(contact2, currentUser, TEXT2);
        DirectMessageRow dm3 = new DirectMessageRow(currentUser, contact3, TEXT3);
        DirectMessageRow dm4 = new DirectMessageRow(contact1, currentUser, TEXT4);
        DirectMessageRow dm5 = new DirectMessageRow(currentUser, contact2, TEXT5);
        List<DirectMessageRow> dmList = List.of(dm1, dm2, dm3, dm4, dm5);
        for (DirectMessageRow dm : dmList){
            directMessageService.addDirectMessage(dm);
        }
    }
}
