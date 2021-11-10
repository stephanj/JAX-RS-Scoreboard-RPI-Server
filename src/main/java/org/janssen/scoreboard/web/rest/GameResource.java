package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.service.GameService;
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

    public GameResource(GameService gameService) {
        this.gameService = gameService;
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

        return gameService
            .findGameById(gameId)
            .map(game -> {
                log.debug(">>>>> Game found, set clock");

                gameService.init(game);

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
                        .body(gameService.getInfo(game)))
                .orElse(ResponseEntity.notFound().build());
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
