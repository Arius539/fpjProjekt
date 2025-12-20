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

    @Test
    public void testGetChatPreviews(){
        final String text1 = "Hallo wie geht es dir?";
        final String text2 = "Hallo wo bleibt mein Geld!?";
        List<User> content = List.of(contact1, contact2);
        final DirectMessage lastMessageWithC1 = new DirectMessage(null, currentUser, contact1, text1, Instant.ofEpochSecond(2348756));
        final DirectMessage lastMessageWithC2 = new DirectMessage(null, contact2, currentUser, text2, Instant.ofEpochSecond(43785));
        Page<User> contactsPage = new PageImpl<>(content);

        when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
        when(contact1.getId()).thenReturn(CONTACT1_USER_ID);
        when(contact2.getId()).thenReturn(CONTACT2_USER_ID);
        when(userService.findContacts(currentUser, pageable)).thenReturn(contactsPage);
        when(dmRepo.lastMessageNative(CURRENT_USER_ID, CONTACT1_USER_ID)).thenReturn(Optional.of(lastMessageWithC1));
        when(dmRepo.lastMessageNative(CURRENT_USER_ID, CONTACT2_USER_ID)).thenReturn(Optional.of(lastMessageWithC2));

        Page<ChatPreview> chatPreviews = underTest.getChatPreviews(currentUser, pageable);
        long totalElements = chatPreviews.getTotalElements();
        List<ChatPreview> chatPreviewList = chatPreviews.getContent();
        ChatPreview firstChatPreview = chatPreviewList.get(0);
        ChatPreview secondChatPreview = chatPreviewList.get(1);

        assertEquals(2, totalElements);
        assertEquals(text1, firstChatPreview.lastMessage());
        assertEquals(text2, secondChatPreview.lastMessage());
    }

    //TODO: Fall Zeile 39-45 testen

}
