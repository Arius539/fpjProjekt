package org.fpj.users.application;

import org.fpj.exceptions.DataNotPresentException;
import org.fpj.exceptions.LoginFailedException;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    public static final String REGEX_USERNAME_VALIDATOR =
            "^(?!.*\\.\\.)(?=.{8,255})[A-Za-z0-9_%.+-]{2,64}@[A-Za-z0-9.-]{2,}\\.[A-Za-z]{2,}$";
    private static final String REGEX_PASSWORT_VALIDATOR =
            "^(?=.{8,127}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$";

    private final ConfigurableApplicationContext context;
    private final UserService userService;

    @Autowired
    public LoginService(ConfigurableApplicationContext context, UserService userService){
        this.context = context;
        this.userService = userService;
    }

    public void login(final String username, final String password) {
        final User user;
        try {
            user = userService.findByUsername(username.toLowerCase());
        }
        catch (DataNotPresentException e){
            throw new LoginFailedException("Kein User mit Username " + username + " vorhanden.");
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(password, user.getPasswordHash())){
            context.getBeanFactory().registerSingleton("loggedInUser", user);
        }
        else {
            throw new LoginFailedException("Passwort falsch");
        }
    }

    public void register(final String username, final String password, final String passwordCheck) {
        final String message;
        if (userService.usernameExists(username.toLowerCase())) {
            message = "Username existiert bereits";
        } else if (!username.matches(REGEX_USERNAME_VALIDATOR)) {
            message = "Username erfüllt nicht die Anforderungen";
        } else if (!passwordCheck.equals(password)) {
            message = "Passwörter stimmen nicht überein";
        }
        else if (!password.matches(REGEX_PASSWORT_VALIDATOR)) {
            message = "Passwort erfüllt nicht die Anforderungen";
        }
        else {
            final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            final String hashedPassword = passwordEncoder.encode(password);
            final User newUser = new User(username.toLowerCase(), hashedPassword);
            userService.save(newUser);
            return;
        }
        throw new LoginFailedException(message);
    }
}
