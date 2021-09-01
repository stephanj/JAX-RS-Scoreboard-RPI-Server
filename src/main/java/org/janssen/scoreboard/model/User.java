package org.janssen.scoreboard.model;

import org.janssen.scoreboard.model.type.AgeCategory;
import org.janssen.scoreboard.model.type.GameType;

import java.util.StringTokenizer;

/**
 * @author Stephan Janssen
 */
@SuppressWarnings("unused")
public class User {

    private String userName;

    private String fullName;

    private String email;

    private AgeCategory ageCategory;

    private GameType gameType;

    private String password;

    public User() {
    }

    public User(final String userName, final String property) {
        StringTokenizer tk = new StringTokenizer(property, ",");
        this.userName = userName;
        this.fullName = tk.nextToken();
        this.ageCategory = AgeCategory.valueOf(tk.nextToken());
        this.gameType = GameType.valueOf(tk.nextToken());
        this.email = tk.nextToken();
        this.password = tk.nextToken();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public AgeCategory getAgeCategory() {
        return ageCategory;
    }

    public void setAgeCategory(final AgeCategory ageCategory) {
        this.ageCategory = ageCategory;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(final GameType gameType) {
        this.gameType = gameType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }
}
