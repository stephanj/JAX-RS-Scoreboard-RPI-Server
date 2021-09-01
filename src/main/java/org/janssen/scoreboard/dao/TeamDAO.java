package org.janssen.scoreboard.dao;

import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.TeamType;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * @author Stephan Janssen
 */
@Singleton
@Lock(LockType.READ)
public class TeamDAO {

    @Inject
    private DAO dao;

    public Team create(final String name, final TeamType key, final boolean mirrored) {
        final Team team = new Team();
        team.setKey(key.toString());
        team.setName(name);
        team.setMirrored(mirrored);
        return dao.create(team);
    }

    public Team find(long id) {
        return dao.find(Team.class, id);
    }

    public Team update(final Team team) {
        team.setScore(team.getScore());
        team.setFouls(team.getFouls());
        return dao.update(team);
    }
}
