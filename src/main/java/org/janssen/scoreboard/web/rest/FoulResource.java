package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.service.GameService;
import org.janssen.scoreboard.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The team fouls service.
 *
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/foul", produces = MediaType.APPLICATION_JSON_VALUE)
public class FoulResource {

    public static final int MAX_TEAM_FOULS = 5;
    private final Logger log = LoggerFactory.getLogger(FoulResource.class);

    private final TeamService teamService;
    private final GameService gameService;

    private final DeviceController device;

    public FoulResource(TeamService teamService,
                        GameService gameService,
                        DeviceController device) {
        this.teamService = teamService;
        this.gameService = gameService;
        this.device = device;
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<String> getFouls(@PathVariable("teamId") Long teamId) {

        log.debug("Get fouls for team {}", teamId);

        if (teamId == null || teamId == 0) {
            return ResponseEntity.badRequest().body("Team id can't be null or zero");
        }

        return teamService
                .findById(teamId)
                .map(team -> ResponseEntity.ok().body(team.getFouls().toString()))
                .orElse(ResponseEntity.badRequest().body("Team not found"));
    }

    @PutMapping("/reset/{gameId}")
    public ResponseEntity<?> resetFouls(@PathVariable("gameId") Long gameId) {

        log.debug("Reset fouls for game {}", gameId);

        if (gameId == null || gameId == 0) {
            return ResponseEntity.badRequest().body("Game id can't be null or zero");
        }

        gameService.resetGameFouls(gameId);
        return ResponseEntity.ok().build();
    }

    /**
     * Increment team fouls.
     *
     * @param teamId        the team id for which the foul is added
     * @param playerFouls   the total player fouls selected by "table responsible person" on Android app
     * @return
     */
    @PutMapping("/inc/{teamId}/{totalFouls}")
    public ResponseEntity<?> incrementFouls(
            @PathVariable("teamId") Long teamId,
            @PathVariable("totalFouls") Integer playerFouls) {

        log.debug("Increment fouls for team {} with player fouls {}", teamId, playerFouls);

        if (teamId == null || teamId == 0) {
            return ResponseEntity.badRequest().body("Team id can't be null or zero");
        }

        return teamService
                .findById(teamId)
                .map(team -> {
                    int fouls = team.getFouls();
                    if (fouls < MAX_TEAM_FOULS) {
                        team.setFouls(++fouls);

                        log.info(String.format("Team %s has %d fouls", team.getName(), team.getFouls()));

                        gameService.update(team);

                    } else {
                        // 5 Team fouls so only show player fouls
                        device.setPlayerFoul(playerFouls);
                    }
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.badRequest().body("Team not found"));
    }

    @PutMapping("/dec/{teamId}")
    public ResponseEntity<?> decrementFouls(
            @PathVariable("teamId") Long teamId) {

        if (teamId == null || teamId == 0) {
            return ResponseEntity.badRequest().body("Team id can't be null or zero");
        }

        return teamService
                .findById(teamId)
                .filter(team -> team.getFouls() > 0)
                .map(team -> {
                    int fouls = team.getFouls();
                    team.setFouls(--fouls);

                    log.info(String.format("Team %s has %d fouls", team.getName(), team.getFouls()));

                    gameService.update(team);

                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.badRequest().body("Team not found"));
    }
}
