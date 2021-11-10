package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.controller.TimeoutClockController;
import org.janssen.scoreboard.controller.TwentyFourClockController;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.service.GameService;
import org.janssen.scoreboard.service.broadcast.ProducerService;
import org.janssen.scoreboard.service.util.ResponseUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

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

    private final GameService gameService;

    private final GameClockController gameClockController;

    private final TwentyFourClockController twentyFourClockController;

    private final ProducerService producerService;

    private final DeviceController deviceController;

    private final TimeoutClockController timeoutClockController;

    public GameResource(GameService gameService,
                        GameClockController gameClockController,
                        TwentyFourClockController twentyFourClockController,
                        TimeoutClockController timeoutClockController,
                        DeviceController deviceController,
                        ProducerService producerService) {
        this.gameService = gameService;
        this.deviceController = deviceController;
        this.gameClockController = gameClockController;
        this.twentyFourClockController = twentyFourClockController;
        this.producerService = producerService;
        this.timeoutClockController = timeoutClockController;
    }

    @PostMapping
    public ResponseEntity<?> createGame(@RequestParam("teamA") String teamNameA,
                                        @RequestParam("teamB") String teamNameB,
                                        @RequestParam("type")  int gameType,
                                        @RequestParam("age")   int ageCategory,
                                        @RequestParam("court") String court,
                                        @RequestParam("mirrored") boolean mirrored) throws URISyntaxException {

        log.debug(">>>>> Create new game for home team : {} and away team : {}", teamNameA, teamNameB);

        Game game = gameService.newGame(teamNameA, teamNameB, gameType, ageCategory, court, mirrored);

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("game", game);

        return ResponseEntity.created(new URI("/api/game/" + game.getId())).body(jsonResponse);
    }

    @PostMapping("/start")
    public ResponseEntity<Object> startGame(@RequestParam("token") String token,
                                            @RequestParam("gameId") Long gameId,
                                            @RequestParam("mirrored") Boolean mirrored) {

        log.debug(">>>>> Start game with id '{}', token '{}' and mirrored '{}'", gameId, token, mirrored);

        // Clock might be running in countdown mode
        if (gameClockController.isRunning()) {
            gameClockController.stop();
        }

        return gameService
            .findGameById(gameId)
            .map(game -> {
                log.debug(">>>>> Game found, set clock");

                if (game.isMirrored()) {
                    // Init slave scoreboard
                    producerService.newGame();
                }

                gameClockController.setSeconds(game.getClock());

                deviceController.setAllClocks(game.getClock(), TWENTY_FOUR_SECONDS);

                return ResponseUtil.ok();
            }).orElse(ResponseUtil.badRequest("Game not found"));
    }

    @GetMapping("{gameId}")
    public ResponseEntity<?> findGameById(@PathVariable("gameId") Long gameId) {
        log.debug(">>>>> Find game by id {}", gameId);

        if (gameId == null || gameId == 0) {
            log.error("Game id is null or zero");
            return ResponseEntity.badRequest().body("Game Id can't be null or zero");
        }

        return gameService
                .findGameById(gameId)
                .map(game -> ResponseEntity.ok().body(game))
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("list")
    public ResponseEntity<?> listGame() {
        log.debug(">>>>> List games");
        return ResponseEntity.ok().body(gameService.findAll());
    }

    /**
     * Return the game info as JSON
     * This is used to show game score, clock and 24 seconds in the OBS streaming layer.
     *
     * @param gameId    the game id
     * @return  return Game JSON info
     */
    @GetMapping("info/{gameId}")
    public ResponseEntity<?> getGameInfoAsJson(@PathVariable("gameId") Long gameId) {
        return gameService
                .findGameById(gameId)
                .map(game -> ResponseEntity
                        .ok()
                        .body(getGameClockFormatted(game)))
                .orElse(ResponseEntity.notFound().build());
    }

    private String getGameClockFormatted(final Game game) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("24", twentyFourClockController.getTwentyFourSeconds()); // 24 seconds
        jsonObject.put("s", gameClockController.getSeconds() % 60);             // Clock seconds
        jsonObject.put("m", gameClockController.getSeconds() / 60);             // Clock minutes
        jsonObject.put("A", game.getTeamA().getScore());                        // Home team score
        jsonObject.put("B", game.getTeamB().getScore());                        // Visiting team score
        jsonObject.put("Q", getQuarterString(game.getQuarter(), gameClockController.inCountDownMode()));
        jsonObject.put("T", timeoutClockController.isRunning());                // Timeout clock running?
        jsonObject.put("TT", timeoutClockController.getTimeoutValue());         // Timeout time
        return jsonObject.toJSONString();
    }

    private String getQuarterString(Integer quarter, boolean countDownMode) {
        if (!countDownMode) {
            switch (quarter) {
                case 1:
                    return "1st";
                case 2:
                    return "2nd";
                case 3:
                    return "3rd";
                case 4:
                    return "4th";
                default:
                    return "OT";
            }
        } else {
            return "---";
        }
    }

    /**
     * Show game info as text.
     * @param gameId the game id
     * @return game info as text
     */
    @GetMapping(value = "{gameId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String showGameAsText(@PathVariable("gameId") Long gameId) {
        log.debug(">>>>> Get game info as text {}", gameId);

        if (gameId == null || gameId == 0) {
            log.error("Game id is null or zero");
            return "Game Id can't be null or zero";
        }

        return gameService
                .findGameById(gameId)
                .map(game -> game +
                        ", Quarter:" + game.getQuarter() +
                        ", Team A Fouls:" + game.getTeamA().getFouls() +
                        ", Team B Fouls:" + game.getTeamB().getFouls() +
                        ", Category:" + game.getAgeCategory().getName() +
                        ", Game clock:" + getGameClockFormatted(game) +
                        ", Type:" + game.getGameType() +
                        ", Court:" + game.getCourt() +
                        ", CreatedBy:" + game.getUserName())
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
            log.error("Game id is null or zero");
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
