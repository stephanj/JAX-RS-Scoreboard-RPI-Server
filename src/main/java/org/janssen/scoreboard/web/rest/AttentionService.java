package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Token;
import org.janssen.scoreboard.model.type.GPIOType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.ResponseUtil.unauthorized;
import static org.janssen.scoreboard.service.util.ResponseUtil.ok;

/**
 * @author Stephan Janssen
 */
@Path("/api/attention")
@Produces({MediaType.APPLICATION_JSON})
public class AttentionService {

    private static final Logger LOGGER = Logger.getLogger(AttentionService.class.getName());

    @Inject
    private TokenDAO tokenDAO;

    @Inject
    private GPIOController gpioController;

    @Path("/")
    @GET
    public Response getAttention(@QueryParam("token") String token) {
        LOGGER.info("Triggering attention buzzer");

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        gpioController.setBuzz(GPIOType.ATTENTION);

        return ok();
    }
}
