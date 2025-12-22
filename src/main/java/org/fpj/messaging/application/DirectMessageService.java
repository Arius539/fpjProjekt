package org.fpj.messaging.application;

import org.fpj.exceptions.DataNotPresentException;
import org.fpj.messaging.domain.ChatPreview;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.messaging.domain.DirectMessageRepository;
import org.fpj.messaging.domain.DirectMessageRow;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class DirectMessageService {

    private final DirectMessageRepository dmRepo;
    private final UserService userService;

    @Autowired
    public DirectMessageService(DirectMessageRepository dmRepo, UserService userService){
        this.dmRepo = dmRepo;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public Page<ChatPreview> getChatPreviews(User user, Pageable pageable) {
        Page<User> contactsPage = userService.findContacts(user, pageable);

        List<ChatPreview> previews = contactsPage.getContent().stream()
                .map(contact -> {
                    Optional<DirectMessage> lastOpt =
                            dmRepo.lastMessageNative(user.getId(), contact.getId());
                    if (lastOpt.isEmpty()) {
                        return new ChatPreview(contact.getUsername(),
                                null,
                                null, null);
                    }
                    DirectMessage dm = lastOpt.get();
                    String lastText = dm.getContent();
                    Instant ts = dm.getCreatedAt();
                    return new ChatPreview(contact.getUsername(), lastText,LocalDateTime.ofInstant(ts, ZoneId.systemDefault()), dm.getSender().getUsername());
                })
                .toList();
        return new PageImpl<>(previews, pageable, contactsPage.getTotalElements());
    }

    public Page<DirectMessage> getConversation(User userA, User userB, Pageable pageable) {
      return  dmRepo.findConversation(userA.getId(), userB.getId(), pageable);
    }

    @Transactional
    public DirectMessage addDirectMessage(DirectMessageRow row) {
        Long id = dmRepo.add(row.sender().getId(), row.recipient().getId(), row.content());
        return dmRepo.getDirectMessageById(id)
                .orElseThrow(() -> new DataNotPresentException("DirectMessage not found for id " + id));
    }

    public List<DirectMessage> getConversationMessageList(Long userId1, Long userId2){
        return this.dmRepo.findConversationAsList(userId1, userId2);
    }

}
