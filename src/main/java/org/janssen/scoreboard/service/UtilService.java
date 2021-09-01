package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.dao.GameDAO;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Token;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.ResponseUtil.*;

/**
 * @author Stephan Janssen
 */
@Singleton
@Path("/api/util")
@Produces({MediaType.APPLICATION_JSON})
public class UtilService {

    private static final Logger LOGGER = Logger.getLogger(UtilService.class.getName());

    @Inject
    private GameDAO gameDAO;

    @Inject
    private TokenDAO tokenDAO;

    @Inject
    private DeviceController device;


    @Path("/ping")
    @GET
    public Response ping() {
        return ok();
    }

    @Path("/version")
    @GET
    public Response version() {
        String version = "Version 2.0 - 29 Jan 2015";
        LOGGER.info(version);
        return ok(version);
    }

    @Path("/clear")
    @GET
    public Response clearGameboard() {
        device.clearBoard();
        return ok();
    }

    @Path("/redraw/{gameId}")
    @PUT
    public Response redrawGameboard(@PathParam("gameId") final Long gameId,
                                    @QueryParam("token") String token) {

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        if (gameId == null || gameId == 0) {
            return badRequest("Game can't by null or zero");
        }

        final Game game = gameDAO.find(gameId);

        if (game != null) {
            device.setClockOnly(game.getClock());

            device.setScore(game.getTeamA());
            device.setScore(game.getTeamB());

            device.setPlayerFoul(game.getQuarter());

            device.setFoul(game.getTeamA());
            device.setFoul(game.getTeamB());

            return ok();
        } else {
            return badRequest("No games exist");
        }
    }

    @Path("/turnoff")
    @PUT
    public Response turnOffScoreBoard(@QueryParam("token") String token) {

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        LOGGER.info("Turnoff scoreboard");

        device.turnOff();

        return ok();
    }
}
