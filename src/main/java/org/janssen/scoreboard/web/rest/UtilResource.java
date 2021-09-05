package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.service.GameService;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;

/**
 * @author Stephan Janssen
 */
@RestController
@RequestMapping("/api/util")
public class UtilResource {

    private final Logger log = LoggerFactory.getLogger(UtilResource.class);

    private final GameService gameService;

    private final DeviceController device;

    public UtilResource(GameService gameService,
                        DeviceController device) {
        this.gameService = gameService;
        this.device = device;
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/version", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> version() {
        log.debug("Return server version");
        String version = "Version 3.0 - 4 sept 2021";
        log.info(version);
        return ResponseEntity.ok().body(version);
    }

    @GetMapping("/clear")
    public ResponseEntity<?> clearGameBoard() {
        log.debug("Clear scoreboard");
        device.clearBoard();
        return ResponseEntity.ok().build();
    }

//    @PutMapping("/redraw")
//    public ResponseEntity<?> reDrawGameBoard() {
//        log.debug("Redraw scoreboard");
//
//        return gameService
//                .current()
//                .map(game -> {
//                    device.setClockOnly(game.getClock());
//
//                    device.setScore(game.getTeamA());
//                    device.setScore(game.getTeamB());
//
//                    device.setPlayerFoul(game.getQuarter());
//
//                    device.setFoul(game.getTeamA());
//                    device.setFoul(game.getTeamB());
//
//                    return ResponseEntity.ok().build();
//                }).orElse(ResponseEntity.badRequest().body("No games exist"));
//    }

    @PutMapping("/turnoff")
    public ResponseEntity<?> turnOffScoreBoard() {
        log.debug("Turnoff scoreboard");
        device.turnOff();
        return ResponseEntity.ok().build();
    }
}
