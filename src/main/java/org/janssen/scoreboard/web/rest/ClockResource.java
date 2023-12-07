package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.service.ClockService;
import org.janssen.scoreboard.service.GameService;
import org.janssen.scoreboard.service.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The clock service
 *
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/clock", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClockResource {

    private final Logger log = LoggerFactory.getLogger(ClockResource.class);

    private final GameService gameService;
    private final ClockService clockService;
    private final GameClockController gameClockController;

    public ClockResource(GameService gameService,
                         ClockService clockService,
                         GameClockController gameClockController) {
        this.gameService = gameService;
        this.clockService = clockService;
        this.gameClockController = gameClockController;
    }

    /**
     * Get the game clock
     * @param gameId the game id
     * @return the game clock
     */
    @GetMapping("{gameId}")
    public ResponseEntity<?> getClock(@PathVariable("gameId") Long gameId) {

        log.debug("Get game clock");

        if (gameId == null || gameId == 0) {
            return ResponseEntity.badRequest().body("Game id can't be null or zero");
        }

        return gameService
                .findGameById(gameId)
                .map(game -> {
                    int time = game.getClock() - gameClockController.getSeconds();
                    return ResponseUtil.ok(Integer.toString(time));
                })
                .orElse(ResponseUtil.badRequest("Game not found"));
    }

    @PutMapping("/start/{gameId}")
    public ResponseEntity<?> startClock(@PathVariable("gameId") Long gameId) {

        log.debug("Start clock for game with id {} ", gameId);

        if (gameId == null || gameId == 0) {
            String msg = String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId);
            log.info(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        return gameService
                .findGameById(gameId)
                .map(game -> {
                    if (clockService.start(game)) {
                        log.debug("Clock started");
                        return ResponseUtil.ok();
                    } else {
                        log.debug("Clock NOT started!!!");
                        return ResponseUtil.badRequest();
                    }
                })
                .orElse(ResponseUtil.badRequest("Game not found"));
    }

    @PutMapping("/countdown/{seconds}")
    public ResponseEntity<?> countDownClock(
            @PathVariable("seconds") int seconds,
            @RequestParam("mirrored") boolean mirrored) {
        clockService.startCountdown(seconds, mirrored, true);
        return ResponseUtil.ok();
    }

    @PutMapping("/stop/{gameId}")
    public ResponseEntity<?> stopClock(@PathVariable("gameId") Long gameId) {

        log.debug("Stop clock for game with id {} ", gameId);

        if (gameId == null || gameId == 0) {
            String msg = String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId);
            log.info(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        return gameService
                .findGameById(gameId)
                .map(game -> {
                    if (clockService.stop(game)) {
                        return ResponseUtil.ok();
                    } else {
                        return ResponseUtil.badRequest();
                    }
                }).orElse(ResponseUtil.badRequest("Klok is niet actief, dus kan je ook niet stoppen"));
    }

    @PutMapping("/inc/{gameId}")
    public ResponseEntity<?> incClock(
            @PathVariable("gameId") Long gameId,
            @RequestParam("seconds") @DefaultValue("1") int seconds) {

        log.debug("Increment game clock {} with {} seconds", gameId, seconds);

        if (gameId == null || gameId == 0) {
            return ResponseEntity.badRequest().body(String.format("Game id (%d) is wrong, start 'New Game'.", gameId));
        }

        return gameService
            .findGameById(gameId)
            .map(game -> {
                if (clockService.incrementClock(game, seconds)) {
                    return ResponseUtil.ok();
                } else {
                    return ResponseUtil.badRequest();
                }
            })
            .orElse(ResponseEntity.badRequest().body("Game niet gevonden"));
    }

    @PutMapping("/dec/{gameId}")
    public ResponseEntity<?> decClock(
            @PathVariable("gameId") Long gameId,
            @RequestParam("seconds") @DefaultValue("1") int seconds) {

        if (gameId == null || gameId == 0) {
            return ResponseEntity.badRequest().body(String.format("Game id (%d) is wrong, start 'New Game'.", gameId));
        }

        return gameService
            .findGameById(gameId)
            .map(game -> {
                if (clockService.decrementClock(game, seconds)) {
                    return ResponseUtil.ok();
                } else {
                    return ResponseUtil.badRequest();
                }
            })
            .orElse(ResponseEntity.badRequest().body("Game niet gevonden"));
    }
}
