package ru.nikidzawa.datingapp.store.entities.user;

import lombok.Getter;

@Getter
public enum GenderSearchEnum {
    MALE("Парни",
            "\uD83D\uDC68",
            "\uD83D\uDC68 Парни",
            "парням",
            "парню"
    ),
    FEMALE("Девушки",
            "\uD83D\uDC69",
            "\uD83D\uDC69 Девушки",
            "девушкам",
            "девушке"
    ),
    NO_DIFFERENCE ("Без разницы",
            "\uD83D\uDC68 \uD83D\uDC69",
            "\uD83D\uDC68 \uD83D\uDC69 Без разницы",
            "людям",
            "человеку"
    );

    private final String name;
    private final String smile;
    private final String smileAndName;
    private final String multiPrefix;
    private final String singlePrefix;

    GenderSearchEnum(String name, String smile, String smileAndName, String multiPrefix, String singlePrefix) {
        this.name = name;
        this.smile = smile;
        this.smileAndName = smileAndName;
        this.multiPrefix = multiPrefix;
        this.singlePrefix = singlePrefix;
    }
}
