package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.model.Login;
import org.janssen.scoreboard.model.Token;
import org.janssen.scoreboard.model.User;
import org.janssen.scoreboard.service.repository.TokenRepository;
import org.janssen.scoreboard.service.util.PasswordHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.UUID;

/**
 * Very basic authentication service.
 *
 * @author Stephan Janssen
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticateService {

    private final Logger log = LoggerFactory.getLogger(AuthenticateService.class);

    private static final String CONFIG_PROPERTIES = "org/janssen/scoreboard/resources/config.properties";

    private final TokenRepository tokenRepository;

    public AuthenticateService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> verifyLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {

        log.debug(">>> verify login : {}:{}", username, password);

        if (username == null || username.length() == 0) {
            log.info("Username not defined:"+username);
            return ResponseEntity.badRequest().body("Username not defined");
        }

        if (password == null || password.length() == 0) {
            return ResponseEntity.badRequest().body("Password not defined");
        }

        try {
            final String user = findUser(username);
            if (user == null) {
                log.info("User does not exist:"+username);
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User does not exist");
            }

            final User foundUser = new User(username, user);
            final String hashedPassword = foundUser.getPassword();

            if (PasswordHash.validatePassword(password, hashedPassword)) {
                final Token token = new Token(UUID.randomUUID().toString());
                token.setFullName(foundUser.getFullName());
                token.setGameType(foundUser.getGameType());
                tokenRepository.save(token);

                log.info("Token created: "+token.getValue());

                return ResponseEntity.ok().body(token.getValue());
            } else {
                log.info("Invalid login credentials:"+username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid login credentials");
            }

        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        }

        log.info("Authorisation failed for "+username);
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Authorisation failed");
    }

    /**
     * Find user in property file.
     *
     * @param username the username
     * @return the related user
     * @throws IOException
     */
    private String findUser(final String username) throws IOException {
        log.debug("findUser {}", username);

        Properties prop = new Properties();

        final InputStream resourceAsStream =
                getClass().getClassLoader().getResourceAsStream(CONFIG_PROPERTIES);

        if (resourceAsStream != null) {
            prop.load(resourceAsStream);
            return prop.getProperty(username);
        } else {
            return null;
        }
    }
}
