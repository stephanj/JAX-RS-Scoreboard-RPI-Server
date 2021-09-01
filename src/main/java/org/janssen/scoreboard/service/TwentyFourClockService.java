package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.controller.TimeoutClockController;
import org.janssen.scoreboard.controller.TwentyFourClockController;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Token;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.Constants.SECRET_TOKEN;
import static org.janssen.scoreboard.service.util.Constants.TWENTY_FOUR_SECONDS;
import static org.janssen.scoreboard.service.util.ResponseUtil.*;

/**
 * @author Stephan Janssen
 */
@Singleton
@Path("/api/twentyfour")
@Produces({MediaType.APPLICATION_JSON})
public class TwentyFourClockService {

    private static final Logger LOGGER = Logger.getLogger(TwentyFourClockService.class.getName());

    @Inject
    private TokenDAO tokenDAO;

    @EJB
    private TwentyFourClockController twentyFourClockController;

    @EJB
    private TimeoutClockController timeoutClockController;

    @EJB
    private GameClockController gameClockController;

    private boolean timerOn = false;

    @GET
    @Path("/")
    public Response getClock() {
        return ok(twentyFourClockController.getTwentyFourSeconds());
    }

    @GET
    @Path("/date")
    public Response getDate() {
        return ok(new Date().toString());
    }

    @PUT
    @Path("/start")
    public Response startClock(@QueryParam("token") String token) {

        if (isInvalid(token)) return unauthorized("Invalid token");

        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
        }

        timerOn = true;

        if (twentyFourClockController.isNotRunning()) {
            twentyFourClockController.start();
            return ok();

        } else {
            return badRequest("Clock already running");
        }
    }

    @PUT
    @Path("/stop")
    public Response stopClock(@QueryParam("token") String token) {

        if (isInvalid(token)) return unauthorized("Invalid token");

        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
        }

        timerOn = false;

        if (twentyFourClockController.isRunning()) {

            twentyFourClockController.stop();
            return ok();

        } else {
            return badRequest("Clock not running");
        }
    }

    @GET
    @Path("/running")
    public Response isRunning() {

        if (twentyFourClockController.isRunning()) {
            return ok();
        } else {
            return badRequest("Clock not running");
        }
    }

    @GET
    @Path("/stopped")
    public Response isStopped() {

        if (twentyFourClockController.isNotRunning()) {
            return ok();
        } else {
            return badRequest("Clock is running");
        }
    }

    @PUT
    @Path("/reset")
    public Response resetClock(@QueryParam("token") String token) {

        if (isInvalid(token)) return unauthorized("Invalid token");

        // If 60s timeout is running then stop it
        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
        }

        twentyFourClockController.reset();

        if (timerOn &&
            twentyFourClockController.isNotRunning()) {

            if (timeoutClockController.isRunning()) {
                timeoutClockController.stop();
            }

            twentyFourClockController.start();
        }

        return ok();
    }

    @PUT
    @Path("/fourteen")
    public Response setFourteenSeconds(@QueryParam("token") String token) {

        if (isInvalid(token)) return unauthorized("Invalid token");

        if (timeoutClockController.isNotRunning()) {
            twentyFourClockController.setFourTeen();
        }

        if (timerOn &&
            twentyFourClockController.isNotRunning() &&
            timeoutClockController.isNotRunning()) {

            twentyFourClockController.start();
        }

        return ok();
    }

    @PUT
    @Path("/inc")
    public Response incClock(@QueryParam("token") String token) {

        if (twentyFourClockController.isNotRunning() &&
            timeoutClockController.isNotRunning()) {

            if (isInvalid(token)) return unauthorized("Invalid token");

            int twentyFourSeconds = twentyFourClockController.getTwentyFourSeconds();

            if (twentyFourSeconds < TWENTY_FOUR_SECONDS) {
                twentyFourSeconds++;
                twentyFourClockController.setTwentyFourSeconds(twentyFourSeconds);
                return ok();
            } else {
                return badRequest("24 secs can go beyond 24 secs  :)");
            }
        } else {
            return badRequest("Can't change clock will running");
        }
    }

    @PUT
    @Path("/dec")
    public Response decClock(@QueryParam("token") String token) {

        if (twentyFourClockController.isNotRunning() &&
            timeoutClockController.isNotRunning()) {

            if (isInvalid(token)) return unauthorized("Invalid token");

            int twentyFourSeconds = twentyFourClockController.getTwentyFourSeconds();

            if (twentyFourSeconds > 1) {
                twentyFourSeconds --;
                twentyFourClockController.setTwentyFourSeconds(twentyFourSeconds);
                return ok();
            } else {
                return badRequest("Can't have a negative twenty four seconds");
            }
        } else {
            return badRequest("Can't change clock will running");
        }
    }

    @PUT
    @Path("/show")
    public Response setVisible(@QueryParam("token") String token) {

        final int seconds = gameClockController.getSeconds();
        if (seconds < 25 && seconds > 0) {

            LOGGER.info("switchTwentyFourSeconds()");
            twentyFourClockController.switchTwentyFourSeconds();
            return ok();
        } else {
            LOGGER.info("setVisible(true)");
            twentyFourClockController.setVisible(true);
            return ok();
        }
    }

    private boolean isInvalid(final String token) {
        if (!SECRET_TOKEN.equals(token)) {
            final Token foundToken = tokenDAO.find(token);
            if (foundToken == null) {
                return true;
            }
        }
        return false;
    }
}
