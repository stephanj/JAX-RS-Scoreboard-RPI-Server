package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.dao.GameDAO;
import org.janssen.scoreboard.dao.TeamDAO;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.Token;
import org.janssen.scoreboard.model.type.TeamType;
import org.janssen.scoreboard.service.broadcast.ProducerService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.ResponseUtil.badRequest;
import static org.janssen.scoreboard.service.util.ResponseUtil.ok;
import static org.janssen.scoreboard.service.util.ResponseUtil.unauthorized;

/**
 * The team fouls service.
 *
 * @author Stephan Janssen
 */
@Path("/api/foul")
@Produces({MediaType.APPLICATION_JSON})
public class FoulService {

    private static final Logger LOGGER = Logger.getLogger(FoulService.class.getName());

    @Inject
    private TeamDAO teamDAO;

    @Inject
    private GameDAO gameDAO;

    @Inject
    private TokenDAO tokenDAO;

    @Inject
    private DeviceController device;

    @Inject
    private ProducerService producerService;

    @GET
    @Path("/{teamId}")
    public Response getFouls(@PathParam("teamId") Long teamId) {

        if (teamId == null || teamId == 0) {
            return badRequest("Team id can't be null or zero");
        }

        final Team team = teamDAO.find(teamId);

        if (team == null) {
            return badRequest("Team does not exist");
        }

        return ok(team.getFouls());
    }

    @PUT
    @Path("/reset/{gameId}")
    public Response resetFouls(@PathParam("gameId") Long gameId,
                               @QueryParam("token") String token) {

        if (gameId == null || gameId == 0) {
            return badRequest("Game id can't be null or zero");
        }

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        final Game game = gameDAO.find(gameId);

        if (game == null) {
            return badRequest("Game does not exist");
        }

        final Team teamA = game.getTeamA();
        teamA.setFouls(0);
        update(teamA, 0);

        final Team teamB = game.getTeamB();
        teamB.setFouls(0);
        update(teamB, 0);

        return ok();
    }

    @PUT
    @Path("/inc/{teamId}/{totalFouls}")
    public Response incrementFouls(@PathParam("teamId") Long teamId,
                                   @PathParam("totalFouls") Integer totalFouls,
                                   @QueryParam("token") String token) {

        if (teamId == null || teamId == 0) {
            return badRequest("Team id can't be null or zero");
        }

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        final Team team = teamDAO.find(teamId);

        if (team == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Team does not exist:"+teamId);
            }
            return badRequest("Team does not exist");
        }

        int fouls = team.getFouls();
        if (fouls < 5) {
            team.setFouls(++fouls);

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("Team %s has %d fouls", team.getName(), team.getFouls()));
            }

            update(team, totalFouls);

        } else {
            // 5 Team fouls so only show player fouls
            device.setPlayerFoul(totalFouls);
        }
        return ok();
    }

    @PUT
    @Path("/dec/{teamId}")
    public Response decrementFouls(@PathParam("teamId") Long teamId,
                                   @QueryParam("token") String token) {

        if (teamId == null || teamId == 0) {
            return badRequest("Team id can't be null or zero");
        }

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        final Team team = teamDAO.find(teamId);

        if (team == null) {
            return badRequest("Team does not exist");
        }

        int fouls = team.getFouls();

        if (fouls > 0) {
            team.setFouls(--fouls);
            update(team, 0);
            return ok();
        } else {
            return badRequest("Fouls can't be negative");
        }
    }

    private void update(final Team team, final int totalPersonalFouls) {

        LOGGER.info("team.isMirrored? " + team.isMirrored());

        if (team.isMirrored()) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("FoulService mirroring is turned ON");
            }

            if (team.getKey().equals(TeamType.A.toString())) {
                producerService.printFoulsA(team.getFouls());
            } else {
                producerService.printFoulsB(team.getFouls());
            }

            producerService.setPlayerFoul(totalPersonalFouls);
        } else if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("FoulService mirroring is turned OFF");
        }

        teamDAO.update(team);

        device.setFoul(team);

        if (totalPersonalFouls != 0) {
            device.setPlayerFoul(totalPersonalFouls);
        }
    }
}
