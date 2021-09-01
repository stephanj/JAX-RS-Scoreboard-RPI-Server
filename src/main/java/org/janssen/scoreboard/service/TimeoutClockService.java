package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.controller.TimeoutClockController;
import org.janssen.scoreboard.dao.TeamDAO;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.Token;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.TeamType;
import org.janssen.scoreboard.service.broadcast.ProducerService;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.Constants.*;
import static org.janssen.scoreboard.service.util.ResponseUtil.badRequest;
import static org.janssen.scoreboard.service.util.ResponseUtil.unauthorized;
import static org.janssen.scoreboard.service.util.ResponseUtil.ok;

/**
 * @author Stephan Janssen
 */
@Singleton
@Path("/api/timeout")
@Produces({MediaType.APPLICATION_JSON})
public class TimeoutClockService {

    private static final Logger LOGGER = Logger.getLogger(TimeoutClockService.class.getName());

    @Inject
    private TokenDAO tokenDAO;

    @Inject
    private TeamDAO teamDAO;

    @EJB
    private TimeoutClockController timeoutClockController;

    @Inject
    private GPIOController gpioController;

    @Inject
    private ProducerService producerService;

    @Inject
    private GameClockController gameClockController;

    // Start timeout
    @PUT
    @Path("/start")
    public Response startClock(@QueryParam("token") String token) {

        if (gameClockController.isRunning()) {
            return badRequest("Can't start timeout when clock is running");
        }

        if (!SECRET_TOKEN.equals(token)) {
            final Token foundToken = tokenDAO.find(token);
            if (foundToken == null) {
                return unauthorized("Invalid token");
            }
        }

        // Start timeout clock
        if (timeoutClockController.isNotRunning()) {
            timeoutClockController.start();
            return ok();
        } else {
            return badRequest("Timeout already running");
        }
    }

    @PUT
    @Path("/inc/{teamId}")
    public Response incrementTimeout(@QueryParam("token") String token,
                                     @PathParam("teamId") Integer teamId) {

        LOGGER.info("Increment timeout for team: "+teamId);

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        if (teamId == null || teamId == 0) {
            return badRequest("Team id can't be null or zero");
        }

        final Team team = teamDAO.find(teamId);
        if (team == null) {
            return badRequest("Team does not exist");
        }

        if (team.getTimeOut() + 1 > 2) {
            team.setTimeOut(0);                         // reset timeout counter
        } else {
            team.setTimeOut(team.getTimeOut() + 1);     // increment timeout counter
        }

        teamDAO.update(team);                       // save team
        setTimeOutLed(team);                        // set timeout led
        return ok();
    }

    private void setTimeOutLed(final Team team) {

        if (team.getKey().equals(TeamType.A.toString())) {
            if (team.getTimeOut() == 0) {
                gpioController.setLed(GPIOType.TIME_OUT_H1, false);
                gpioController.setLed(GPIOType.TIME_OUT_H2, false);
            } else if (team.getTimeOut() == 1) {
                gpioController.setLed(GPIOType.TIME_OUT_H1, true);
            } else {
                gpioController.setLed(GPIOType.TIME_OUT_H2, true);
            }

            if (team.isMirrored()) {
                LOGGER.info("Print mirrored timeout LED(A):"+team.getTimeOut());
                producerService.printHomeTimeout(team.getTimeOut());
            }

        } else {
            if (team.getTimeOut() == 0) {
                gpioController.setLed(GPIOType.TIME_OUT_V1, false);
                gpioController.setLed(GPIOType.TIME_OUT_V2, false);
            }
            else if (team.getTimeOut() == 1) {
                gpioController.setLed(GPIOType.TIME_OUT_V1, true);
            } else {
                gpioController.setLed(GPIOType.TIME_OUT_V2, true);
            }

            if (team.isMirrored()) {
                LOGGER.info("Print mirrored timeout LED(B):"+team.getTimeOut());
                producerService.printVisitorsTimeout(team.getTimeOut());
            }
        }
    }

    // Stop timeout and Restore 24s
    @PUT
    @Path("/stop")
    public Response stopClock(@QueryParam("token") String token) {

        if (!SECRET_TOKEN.equals(token)) {
            final Token foundToken = tokenDAO.find(token);
            if (foundToken == null) {
                return unauthorized("Invalid token");
            }
        }

        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
            return ok();
        } else {
            return badRequest("Timeout is not running");
        }
    }
}
