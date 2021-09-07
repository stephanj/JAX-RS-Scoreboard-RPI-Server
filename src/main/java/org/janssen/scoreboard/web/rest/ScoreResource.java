package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.TeamType;
import org.janssen.scoreboard.service.TeamService;
import org.janssen.scoreboard.service.broadcast.ProducerService;
import org.janssen.scoreboard.service.repository.TeamRepository;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The score service.
 *
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/score", produces = MediaType.APPLICATION_JSON_VALUE)
public class ScoreResource {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(ScoreResource.class);

    private final TeamService teamService;

    private final DeviceController device;

    private final ProducerService producerService;

    public ScoreResource(TeamService teamService,
                         DeviceController device,
                         ProducerService producerService) {
        this.teamService = teamService;
        this.device = device;
        this.producerService = producerService;
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<String> getScore(
            @PathVariable("teamId") Long teamId) {

        log.debug("Get score for team {}", teamId);

        if (teamId == null || teamId == 0) {
            return ResponseEntity.badRequest().body("Team id can't be null or zero");
        }

        return teamService.findById(teamId)
                .map(team -> ResponseEntity.ok(team.getScore().toString()))
                .orElse(ResponseEntity.badRequest().body("Team does not exist"));
    }

    @PutMapping("/inc/{teamId}")
    public ResponseEntity<String> incrementScore(
            @PathVariable("teamId") Long teamId,
            @RequestParam("points") @DefaultValue("2") int points) {

        log.debug("Increment score for team {} with {} points", teamId, points);

        if (teamId == null || teamId == 0) {
            log.info("Team id can't be null");
            return ResponseEntity.badRequest().body("Team id can't be null or zero");
        }

        return teamService
                .findById(teamId)
                .filter(team -> team.getScore() + points <= 999)
                .map(team -> {
                    team.setScore(team.getScore() + points);
                    update(team);
                    return ResponseEntity.ok(team.getScore().toString());
                })
                .orElse(ResponseEntity.badRequest().body("Team not found or invalid score"));
    }

    @PutMapping("/dec/{teamId}")
    public ResponseEntity<?> decrementScore(
            @PathVariable("teamId") Long teamId,
            @RequestParam("points") @DefaultValue("1") int points) {

        log.debug("Decrement score for team {} with {}", teamId, points);

        if (teamId == null || teamId == 0) {
            return ResponseEntity.badRequest().body("Team id can't be null or zero");
        }

        return teamService
                .findById(teamId)
                .filter(team -> team.getScore() - points >= 0)
                .map(team -> {
                    team.setScore(team.getScore() - points);
                    update(team);
                    return ResponseEntity.ok(team.getScore().toString());
                })
                .orElse(ResponseEntity.badRequest().body("Team not found or invalid score"));
    }

    private void update(final Team team) {
        log.debug("Update team {}", team);

        if (team.isMirrored()) {
            log.debug("Score mirroring turned ON");

            if (team.getKey().equals(TeamType.A.toString())) {
                producerService.printHomeScore(team.getScore());
            } else {
                producerService.printVisitorsScore(team.getScore());
            }
        }

        teamService.save(team);

        device.setScore(team);
    }
}
