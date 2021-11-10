package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.type.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static org.janssen.scoreboard.service.util.Constants.FOUR_MINUTES;
import static org.janssen.scoreboard.service.util.Constants.TEN_MINUTES_IN_SECONDS;

@Service
public class ClockService {

    private final Logger log = LoggerFactory.getLogger(ClockService.class);

    final private GameClockController gameClockController;
    final private GameService gameService;

    public ClockService(final GameClockController gameClockController,
                        final GameService gameService) {
        this.gameClockController = gameClockController;
        this.gameService = gameService;
    }

    /**
     * Start game clock.
     * @param game  the game
     * @return true when successfu
     */
    public boolean start(Game game) {
        log.debug("Start game clock {}", game);

        if (gameClockController.isNotRunning()) {
            if (game.getClock() > 0 && gameClockController.getSeconds() > 0) {
                gameClockController.start(game.getClock(), game.getGameType(), game.isMirrored());
            } else {
                log.info("Start clock but is 0");
                stop(game);
            }
            return true;
        }
        return false;
    }

    /**
     * Stop game clock.
     * @param game  the game
     * @return true when successful
     */
    public boolean stop(Game game) {
        log.debug("Stop game clock {}", game);

        if (gameClockController.isRunning()) {

            gameClockController.stop();
            game.setClock(gameClockController.getSeconds());

            gameService.update(game);
            return true;
        }
        return false;
    }

    /**
     * Start the pre-game count down clock.
     *
     * @param seconds   seconds to count down
     * @param mirrored  mirror count down on seconds score board
     */
    public void startCountdown(int seconds, boolean mirrored) {
        log.debug("Start game clock countdown {} secs and is mirrored {}", seconds, mirrored);

        if (gameClockController.isRunning()) {
            gameClockController.stop();
        }

        gameClockController.start(seconds, GameType.BASKET, mirrored);
    }

    /**
     * Increment the game clock with x seconds.
     * @param game  the game
     * @param seconds   the total seconds to increment
     * @return true when successful
     */
    public boolean incrementClock(Game game, int seconds) {
        if (gameClockController.isRunning()) {
            return false;
        }

        Integer clock = game.getClock();
        if (clock < TEN_MINUTES_IN_SECONDS) {
            if (game.getGameType() == GameType.BASKET_KIDS && clock >= FOUR_MINUTES) {
                return false;
            }
            clock += seconds;
            game.setClock(clock);
            gameService.update(game);

            gameClockController.setSeconds(clock);
            return true;
        }
        return false;
    }

    /**
     * Decrement the game clock with x seconds.
     * @param game  the game
     * @param seconds   the total seconds to decrement
     * @return true  when successful
     */
    public boolean decrementClock(Game game, int seconds) {
        if (gameClockController.isRunning()) {
            return false;
        }

        Integer clock = game.getClock();
        if (clock - seconds >= 0) {
            clock -= seconds;
            game.setClock(clock);
            gameService.update(game);

            gameClockController.setSeconds(clock);
            return true;
        }
        return false;
    }
}
