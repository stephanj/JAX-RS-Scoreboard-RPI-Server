package org.janssen.scoreboard.dao;

import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.AgeCategory;
import org.janssen.scoreboard.model.type.GameType;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.List;

import static org.janssen.scoreboard.service.util.Constants.*;

/**
 * @author Stephan Janssen
 */
@Singleton
@Lock(LockType.READ)
public class GameDAO {

    @Inject
    private DAO dao;

    public Game create(final Team teamA,
                       final Team teamB,
                       final int typeNumber,
                       final int age,
                       final String court,
                       final String userName,
                       final boolean mirrored) {

        final Game game = new Game();

        game.setUserName(userName);
        game.setCourt(court);
        game.setMirrored(mirrored);

        final GameType type = GameType.values()[typeNumber];
        game.setGameType(type);
        setGameClock(game);

        final AgeCategory ageCategory = AgeCategory.values()[age];
        game.setAgeCategory(ageCategory);

        game.setTeamA(teamA);
        game.setTeamB(teamB);
        return dao.create(game);
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


    public List<Game> list(int first, int max) {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return dao.queryFindByCreation(Game.class, "game.list", cal.getTime(), first, max);
    }

    public Long totalGames() {
        return (Long)dao.count("game.count");
    }

    public Game find(long id) {
        return dao.find(Game.class, id);
    }

    public void delete(long id) {
        dao.delete(Game.class, id);
    }

    public Game update(final Game game) {
        game.setQuarter(game.getQuarter());
        game.setClock(game.getClock());
        return dao.update(game);
    }
}
