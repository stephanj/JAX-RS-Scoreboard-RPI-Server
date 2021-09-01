package org.janssen.scoreboard.dao;

import org.janssen.scoreboard.model.Token;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.List;

/**
 * @author Stephan Janssen
 */
@Singleton
@Lock(LockType.READ)
public class TokenDAO {

    @Inject
    private DAO dao;

    public Token create(final Token token) {
        return dao.create(token);
    }

    public Token find(final String token) {

        final List<Token> tokens = dao.queryFindByToken("token.find", token);

        if (tokens.size() == 0) {
            return null;
        } else {
            return tokens.get(0);
        }
    }

    public List<Token> listTokens() {
        return dao.find(Token.class, "select t from Token t", 0, 100);
    }

    public void delete(long id) {
        dao.delete(Token.class, id);
    }
}
