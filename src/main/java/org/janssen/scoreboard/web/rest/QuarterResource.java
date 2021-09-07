package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The Quarter service
 *
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/quarter", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuarterResource {

    private final Logger log = LoggerFactory.getLogger(QuarterResource.class);

    private final GameService gameService;

    private final GameClockController gameClockController;

    public QuarterResource(GameService gameService,
                           GameClockController gameClockController) {
        this.gameService = gameService;
        this.gameClockController = gameClockController;
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<String> getQuarter(
            @PathVariable("gameId") Long gameId) {

        log.debug("Get game quarter");

        if (gameId == null || gameId == 0) {
            return ResponseEntity.badRequest().body("Game id can't be null or zero");
        }

        return gameService
                .findGameById(gameId)
                .map(game -> ResponseEntity.ok(game.getQuarter().toString()))
                .orElse(ResponseEntity.badRequest().body("Game does not exist"));
    }

    /**
     * Increment the game quarter.
     *
     * @param gameId    the game identifier
     * @return the game quarter value
     */
    @PutMapping("/inc/{gameId}")
    public ResponseEntity<?> incrementQuarter(@PathVariable("gameId") Long gameId) {

        log.debug("Increment quarter for game {}", gameId);

        if (gameClockController.isRunning()) {
            log.info("Game clock controller is running");
            return ResponseEntity.badRequest().body("Quarter kan je niet veranderen wanneer klok actief is");
        }

        if (gameClockController.getSeconds() > 0) {
            log.info("seconds is not 0");
            return ResponseEntity.badRequest().body("Klok staat nog niet op 00:00");
        }

        if (gameId == null || gameId == 0) {
            String msg = String.format("Game ID kan niet '%d' zijn", gameId);
            log.info(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        return gameService
                .findGameById(gameId)
                .map(game -> {
                    gameService.incrementGameQuarter(game);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.badRequest().body("Wedstrijd niet gevonden, start een 'New Game'!!"));
    }

    /**
     * Decrement the game quarter.
     *
     * @param gameId    the game identifier
     * @return  the game quarter value
     */
    @PutMapping("/dec/{gameId}")
    public ResponseEntity<?> decrementQuarter(@PathVariable("gameId") Long gameId) {

        log.debug("Decrement game quarter");

        if (gameId == null || gameId == 0) {
            String msg = String.format("Game ID kan niet '%d' zijn", gameId);
            log.info(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        return gameService
                .findGameById(gameId)
                .filter(game -> game.getQuarter() > 1)
                .map(game -> {

                    game.decrementQuarter();

                    gameService.resetTeamFouls(game);

                    gameService.resetTimeoutLeds(game);

                    gameService.update(game);

                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.badRequest().body("Wedstrijd niet gevonden, start een 'New Game'!!"));
    }
}
