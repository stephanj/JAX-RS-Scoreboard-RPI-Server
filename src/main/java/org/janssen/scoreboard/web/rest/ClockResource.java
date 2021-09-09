package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.model.type.GameType;
import org.janssen.scoreboard.service.GameService;
import org.janssen.scoreboard.service.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.janssen.scoreboard.service.util.Constants.FOUR_MINUTES;
import static org.janssen.scoreboard.service.util.Constants.TEN_MINUTES_IN_SECONDS;

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
    private final GameClockController gameClockController;

    public ClockResource(GameService gameService,
                         GameClockController gameClockController) {
        this.gameService = gameService;
        this.gameClockController = gameClockController;
    }

    @GetMapping("{gameId}")
    public ResponseEntity<?> getClock(
            @PathVariable("gameId") Long gameId) {

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
    public ResponseEntity<Object> startClock(@PathVariable("gameId") Long gameId) {

        log.debug("Start clock for game with id {} ", gameId);

        if (gameClockController.isNotRunning()) {

            if (gameId == null || gameId == 0) {
                String msg = String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId);
                log.info(msg);
                return ResponseEntity.badRequest().body(msg);
            }

            return gameService
                    .findGameById(gameId)
                    .map(game -> {
                        if (game.getClock() > 0 && gameClockController.getSeconds() > 0) {
                            gameClockController.start(game.getClock(), game.getGameType(), game.isMirrored());
                        } else {
                            log.info("Start clock but is 0");
                        }
                        return ResponseUtil.ok();
                    })
                    .orElse(ResponseUtil.badRequest("Game not found"));
        }
        return ResponseEntity.badRequest().body("Clock is running");
    }

    @PutMapping("/countdown/{seconds}")
    public ResponseEntity<?> countDownClock(
            @PathVariable("seconds") int seconds,
            @RequestParam("mirrored") boolean mirrored) {

        if (gameClockController.isRunning()) {
            gameClockController.stop();
        }

        gameClockController.start(seconds, GameType.BASKET, mirrored);
        return ResponseUtil.ok();
    }

    @PutMapping("/stop/{gameId}")
    public ResponseEntity<?> stopClock(@PathVariable("gameId") Long gameId) {

        log.debug("Stop clock for game with id {} ", gameId);

        if (gameClockController.isRunning()) {

            if (gameId == null || gameId == 0) {
                String msg = String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId);
                log.info(msg);
                return ResponseEntity.badRequest().body(msg);
            }

            gameClockController.stop();

            return gameService
                .findGameById(gameId)
                .map(game -> {
                    game.setClock(gameClockController.getSeconds());
                    gameService.update(game);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.badRequest().body("Wedstrijd niet gevonden, start 'New Game"));
        }

        return ResponseEntity.badRequest().body("Klok is niet actief, dus kan je ook niet stoppen");
    }

    @PutMapping("/inc/{gameId}")
    public ResponseEntity<?> incClock(
            @PathVariable("gameId") Long gameId,
            @RequestParam("seconds") @DefaultValue("1") int seconds) {

        log.debug("Increment game clock {} with {} seconds", gameId, seconds);

        if (gameClockController.isNotRunning()) {

            if (gameId == null || gameId == 0) {
                return ResponseEntity.badRequest().body(String.format("Game id (%d) is wrong, start 'New Game'.", gameId));
            }

            return gameService
                .findGameById(gameId)
                .map(game -> {
                    Integer clock = game.getClock();
                    if (clock < TEN_MINUTES_IN_SECONDS) {

                        if (game.getGameType() == GameType.BASKET_KIDS && clock >= FOUR_MINUTES) {
                            return ResponseEntity.badRequest().body("Can't be higher than 4 min. for kids basket");
                        }

                        clock += seconds;
                        game.setClock(clock);
                        gameService.update(game);

                        gameClockController.setSeconds(clock);
                        return ResponseEntity.ok().build();
                    } else {
                        return ResponseEntity.badRequest().body("Can't be higher than 10 min.");
                    }
                }).orElse(ResponseEntity.badRequest().body("Game niet gevonden"));
        }

        return ResponseEntity.badRequest().body("Not allowed, clock is still running");
    }

    @PutMapping("/dec/{gameId}")
    public ResponseEntity<?> decClock(
            @PathVariable("gameId") Long gameId,
            @RequestParam("seconds") @DefaultValue("1") int seconds) {

        if (gameClockController.isNotRunning()) {

            if (gameId == null || gameId == 0) {
                String msg = String.format("Wedstrijd ID (%d) is verkeerd, start 'New Game'.", gameId);
                return ResponseEntity.badRequest().body(msg);
            }

            return gameService
                .findGameById(gameId)
                .map(game -> {
                    Integer clock = game.getClock();
                    if (clock - seconds >= 0) {
                        clock -= seconds;
                        game.setClock(clock);
                        gameService.update(game);

                        gameClockController.setSeconds(clock);
                        return ResponseEntity.ok().build();
                    } else {
                        return ResponseEntity.badRequest().body("Can't have a negative clock");
                    }
                })
                .orElse(ResponseEntity.badRequest().body("Game niet gevonden"));
        } else {
            return ResponseEntity.badRequest().body("Not allowed, clock is still running");
        }
    }
}
