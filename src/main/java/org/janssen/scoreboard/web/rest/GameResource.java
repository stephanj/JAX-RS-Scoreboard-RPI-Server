package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.TeamType;
import org.janssen.scoreboard.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.janssen.scoreboard.service.util.Constants.TWENTY_FOUR_SECONDS;

/**
 * The Game service.
 *
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/game", produces = MediaType.APPLICATION_JSON_VALUE)
public class GameResource {

    private final Logger log = LoggerFactory.getLogger(GameResource.class);

    private static final String MIRRORED = "mirrored";
    private static final String FIRST = "first";
    private static final String MAX = "max";

    private final GameService gameService;

    private final GameClockController gameClockController;

    public GameResource(GameService gameService,
                        GameClockController gameClockController) {
        this.gameService = gameService;
        this.gameClockController = gameClockController;
    }

    @PostMapping
    public ResponseEntity<?> createGame(@RequestParam("teamA") String nameA,
                                        @RequestParam("teamB") String nameB,
                                        @RequestParam("type") int type,
                                        @RequestParam("age") int ageCategory,
                                        @RequestParam("court") String court,
                                        @RequestParam(MIRRORED) boolean mirrored) {

        log.debug("Create new game");

        // Clock might be running in countdown mode
        if (gameClockController.isRunning()) {
            gameClockController.stop();
        }

        final Team teamA = teamDAO.create(nameA, TeamType.A, mirrored);
        log.info("TeamA mirroring turned on? " + teamA.isMirrored());

        final Team teamB = teamDAO.create(nameB, TeamType.B, mirrored);
        log.info("TeamB mirroring turned on? " + teamB.isMirrored());

        final Game game = gameDAO.create(teamA, teamB, type, ageCategory, court, foundToken.getFullName(), mirrored);

        log.info("Game mirroring turned on? " + game.isMirrored());

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

        return ResponseEntity.created(game).build();
    }

    @PostMapping("/start")
    public ResponseEntity<?> startGame(@QueryParam(TOKEN) String token,
                                       @QueryParam("gameId") int gameId,
                                       @QueryParam(MIRRORED) boolean mirrored) {

        log.debug("Start game");

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

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Game> listGames(@RequestParam(FIRST) @DefaultValue("0") int first,
                                @RequestParam(MAX) @DefaultValue("20") int max) {
        return gameService.findAll(first, max);
    }

    @GetMapping(value = "list", produces = MediaType.TEXT_PLAIN_VALUE)
    public String listGamesAsText(@RequestParam(FIRST) @DefaultValue("0") int first,
                                  @RequestParam(MAX) @DefaultValue("20") int max) {

        final List<Game> games = gameService.findAll(first, max);

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

    @GetMapping("{gameId}")
    public ResponseEntity<?> findGameById(@PathVariable("gameId") Long gameId) {

        if (gameId == null || gameId == 0) {
            return ResponseEntity.badRequest().body("Game Id can't be null or zero");
        }

        return gameService
                .findGameById(gameId)
                .map(game -> ResponseEntity.ok().body(game))
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping(value = "{gameId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String showGameAsText(@PathVariable("gameId") Long gameId) {
        if (gameId == null || gameId == 0) {
            return "Game Id can't be null or zero";
        }

        return gameService
                .findGameById(gameId)
                .map(game ->  {
                    String gameClock = String.format(", Game time:%02d:%02d", game.getClock()/60, game.getClock()%60);
                    return game +
                            ", Quarter:" + game.getQuarter() +
                            ", Team A Fouls:" + game.getTeamA().getFouls() +
                            ", Team B Fouls:" + game.getTeamB().getFouls() +
                            ", Category:" + game.getAgeCategory().getName() +
                            gameClock +
                            ", Type:" + game.getGameType() +
                            ", Court:" + game.getCourt() +
                            ", CreatedBy:" + game.getUserName();
                })
                .orElse("Game not found");
    }

    /**
     * Delete a game and reset score board.
     *
     * @param gameId    the game identifier to delete
     * @return gone or not found HTTP response
     */
    @DeleteMapping("{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable("gameId") Long gameId) {

        log.debug("Delete game with id {}", gameId);

        if (gameId == null || gameId == 0) {
            return ResponseEntity.badRequest().body("Game Id can't be null or zero");
        }

        return gameService
                .findGameById(gameId)
                .map(game -> {
                    gameService.delete(game);
                    return ResponseEntity.status(HttpStatus.GONE).build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
