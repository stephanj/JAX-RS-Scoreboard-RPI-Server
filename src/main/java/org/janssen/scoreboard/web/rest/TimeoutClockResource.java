package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.controller.TimeoutClockController;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.TeamType;
import org.janssen.scoreboard.service.broadcast.ProducerService;
import org.janssen.scoreboard.service.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/timeout", produces = MediaType.APPLICATION_JSON_VALUE)
public class TimeoutClockResource {

    private final Logger log = LoggerFactory.getLogger(TimeoutClockResource.class);
    
    private final TeamRepository teamRepository;

    private final TimeoutClockController timeoutClockController;

    private final GPIOController gpioController;

    private final ProducerService producerService;

    private final GameClockController gameClockController;

    public TimeoutClockResource(
            TeamRepository teamRepository, 
            TimeoutClockController timeoutClockController, 
            GPIOController gpioController, 
            ProducerService producerService, 
            GameClockController gameClockController) {
        this.teamRepository = teamRepository;
        this.timeoutClockController = timeoutClockController;
        this.gpioController = gpioController;
        this.producerService = producerService;
        this.gameClockController = gameClockController;
    }

    /**
     * Start the one minute timeout clock counter.
     * @return status
     */
    @PutMapping("/start")
    public ResponseEntity<?> startTimeoutClockCounter() {

        log.debug("start timeout clock counter");
        
        if (gameClockController.isRunning()) {
            return ResponseEntity.badRequest().body("Can't start timeout when clock is running");
        }
        
        // Start timeout clock
        if (timeoutClockController.isNotRunning()) {
            timeoutClockController.start();
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Timeout already running");
        }
    }

    @PutMapping("/inc/{teamId}")
    public ResponseEntity<?> incrementTimeout(@PathVariable("teamId") Long teamId) {

        log.debug("Increment timeout for team {} ", teamId);
        
        if (teamId == null || teamId == 0) {
            return ResponseEntity.badRequest().body("Team id can't be null or zero");
        }

        return teamRepository
                .findById(teamId)
                .map(team -> {
                    if (team.getTimeOut() + 1 > 2) {
                        team.setTimeOut(0);                         // reset timeout counter
                    } else {
                        team.setTimeOut(team.getTimeOut() + 1);     // increment timeout counter
                    }

                    teamRepository.save(team);                      // save team
                    setTimeOutLed(team);                            // set timeout led
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.badRequest().body("Team does not exist"));
    }

    private void setTimeOutLed(final Team team) {

        if (team.getKey().equals(TeamType.A.toString())) {
            if (team.getTimeOut() == 0) {
                gpioController.setLed(GPIOType.TIME_OUT_H1, false);
                gpioController.setLed(GPIOType.TIME_OUT_H2, false);
            } else if (team.getTimeOut() == 1) {
                gpioController.setLed(GPIOType.TIME_OUT_H1, true);
            } else {
                gpioController.setLed(GPIOType.TIME_OUT_H2, true);
            }

            if (team.isMirrored()) {
                log.debug("Print mirrored timeout LED(A) : {}", team.getTimeOut());
                producerService.printHomeTimeout(team.getTimeOut());
            }

        } else {
            if (team.getTimeOut() == 0) {
                gpioController.setLed(GPIOType.TIME_OUT_V1, false);
                gpioController.setLed(GPIOType.TIME_OUT_V2, false);
            }
            else if (team.getTimeOut() == 1) {
                gpioController.setLed(GPIOType.TIME_OUT_V1, true);
            } else {
                gpioController.setLed(GPIOType.TIME_OUT_V2, true);
            }

            if (team.isMirrored()) {
                log.debug("Print mirrored timeout LED(B): {}", team.getTimeOut());
                producerService.printVisitorsTimeout(team.getTimeOut());
            }
        }
    }

    // Stop timeout and Restore 24s
    @PutMapping("/stop")
    public ResponseEntity<?> stopClock() {
        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Timeout is not running");
        }
    }
}
