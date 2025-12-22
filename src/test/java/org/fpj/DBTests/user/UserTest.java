package org.fpj.DBTests.user;

import org.fpj.payments.application.TransactionService;
import org.fpj.users.application.LoginService;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class UserTest {

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
    TransactionService transactionService;
    @Autowired
    UserService userService;
    @Autowired
    LoginService loginService;
    @Autowired
    ApplicationContext context;

    private static final String USERNAME = "testUserA@web.de";
    private static final String PASSWORD = "pass@Word25";

    private static final String USERNAME2 = "testUserB@gmail.com";
    private static final String USERNAME3 = "creativeName@t-online.de";

    @BeforeEach
    public void setUp(){
        userRepo.deleteAll();
    }

    @Test
    public void testRegisterAndLogin(){
        loginService.register(USERNAME, PASSWORD, PASSWORD);
        loginService.login(USERNAME, PASSWORD);

        User user = context.getBean("loggedInUser", User.class);
        BigDecimal balance = transactionService.computeBalance(user.getId());

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        assertEquals(USERNAME.toLowerCase(), user.getUsername());
        assertTrue(passwordEncoder.matches(PASSWORD, user.getPasswordHash()));
        assertEquals(0, balance.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testUsernameContaining(){
        loginService.register(USERNAME, PASSWORD, PASSWORD);
        loginService.register(USERNAME2, PASSWORD, PASSWORD);
        loginService.register(USERNAME3, PASSWORD, PASSWORD);

        List<String> names = userService.usernameContaining("testUser");

        System.out.println(names);

        assertEquals(2, names.size());
        assertEquals(USERNAME.toLowerCase(), names.getFirst());
        assertEquals(USERNAME2.toLowerCase(), names.get(1));
    }

}
