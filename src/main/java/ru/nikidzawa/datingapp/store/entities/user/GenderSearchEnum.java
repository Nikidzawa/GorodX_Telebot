package ru.nikidzawa.datingapp.store.entities.user;

import lombok.Getter;

@Getter
public enum GenderSearchEnum {
    MALE          (
            "Парни",
            "\uD83D\uDC68",
            "\uD83D\uDC68 Парни",
            "парням"
                ),
    FEMALE        (
            "Девушки",
            "\uD83D\uDC69",
            "\uD83D\uDC69 Девушки",
            "девушкам"),
    NO_DIFFERENCE (
            "Без разницы",
            "\uD83D\uDC68 \uD83D\uDC69",
            "\uD83D\uDC68 \uD83D\uDC69 Без разницы",
            "людям");

    private final String name;
    private final String smile;
    private final String smileAndName;
    private final String prefix;


    GenderSearchEnum (String name, String smile, String smileAndName, String prefix) {
        this.name = name;
        this.smile = smile;
        this.smileAndName = smileAndName;
        this.prefix = prefix;
    }
}
