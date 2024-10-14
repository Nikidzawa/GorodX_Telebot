package ru.nikidzawa.datingapp.telegramBot.stateMachines.commands;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

public interface CommandState {
    void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered);
}
