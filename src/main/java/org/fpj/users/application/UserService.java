package org.fpj.users.application;

import org.fpj.exceptions.DataNotPresentException;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.fpj.users.domain.UsernameOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByUsername(final String username){
        return userRepository.findByUsername(username).orElseThrow(() -> new DataNotPresentException("User mit Usernamen " + username + " nicht gefunden."));
    }

    public User save(final User user){
        return userRepository.save(user);
    }

    public Page<User> findContacts(User user, Pageable pageable){
        return userRepository.findContactsOrderByLastMessageDesc(user.getId(), pageable);
    }

    public List<String> usernameContaining(String username){
        return userRepository.findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(
                username).stream().map(UsernameOnly::getUsername).toList();
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}
