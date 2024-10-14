package ru.nikidzawa.datingapp.store.entities.user;

import lombok.Getter;

@Getter
public enum GenderEnum {
    MALE   ("Парень", "\uD83D\uDC68", "\uD83D\uDC68 Парень"),
    FEMALE ("Девушка", "\uD83D\uDC69", "\uD83D\uDC69 Девушка");

    private final String name;
    private final String smile;
    private final String smileAndName;

    GenderEnum (String name, String smile, String smileAndName) {
        this.name = name;
        this.smile = smile;
        this.smileAndName = smileAndName;
    }
}
