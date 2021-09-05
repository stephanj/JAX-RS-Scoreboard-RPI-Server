package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.service.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/attention", produces = MediaType.APPLICATION_JSON_VALUE)
public class AttentionService {

    private final Logger log = LoggerFactory.getLogger(AttentionService.class);

    private final GPIOController gpioController;

    public AttentionService(GPIOController gpioController) {
        this.gpioController = gpioController;
    }

    @GetMapping
    public ResponseEntity<?> getAttention() {
        log.debug("Triggering attention buzzer");

        gpioController.setBuzz(GPIOType.ATTENTION);

        return ResponseUtil.ok();
    }
}
