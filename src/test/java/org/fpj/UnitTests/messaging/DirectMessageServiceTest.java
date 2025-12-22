package org.fpj.UnitTests.messaging;

import org.fpj.messaging.application.DirectMessageService;
import org.fpj.messaging.domain.ChatPreview;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.messaging.domain.DirectMessageRepository;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DirectMessageServiceTest {

    @Mock
    DirectMessageRepository dmRepo;
    @Mock
    UserService userService;

    @InjectMocks
    DirectMessageService underTest;

    @Mock
    User currentUser;
    @Mock
    User contact1;
    @Mock
    User contact2;
    @Mock
    Pageable pageable;

    private static final Long CURRENT_USER_ID = 437850102L;
    private static final Long CONTACT1_USER_ID = 3214958L;
    private static final Long CONTACT2_USER_ID = 343578091L;
    private static final String USERNAME_CURRENT_USER = "testuser1@web.de";
    private static final String USERNAME_C1 = "testuser2@gmx.de";
    private static final String USERNAME_C2 = "testuser3@gmail.com";


    @Test
    public void testGetChatPreviews(){
        LocalDateTime localDateTime = LocalDateTime.of(2025, 12, 11, 13, 30);
        ZoneId zone = ZoneId.of("Europe/Berlin");
        Instant time = localDateTime.atZone(zone).toInstant();
        final String text1 = "Hallo wie geht es dir?";
        final String text2 = "Hallo wo bleibt mein Geld!?";
        List<User> content = List.of(contact1, contact2);
        final DirectMessage lastMessageWithC1 = new DirectMessage(null, currentUser, contact1, text1, time);
        final DirectMessage lastMessageWithC2 = new DirectMessage(null, contact2, currentUser, text2, time);
        Page<User> contactsPage = new PageImpl<>(content);

        when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
        when(contact1.getId()).thenReturn(CONTACT1_USER_ID);
        when(contact2.getId()).thenReturn(CONTACT2_USER_ID);
        when(currentUser.getUsername()).thenReturn(USERNAME_CURRENT_USER);
        when(contact1.getUsername()).thenReturn(USERNAME_C1);
        when(contact2.getUsername()).thenReturn(USERNAME_C2);
        when(userService.findContacts(currentUser, pageable)).thenReturn(contactsPage);
        when(dmRepo.lastMessageNative(CURRENT_USER_ID, CONTACT1_USER_ID)).thenReturn(Optional.of(lastMessageWithC1));
        when(dmRepo.lastMessageNative(CURRENT_USER_ID, CONTACT2_USER_ID)).thenReturn(Optional.of(lastMessageWithC2));

        Page<ChatPreview> chatPreviews = underTest.getChatPreviews(currentUser, pageable);
        long totalElements = chatPreviews.getTotalElements();
        List<ChatPreview> chatPreviewList = chatPreviews.getContent();
        ChatPreview firstChatPreview = chatPreviewList.getFirst();
        ChatPreview secondChatPreview = chatPreviewList.get(1);

        assertEquals(2, totalElements);
        assertEquals(text1, firstChatPreview.lastMessage());
        assertEquals(USERNAME_CURRENT_USER, firstChatPreview.lastMessageUsername());
        assertEquals(USERNAME_C1, firstChatPreview.name());
        assertEquals(text2, secondChatPreview.lastMessage());
        assertEquals(USERNAME_C2, secondChatPreview.lastMessageUsername());
        assertEquals(USERNAME_C2, secondChatPreview.name());
    }

}
