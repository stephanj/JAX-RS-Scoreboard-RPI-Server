package org.janssen.scoreboard.service;

import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Token;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.janssen.scoreboard.service.util.ResponseUtil.ok;

/**
 * @author Stephan Janssen
 */
@Path("/api/token")
@Produces({MediaType.APPLICATION_JSON})
public class TokenService {

    @Inject
    private TokenDAO tokenDAO;

    @Path("/list")
    @GET
    public List<Token> getTokens() {
        return tokenDAO.listTokens();
    }
}
