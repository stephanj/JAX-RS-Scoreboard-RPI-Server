package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.model.DatedModel;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.AgeCategory;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.GameType;
import org.janssen.scoreboard.model.type.TeamType;
import org.janssen.scoreboard.service.broadcast.ProducerService;
import org.janssen.scoreboard.service.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.janssen.scoreboard.service.util.Constants.FOUR_MINUTES;
import static org.janssen.scoreboard.service.util.Constants.TEN_MINUTES_IN_SECONDS;

@Service
public class GameService {

    private final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameClockController gameClockController;

    private final GameRepository gameRepository;

    private final TeamService teamService;

    private final GPIOController gpioController;

    private final DeviceController device;

    private final ProducerService producerService;

    // private Game currentGame = null;

    public GameService(GameRepository gameRepository,
                       GameClockController gameClockController,
                       TeamService teamService,
                       DeviceController device,
                       GPIOController gpioController,
                       ProducerService producerService) {
        this.gameRepository = gameRepository;
        this.gameClockController = gameClockController;
        this.teamService = teamService;
        this.gpioController = gpioController;
        this.producerService = producerService;
        this.device = device;
    }

    public Optional<Game> findGameById(Long gameId) {
        return gameRepository.findById(gameId);
    }

    public List<Game> findAll() {
        return gameRepository
                .findAll()
                .stream()
                .sorted(Comparator.comparing(DatedModel::getCreatedOn).reversed())
                .collect(Collectors.toList());
    }

    public void setGameClock(final Game game) {

        final GameType type = game.getGameType();

        switch(type) {
            case BASKET:
                game.setClock(TEN_MINUTES_IN_SECONDS);
                break;

            case BASKET_KIDS:
                game.setClock(FOUR_MINUTES);
                break;

            case FOOTBALL:
                game.setClock(0);
                break;
        }
    }

    @Transactional
    public void incrementGameQuarter(Game game) {

        // Verify how many quarters mini football can have... 2 ?
        game.incrementQuarter();

        resetTeamFouls(game);

        // Reset the clock based on game type
        setGameClock(game);
        gameClockController.setSeconds(game.getClock());

        if ((game.getGameType() == GameType.BASKET && game.getQuarter() == 3) ||
            (game.getGameType() == GameType.BASKET_KIDS && game.getQuarter() == 5)) {
            resetTimeoutLeds(game);
        }

        // Show the 24s LEDs
        gpioController.showTwentyFourSeconds(true);

        gameRepository.save(game);
    }

    public void resetTimeoutLeds(final Game game) {

        log.info("game.isMirrored? " + game.isMirrored());

        if (game.isMirrored()) {
            log.info("QuarterService mirroring turn ON");

            producerService.printHomeTimeout(0);
            producerService.printVisitorsTimeout(0);
        } else {
            log.info("QuarterService mirroring turn OFF");
        }

        final Team teamA = game.getTeamA();
        teamA.setTimeOut(0);
        teamService.save(teamA);

        final Team teamB = game.getTeamB();
        teamB.setTimeOut(0);
        teamService.save(teamB);

        gpioController.setLed(GPIOType.TIME_OUT_H1, false);
        gpioController.setLed(GPIOType.TIME_OUT_H2, false);
        gpioController.setLed(GPIOType.TIME_OUT_V1, false);
        gpioController.setLed(GPIOType.TIME_OUT_V2, false);
    }

    public void resetTeamFouls(final Game game) {
        if (game.getGameType() == GameType.BASKET ||
            (game.getGameType() == GameType.BASKET_KIDS &&  game.getQuarter() % 2 == 0)) {   // every 2 quarters

            final Team teamA = game.getTeamA();
            teamA.setFouls(0);
            teamService.save(teamA);

            final Team teamB = game.getTeamB();
            teamB.setFouls(0);
            teamService.save(teamB);
        }
    }

    /**
     * Delete a game and clear scoreboard.
     *
     * @param game the game to delete
     */
    @Transactional
    public void delete(Game game) {
        gameRepository.delete(game);
        device.clearBoard();
    }

    @Transactional
    public void resetFouls(Long gameId) {

        gameRepository
            .findById(gameId)
            .ifPresent(game -> {
                final Team teamA = game.getTeamA();
                teamA.setFouls(0);
                update(teamA);

                final Team teamB = game.getTeamB();
                teamB.setFouls(0);
                update(teamB);
            });
    }

    @Transactional
    public void update(final Team team) {

        log.debug("team.isMirrored? " + team.isMirrored());

        if (team.isMirrored()) {
            log.info("FoulService mirroring is turned ON");

            if (team.getKey().equals(TeamType.A.toString())) {
                producerService.printFoulsA(team.getFouls());
            } else {
                producerService.printFoulsB(team.getFouls());
            }

            producerService.setPlayerFoul(team.getFouls());
        }

        teamService.save(team);

        device.setFoul(team);

        if (team.getFouls() != 0) {
            device.setPlayerFoul(team.getFouls());
        }
    }

    /**
     * Create a new game.
     *
     * @param teamNameA team name A
     * @param teamNameB team name B
     * @param gameType  game type
     * @param ageCategory age category
     * @param court court name
     * @param mirrored mirrored scoreboard
     */
    @Transactional
    public Game newGame(String teamNameA,
                        String teamNameB,
                        int gameType,
                        int ageCategory,
                        String court,
                        boolean mirrored) {

        // Clock might be running in countdown mode
        if (gameClockController.isRunning()) {
            gameClockController.stop();
        }

        final Team teamA = teamService.create(teamNameA, TeamType.A, mirrored);
        log.info("TeamA mirroring turned on? " + teamA.isMirrored());

        final Team teamB = teamService.create(teamNameB, TeamType.B, mirrored);
        log.info("TeamB mirroring turned on? " + teamB.isMirrored());

        final Game game = create(teamA, teamB, gameType, ageCategory, court, mirrored);

        log.info("Game mirroring turned on? " + game.isMirrored());

        if (game.isMirrored()) {
            // Init slave scoreboard
            producerService.newGame();
        }

        device.setGame(game);

        // Reset the timeout LEDs
        gpioController.setLed(GPIOType.TIME_OUT_V1, false);
        gpioController.setLed(GPIOType.TIME_OUT_V2, false);

        gpioController.setLed(GPIOType.TIME_OUT_H1, false);
        gpioController.setLed(GPIOType.TIME_OUT_H2, false);

        gpioController.showTwentyFourSeconds(true);

        return game;
    }

    @Transactional
    public Game create(final Team teamA,
                       final Team teamB,
                       final int typeNumber,
                       final int age,
                       final String court,
                       final boolean mirrored) {

        final Game game = new Game();

        game.setUserName("na");
        game.setCourt(court);
        game.setMirrored(mirrored);

        final GameType type = GameType.values()[typeNumber];
        game.setGameType(type);
        setGameClock(game);

        final AgeCategory ageCategory = AgeCategory.values()[age];
        game.setAgeCategory(ageCategory);

        game.setTeamA(teamA);
        game.setTeamB(teamB);
        return gameRepository.save(game);
    }

    @Transactional
    public void update(final Game game) {
        game.setQuarter(game.getQuarter());
        game.setClock(game.getClock());
        gameRepository.save(game);
    }
}
