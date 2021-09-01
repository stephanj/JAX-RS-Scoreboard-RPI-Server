package org.janssen.scoreboard.model.type;

/**
 * @author Stephan Janssen
 */
public enum AgeCategory {

    SENIOREN("Senioren"),

    COOPERATIEF("Cooperatief"),

    U21("U21 Junioren"),

    U18("U18 Kadetten"),

    U16("U16 Miniemen"),

    U14("U14 Pupillen"),

    U12("U12 Benjamins"),

    U10("U10 Microben"),

    U8("U8 Premicroben"),

    U5("U5 Baby Basket");

    private String name;

    AgeCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
