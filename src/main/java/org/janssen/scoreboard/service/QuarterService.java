package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.dao.GameDAO;
import org.janssen.scoreboard.dao.TeamDAO;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.Token;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.GameType;
import org.janssen.scoreboard.service.broadcast.ProducerService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.ResponseUtil.badRequest;
import static org.janssen.scoreboard.service.util.ResponseUtil.ok;
import static org.janssen.scoreboard.service.util.ResponseUtil.unauthorized;

/**
 * The Quarter service
 *
 * @author Stephan Janssen
 */
@Path("/api/quarter")
@Produces({MediaType.APPLICATION_JSON})
public class QuarterService {

    private static final Logger LOGGER = Logger.getLogger(QuarterService.class.getName());

    @Inject
    protected GameDAO gameDAO;

    @Inject
    protected TeamDAO teamDAO;

    @Inject
    protected TokenDAO tokenDAO;

    @Inject
    protected DeviceController device;

    @Inject
    protected GPIOController gpioController;

    @Inject
    private ProducerService producerService;

    @Inject
    protected GameClockController gameClockController;

    @GET
    @Path("/{gameId}")
    public Response getQuarter(@PathParam("gameId") Long gameId) {

        if (gameId == null || gameId == 0) {
            return badRequest("Game id can't be null or zero");
        }

        final Game game = gameDAO.find(gameId);

        if (game == null) {
            return badRequest("Game does not exist");
        }

        return ok(game.getQuarter());
    }

    @PUT
    @Path("/inc/{gameId}")
    public Response incrementQuarter(@PathParam("gameId") Long gameId,
                                     @QueryParam("token") String token) {

        if (gameClockController.isRunning()) {
            LOGGER.info("Game clock controller is running");
            return badRequest("Quarter kan je niet veranderen wanneer klok actief is");
        }

        if (gameClockController.getSeconds() > 0) {
            LOGGER.info("seconds is not 0");
            return badRequest("Klok staat nog niet op 00:00");
        }

        if (gameId == null || gameId == 0) {
            String msg = String.format("Game ID kan niet '%d' zijn", gameId);
            LOGGER.info(msg);
            return badRequest(msg);
        }

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            LOGGER.info("Token not found:"+token);
            return unauthorized("Invalid token");
        }

        final Game game = gameDAO.find(gameId);

        if (game == null) {
            return badRequest("Wedstrijd niet gevonden, start een 'New Game'!!");
        }

        // Verify how many quarters mini football can have... 2 ?
        game.incrementQuarter();

        resetTeamFouls(game);

        // Reset the clock based on game type
        gameDAO.setGameClock(game);
        gameClockController.setSeconds(game.getClock());

        if ((game.getGameType() == GameType.BASKET && game.getQuarter() == 3) ||
            (game.getGameType() == GameType.BASKET_KIDS && game.getQuarter() == 5)) {
            resetTimeoutLeds(game);
        }

        // Show the 24s LEDs
        gpioController.showTwentyFourSeconds(true);

        update(game);
        return ok();
    }

    private void resetTeamFouls(final Game game) {
        if (game.getGameType() == GameType.BASKET ||
            (game.getGameType() == GameType.BASKET_KIDS &&  game.getQuarter() % 2 == 0)) {   // every 2 quarters

            final Team teamA = game.getTeamA();
            final Team teamB = game.getTeamB();

            teamA.setFouls(0);
            teamB.setFouls(0);

            teamDAO.update(teamA);
            teamDAO.update(teamB);
        }
    }

    @PUT
    @Path("/dec/{gameId}")
    public Response decrementQuarter(@PathParam("gameId") Long gameId,
                                     @QueryParam("token") String token) {

        if (gameId == null || gameId == 0) {
            String msg = String.format("Game ID kan niet '%d' zijn", gameId);
            LOGGER.info(msg);
            return badRequest(msg);
        }

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        final Game game = gameDAO.find(gameId);

        if (game  == null) {
            return badRequest("Wedstrijd niet gevonden, start een 'New Game'!!");
        }

        if (game.getQuarter() > 1) {

            game.decrementQuarter();

            resetTeamFouls(game);

            resetTimeoutLeds(game);

            update(game);

            return ok();
        } else {
            return badRequest("Quarter kan niet negatief zijn");
        }
    }

    private void update(final Game game) {
        if (game.isMirrored()) {
            // Reset Team Fouls slave board
            producerService.printFoulsA(0);
            producerService.printFoulsB(0);

            // Reset game clock slave board
            producerService.printTimeInSeconds(game.getClock());
        }

        gameDAO.update(game);

        // Reset Team Fouls
        device.setFoulsHome(0);
        device.setFoulsVisitors(0);

        // Reset game clock
        device.setClockOnly(game.getClock());
    }

    private void resetTimeoutLeds(final Game game) {

        LOGGER.info("game.isMirrored? " + game.isMirrored());

        if (game.isMirrored()) {
            LOGGER.info("QuarterService mirroring turn ON");

            producerService.printHomeTimeout(0);
            producerService.printVisitorsTimeout(0);
        } else {
            LOGGER.info("QuarterService mirroring turn OFF");
        }

        final Team teamA = game.getTeamA();
        teamA.setTimeOut(0);
        teamDAO.update(teamA);

        final Team teamB = game.getTeamB();
        teamB.setTimeOut(0);
        teamDAO.update(teamB);

        gpioController.setLed(GPIOType.TIME_OUT_H1, false);
        gpioController.setLed(GPIOType.TIME_OUT_H2, false);
        gpioController.setLed(GPIOType.TIME_OUT_V1, false);
        gpioController.setLed(GPIOType.TIME_OUT_V2, false);
    }
}
