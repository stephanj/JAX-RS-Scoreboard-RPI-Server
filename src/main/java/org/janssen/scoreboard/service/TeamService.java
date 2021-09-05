package org.janssen.scoreboard.service;

import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.service.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional
    public void save(Team team) {
        teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public Optional<Team> findById(Long teamId) {
        return teamRepository.findById(teamId);
    }
}
