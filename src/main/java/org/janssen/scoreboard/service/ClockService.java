package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.dao.GameDAO;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.type.GameType;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.Constants.FOUR_MINUTES;
import static org.janssen.scoreboard.service.util.Constants.TEN_MINUTES_IN_SECONDS;
import static org.janssen.scoreboard.service.util.ResponseUtil.badRequest;
import static org.janssen.scoreboard.service.util.ResponseUtil.ok;

/**
 * The clock service
 *
 * @author Stephan Janssen
 */
@Path("/api/clock")
@Produces({MediaType.APPLICATION_JSON})
public class ClockService {

    private static final Logger LOGGER = Logger.getLogger(ClockService.class.getName());

    @Inject
    private GameDAO gameDAO;

    @Inject
    private TokenDAO tokenDAO;

    @EJB
    private GameClockController clockController;

    @GET
    @Path("/{gameId}")
    public Response getClock(@PathParam("gamedId") Long gameId) {

        if (gameId == null || gameId == 0) {
            return badRequest("Game id can't be null or zero");
        }

        Game game = gameDAO.find(gameId);
        if (game == null) {
            return badRequest("Game does not exist");
        }

        return ok(game.getClock() - clockController.getSeconds());
    }

    @PUT
    @Path("/start/{gameId}")
    public Response startClock(@PathParam("gameId") Long gameId) {

        LOGGER.info("Start clock voor "+gameId);

        if (clockController.isNotRunning()) {

            if (gameId == null || gameId == 0) {
                String msg = String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId);
                LOGGER.info(msg);
                return badRequest(msg);
            }

            final Game game = gameDAO.find(gameId);
            if (game == null) {
                String msg = String.format("Wedstrijd ID %d niet gevonden, start 'New Game'", gameId);
                LOGGER.info(msg);
                return badRequest(msg);
            }

            if (game.getClock() > 0 && clockController.getSeconds() > 0) {
                clockController.start(game.getClock(), game.getGameType(), game.isMirrored());
            } else {
                LOGGER.info("Start clock but is 0");
            }

            LOGGER.info("OK");
            return ok();

        } else {
            String msg = "Klok is reeds gestart";
            LOGGER.info(msg);
            return badRequest(msg);
        }
    }

    @PUT
    @Path("/countdown/{seconds}")
    public Response countDownClock(@PathParam("seconds") int seconds,
                                   @QueryParam("mirrored") boolean mirrored) {

        if (clockController.isRunning()) {
            clockController.stop();
        }

        clockController.start(seconds, GameType.BASKET, mirrored);
        return ok();
    }

    @PUT
    @Path("/stop/{gameId}")
    public Response stopClock(@PathParam("gameId") Long gameId) {

        LOGGER.info("Stop clock voor "+gameId);

        if (clockController.isRunning()) {

            if (gameId == null || gameId == 0) {
                String msg = String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId);
                LOGGER.info(msg);
                return badRequest(msg);
            }

            clockController.stop();

            final Game game = gameDAO.find(gameId);
            if (game == null) {
                String msg = String.format("Wedstrijd ID %d niet gevonden, start 'New Game'", gameId);
                LOGGER.info(msg);
                return badRequest(msg);
            } else {
                game.setClock(clockController.getSeconds());
                gameDAO.update(game);
                LOGGER.info("OK");
                return ok();
            }
        } else {
            return badRequest("Klok is niet actief, dus kan je ook niet stoppen");
        }
    }

    @PUT
    @Path("/inc/{gameId}")
    public Response incClock(@PathParam("gameId") Long gameId,
                             @QueryParam("seconds") @DefaultValue("1") int seconds) {

        if (clockController.isNotRunning()) {

            if (gameId == null || gameId == 0) {
                return badRequest(String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId));
            }

            // Update persistent game clock
            final Game game = gameDAO.find(gameId);
            if (game == null) {
                String msg = String.format("Wedstrijd ID %d niet gevonden, start 'New Game'", gameId);
                LOGGER.info(msg);
                return badRequest(msg);
            }

            Integer clock = game.getClock();

            if (clock < TEN_MINUTES_IN_SECONDS) {

                if (game.getGameType() == GameType.BASKET_KIDS &&
                    clock >= FOUR_MINUTES) {
                    return badRequest("Can't be higher than 4 min. for kids basket");
                }

                clock+=seconds;
                game.setClock(clock);
                gameDAO.update(game);

                clockController.setSeconds(clock);
                return ok();
            } else {
                return badRequest("Can't be higher than 10 min.");
            }
        } else {
            return badRequest("Not allowed, clock is still running");
        }
    }

    @PUT
    @Path("/dec/{gameId}")
    public Response decClock(@PathParam("gameId") Long gameId,
                             @QueryParam("seconds") @DefaultValue("1") int seconds) {

        if (clockController.isNotRunning()) {

            if (gameId == null || gameId == 0) {
                return badRequest(String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId));
            }

            // Update persistent game clock
            final Game game = gameDAO.find(gameId);
            if (game == null) {
                String msg = String.format("Wedstrijd ID %d niet gevonden, start 'New Game'", gameId);
                LOGGER.info(msg);
                return badRequest(msg);
            }

            Integer clock = game.getClock();
            if (clock - seconds >= 0) {
                clock -= seconds;
                game.setClock(clock);
                gameDAO.update(game);

                clockController.setSeconds(clock);
                return ok();
            } else {
                return badRequest("Can't have a negative clock");
            }
        } else {
            return badRequest("Not allowed, clock is still running");
        }
    }
}
