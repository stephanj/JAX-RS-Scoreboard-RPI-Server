package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.controller.TwentyFourClockController;
import org.janssen.scoreboard.dao.GameDAO;
import org.janssen.scoreboard.dao.TeamDAO;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Game;
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
import java.util.List;
import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.Constants.TWENTY_FOUR_SECONDS;
import static org.janssen.scoreboard.service.util.ResponseUtil.*;

/**
 * The Game service.
 *
 * @author Stephan Janssen
 */
@Singleton
@Produces({MediaType.APPLICATION_JSON})
@Path("/api/game")
public class GameService {

    private static final Logger LOGGER = Logger.getLogger(GameService.class.getName());

    private static final String ID = "id";
    private static final String TOKEN = "token";
    private static final String MIRRORED = "mirrored";
    private static final String FIRST = "first";
    private static final String MAX = "max";

    @EJB
    private GameDAO gameDAO;

    @EJB
    private TeamDAO teamDAO;

    @EJB
    private TokenDAO tokenDAO;

    @Inject
    private DeviceController device;

    @Inject
    private GPIOController gpioController;

    @EJB
    private GameClockController clockController;

    @EJB
    private TwentyFourClockController twentyFourClockController;

    @Inject
    private ProducerService producerService;

    @POST
    @Path("/")
    public Response createGame(@QueryParam(TOKEN) String token,
                               @QueryParam("teamA") String nameA,
                               @QueryParam("teamB") String nameB,
                               @QueryParam("type") int type,
                               @QueryParam("age") int ageCategory,
                               @QueryParam("court") String court,
                               @QueryParam(MIRRORED) boolean mirrored) {

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            LOGGER.info("Invalid token: "+token);
            return unauthorized("Invalid token");
        }

        // Clock might be running in countdown mode
        if (clockController.isRunning()) {
            clockController.stop();
        }

        final Team teamA = teamDAO.create(nameA, TeamType.A, mirrored);
        LOGGER.info("TeamA mirroring turned on? " + teamA.isMirrored());

        final Team teamB = teamDAO.create(nameB, TeamType.B, mirrored);
        LOGGER.info("TeamB mirroring turned on? " + teamB.isMirrored());

        final Game game = gameDAO.create(teamA, teamB, type, ageCategory, court, foundToken.getFullName(), mirrored);

        LOGGER.info("Game mirroring turned on? " + game.isMirrored());

        if (game.isMirrored()) {
            // Init slave scoreboard
            producerService.newGame();
        }

        device.setGame(game);

        // Reset the timeout LEDs
        gpioController.setLed(GPIOType.TIME_OUT_V1, false);
        gpioController.setLed(GPIOType.TIME_OUT_V2, false);

        gpioController.setLed(GPIOType.TIME_OUT_H1, false);
        gpioController.setLed(GPIOType.TIME_OUT_H2, false);

        gpioController.showTwentyFourSeconds(true);

        return created(game);
    }

    @POST
    @Path("/start")
    public Response startGame(@QueryParam(TOKEN) String token,
                              @QueryParam("gameId") int gameId,
                              @QueryParam(MIRRORED) boolean mirrored) {

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            LOGGER.info("Invalid token: "+token);
            return unauthorized("Invalid token");
        }

        // Clock might be running in countdown mode
        if (clockController.isRunning()) {
            clockController.stop();
        }

        final Game game = gameDAO.find(gameId);
        if (game.isMirrored()) {
            // Init slave scoreboard
            producerService.newGame();
        }

        //
        // Possible fix for countdown clock when reached 0
        //
        // TODO  TEST !!!
        //
        clockController.setSeconds(game.getClock());

        device.setAllClocks(game.getClock(), TWENTY_FOUR_SECONDS);
        return ok();
    }

    @Path("/list")
    @GET
    public List<Game> listGames(@QueryParam(FIRST) @DefaultValue("0") int first,
                                @QueryParam(MAX) @DefaultValue("20") int max) {
        return gameDAO.list(first, max);
    }

    @Produces("text/*")
    @Path("/list")
    @GET
    public String listGamesAsText(@QueryParam(FIRST) @DefaultValue("0") int first,
                                  @QueryParam(MAX) @DefaultValue("20") int max) {

        final List<Game> games = gameDAO.list(first, max);

        if (games != null && games.size() > 0) {
            StringBuilder builder = new StringBuilder();

            for (final Game game : games) {
                builder.append(game).append("\n");
            }
            return builder.toString();
        } else {
            return "No games";
        }
    }

    @Path("/{id}")
    @GET
    public Response showGame(@PathParam(ID) Long id) {
        if (id == null || id == 0) {
            return badRequest("Game Id can't be null or zero");
        }
        return ok(gameDAO.find(id));
    }

    @Produces("text/*")
    @Path("/{id}")
    @GET
    public String showGameAsText(@PathParam(ID) Long id) {
        if (id == null || id == 0) {
            return "Game Id can't be null or zero";
        }
        Game game = gameDAO.find(id);

        if (game != null) {
            String gameClock = String.format(", Game time:%02d:%02d", game.getClock()/60, game.getClock()%60);

            return new StringBuilder().append(game.toString())
                    .append(", Quarter:").append(game.getQuarter())
                    .append(", Team A Fouls:").append(game.getTeamA().getFouls())
                    .append(", Team B Fouls:").append(game.getTeamB().getFouls())
                    .append(", Category:").append(game.getAgeCategory().getName())
                    .append(gameClock)
                    .append(", Type:").append(game.getGameType())
                    .append(", Court:").append(game.getCourt())
                    .append(", CreatedBy:").append(game.getUserName()).toString();
        } else {
            return "Game not found";
        }
    }

    @Path("/{id}")
    @DELETE
    public Response deleteGame(@PathParam(ID) Long id,
                               @QueryParam(TOKEN) String token) {

        if (id == null || id == 0) {
            return badRequest("Game Id can't be null or zero");
        }

        final Token foundToken = tokenDAO.find(token);
        if (foundToken == null) {
            return unauthorized("Invalid token");
        }

        gameDAO.delete(id);
        device.clearBoard();
        return gone();
    }
}
