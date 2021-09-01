package org.janssen.scoreboard.service;

import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Login;
import org.janssen.scoreboard.model.Token;
import org.janssen.scoreboard.model.User;
import org.janssen.scoreboard.service.util.PasswordHash;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.ResponseUtil.*;

/**
 * TODO This post should happen over HTTPS but we don't have an SSL certificate to do this :(
 *
 * @author Stephan Janssen
 */
@Singleton
@Path("/api/auth")
@Produces({MediaType.TEXT_PLAIN})
public class AuthenticateService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticateService.class.getName());

    private static final String CONFIG_PROPERTIES = "org/janssen/scoreboard/resources/config.properties";

    @Inject
    private TokenDAO tokenDAO;

    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    @Path("/payload")
    public Response loginByPayload(Login login) {
        return verifyLogin(login.getUsername(), login.getPassword());
    }

    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    @Path("/login")
    public Response verifyLogin(@QueryParam("username") String username,
                                @QueryParam("password") String password) {

        if (username == null || username.length() == 0) {
            LOGGER.info("Username not defined:"+username);
            return badRequest("Username not defined");
        }

        if (password == null || password.length() == 0) {
            return badRequest("Password not defined");
        }

        try {
            final String user = findUser(username);
            if (user == null) {
                LOGGER.info("User does not exist:"+username);
                return conflict("User does not exist");
            }

            final User foundUser = new User(username, user);
            final String hashedPassword = foundUser.getPassword();

            if (PasswordHash.validatePassword(password, hashedPassword)) {
                final Token token = new Token(UUID.randomUUID().toString());
                token.setFullName(foundUser.getFullName());
                token.setGameType(foundUser.getGameType());
                tokenDAO.create(token);

                LOGGER.info("Token created: "+token.getValue());

                return ok(token.getValue());
            } else {
                LOGGER.info("Invalid login credentials:"+username);
                return unauthorized("Invalid login credentials");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        LOGGER.info("Authorisation failed for "+username);
        return conflict("Authorisation failed");
    }

    @POST
    @Path("/password")
    public Response passwordForgotten(@QueryParam("username") String username) {

        if (username == null || username.length() == 0) {
            return badRequest("Username not defined");
        }

        try {
            final String user = findUser(username);
            if (user == null) {
                return conflict("User does not exist");
            }

            final User foundUser = new User(username, user);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return conflict("Authorisation failed");
    }

    /**
     * Find user in property file.
     *
     * @param username
     * @return
     * @throws IOException
     */
    private String findUser(final String username) throws IOException {
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
