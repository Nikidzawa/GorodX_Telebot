package ru.nikidzawa.datingapp.telegramBot.stateMachines.commands;

import ru.nikidzawa.datingapp.store.entities.user.UserDetailsEntity;

/**
 * @author Nikidzawa
 */
public interface RoleState {
    void handleInput(Long userId, UserDetailsEntity userDetailsEntity);

}
