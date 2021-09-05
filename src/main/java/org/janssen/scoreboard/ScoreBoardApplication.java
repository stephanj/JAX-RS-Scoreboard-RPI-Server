package org.janssen.scoreboard;

import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.model.type.GPIOType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

import static org.janssen.scoreboard.service.util.Constants.ONE_SECOND_IN_MILLI;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ScoreBoardApplication {

    private final Logger log = LoggerFactory.getLogger(ScoreBoardApplication.class);

    public static void main(String[] args) {
		SpringApplication.run(ScoreBoardApplication.class, args);
	}
}
