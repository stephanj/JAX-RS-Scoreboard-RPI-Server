package org.janssen.scoreboard.service;

/**
 * @author Stephan Janssen
 */

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.dao.TeamDAO;
import org.janssen.scoreboard.dao.TokenDAO;
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
 * The score service.
 *
 * @author Stephan Janssen
 */
@Path("/api/score")
@Produces({MediaType.APPLICATION_JSON})
public class ScoreService {

    private static final Logger LOGGER = Logger.getLogger(ScoreService.class.getName());

    @Inject
    private TeamDAO teamDAO;

    @Inject
    private TokenDAO tokenDAO;

    @Inject
    private DeviceController device;

    @Inject
    private ProducerService producerService;

    @GET
    @Path("/{teamId}")
    public Response getScore(@PathParam("teamId") Long teamId) {

        if (teamId == null || teamId == 0) {
            return badRequest("Team id can't be null or zero");
        }

        final Team team = teamDAO.find(teamId);

        if (team == null) {
            return badRequest("Team does not exist");
        }

        return ok(team.getScore());
    }

    @PUT
    @Path("/inc/{teamId}")
    public Response incrementScore(@PathParam("teamId") Long teamId,
                                   @QueryParam("points") @DefaultValue("2") int points,
                                   @QueryParam("token") String token) {

        if (teamId == null || teamId == 0) {
            LOGGER.info("Team id can't be null");
            return badRequest("Team id can't be null or zero");
        }

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            LOGGER.info("Token not found: "+token);
            return unauthorized("Invalid token");
        }

        final Team team = teamDAO.find(teamId);

        if (team == null) {
            LOGGER.info("Team not found");
            return badRequest("Team does not exist");
        }

        if (team.getScore() + points <= 999) {

            team.setScore(team.getScore() + points);
            update(team);

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("Set score %d for team %s", team.getScore(), team.getName()));
            }

            return ok(team.getScore());
        } else {
            return badRequest("Score can't go beyond 999");
        }
    }

    @PUT
    @Path("/dec/{teamId}")
    public Response decrementScore(@PathParam("teamId") Long teamId,
                                   @QueryParam("points") @DefaultValue("1") int points,
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

        if (team.getScore() - points >= 0) {
            team.setScore(team.getScore() - points);
            update(team);
            return ok(team.getScore());
        } else {
            return badRequest("Score can't be negative");
        }
    }

    private void update(final Team team) {

        if (team.isMirrored()) {
            LOGGER.info("Score mirroring turned ON");

            if (team.getKey().equals(TeamType.A.toString())) {
                producerService.printHomeScore(team.getScore());
            } else {
                producerService.printVisitorsScore(team.getScore());
            }
        }

        teamDAO.update(team);

        device.setScore(team);
    }
}
