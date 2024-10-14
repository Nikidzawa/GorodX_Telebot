package ru.nikidzawa.datingapp.store.entities.user;

import lombok.Getter;

@Getter
public enum RoleEnum {
    USER("Пользователь", "\uD83D\uDC76"),
    ADMIN("Администратор", "\uD83D\uDC6E"),
    SUPER_ADMIN("Создатель", "\uD83D\uDC51");

    private final String name;
    private final String smile;

    RoleEnum(String name, String smile) {
        this.name = name;
        this.smile = smile;
    }

}
