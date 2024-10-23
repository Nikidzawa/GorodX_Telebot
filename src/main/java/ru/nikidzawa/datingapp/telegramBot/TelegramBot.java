package ru.nikidzawa.datingapp.telegramBot;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nikidzawa.datingapp.store.entities.user.UserDetailsEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.entities.user.helpers.UserEntityAndRegisterStatus;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.messages.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.callBacks.CallBacksStateMachine;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.commands.CommandStateMachine;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateMachine;

import java.util.Optional;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    BotData botData;

    @Override
    public String getBotUsername() {
        return botData.getLogin();
    }

    @Override
    public String getBotToken() {
        return botData.getToken();
    }

    public BotFunctions botFunctions;

    @Autowired
    Messages messages;

    @Autowired
    StateMachine stateMachine;

    @Autowired
    CommandStateMachine commandStateMachine;

    @Autowired
    DataBaseService dataBaseService;

    @Autowired
    CacheService cacheService;

    @Autowired
    CallBacksStateMachine callBacksStateMachine;

    @PostConstruct
    @SneakyThrows
    private void init () {
        botFunctions = new BotFunctions(this);
        stateMachine.setBotFunctions(botFunctions);
        commandStateMachine.setBotFunctions(botFunctions);
        callBacksStateMachine.setBotFunctions(botFunctions);
    }

    @Override
    public void onUpdateReceived(Update update) {

        // Обработчик, если событие - это сообщение
        if (update.hasMessage()) {
            Message message = update.getMessage();

            // Получение данных о юзере
            Long userId = update.getMessage().getFrom().getId();
            UserEntityAndRegisterStatus registerStatus = checkRegister(dataBaseService.getUserById(userId));
            UserEntity userEntity = registerStatus.getUserEntity();
            boolean hasBeenRegistered = registerStatus.isHasBeenRegistered();

            UserDetailsEntity userDetails = dataBaseService.getUserDetails(userId);
            if (userDetails == null) {
                botFunctions.sendMessageAndKeyboard(userId, "Чтобы создать анкету, сначала необходимо приобрести доступ. Сделать это можно перейдя по кнопке \"Доступ\"", botFunctions.webAppButton());
                return;
            }

            // Проверка на то, включена ли анкета
            if (hasBeenRegistered && !userEntity.isActive()) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Мы ждали твоего возвращения! Твоя анкета снова включена, удачи в поисках \uD83D\uDE09");
                cacheService.evictAllUserCacheWithoutState(userId);
                userEntity.setActive(true);
                dataBaseService.saveUser(userEntity);
                stateMachine.goToMenu(userId, userEntity);
                return;
            }

            // Если сообщение - это команда (начинается с "/"), то используется стейт машина по контролю за командами
            if (message.hasText() && message.getText().startsWith("/")) {
                commandStateMachine.handleInput(userId, message, userEntity, userDetails, hasBeenRegistered);
            }
            // Если это не команда, значит происходит идентификация текущего состояния
            else {
                // Получаем состояние из кеша. Если его нет в кеше, значит произошла ошибка, статус ERROR или START
                Cache.ValueWrapper optionalCurrentState = cacheService.getCurrentState(userId);
                StateEnum currentState = optionalCurrentState == null ?
                        (hasBeenRegistered ? StateEnum.ERROR : StateEnum.START) : (StateEnum) optionalCurrentState.get();

                // Запуск стейт машины: в зависимости от текущего состояния, бот выполняет соответствующие действия
                try {
                    stateMachine.handleInput(currentState, userId, userEntity, message, hasBeenRegistered);
                } catch (Exception ex) {
                    stateMachine.handleInput(StateEnum.ERROR, userId, userEntity, message, hasBeenRegistered);
                }
            }
        }
        // Обработчик, если событие - это каллбек
        else if (update.hasCallbackQuery()) {
            Long userId = update.getCallbackQuery().getMessage().getChatId();
            Optional<UserEntity> optionalUser = dataBaseService.getUserById(userId);
            optionalUser.ifPresentOrElse(userEntity -> {
                if (!userEntity.isBanned() && userEntity.isActive()) {
                    String[] response = update.getCallbackQuery().getData().split(",");
                    callBacksStateMachine.handleCallback(response[0], userId, Long.parseLong(response[1]));
                } else {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getROLE_EXCEPTION());
                }
            }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_REGISTER()));
        }
        // Обработчик событий, если пользователь заблокировал или разблокировал бота
        else if (update.hasMyChatMember()) {
            // Получение данных о юзере
            Long userId = update.getMyChatMember().getFrom().getId();
            UserEntityAndRegisterStatus registerStatus = checkRegister(dataBaseService.getUserById(userId));
            UserEntity userEntity = registerStatus.getUserEntity();
            boolean hasBeenRegistered = registerStatus.isHasBeenRegistered();

            // Если пользователь заблокировал бота
            if (update.getMyChatMember().getNewChatMember().getStatus().equals("kicked")) {
                cacheService.evictAllUserCache(userId);
                // Если пользователь зарегистрирован, то отключаем анкету
                if (hasBeenRegistered) {
                    userEntity.setActive(false);
                    dataBaseService.saveUser(userEntity);
                }
            }
        }
    }

    // Если пользователь есть в кеше или в базе и у него есть id, значит пользователь уже зарегистрирован
    private UserEntityAndRegisterStatus checkRegister(Optional<UserEntity> optionalUser) {
        UserEntity userEntity = null;
        boolean hasBeenRegistered = false;

        if (optionalUser.isPresent() && optionalUser.get().getId() != null) {
            userEntity = optionalUser.get();
            hasBeenRegistered = true;
        }

        return new UserEntityAndRegisterStatus(userEntity, hasBeenRegistered);
    }
}