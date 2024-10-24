package ru.nikidzawa.datingapp.telegramBot.stateMachines.commands;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.complaint.ComplaintEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.user.RoleEnum;
import ru.nikidzawa.datingapp.store.entities.user.UserDetailsEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.messages.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateMachine;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class CommandStateMachine {

    private final HashMap<String, CommandState> userCommands;

    private final HashMap<String, CommandState> adminCommands;

    @Autowired
    private StateMachine stateMachine;

    @Autowired
    private Messages messages;

    @Autowired
    private DataBaseService dataBaseService;

    @Autowired
    private CacheService cacheService;

    @Setter
    public BotFunctions botFunctions;

    public CommandStateMachine() {
        userCommands = new HashMap<>();
        userCommands.put("/start", new Start());
        userCommands.put("/myId", new MyId());
        userCommands.put("/menu", new Menu());
        userCommands.put("/faq", new FAQ());
        userCommands.put("/error", new Error());
        userCommands.put("/complaint", new SendComplaint());

        adminCommands = new HashMap<>();
        adminCommands.put("/show_errors", new ShowErrors());
        adminCommands.put("/analysis", new Analysis());
        adminCommands.put("/complaints", new Complaints());
        adminCommands.put("/roleController", new RoleController());
    }

    public void handleInput(long userId, Message message, UserEntity userEntity, UserDetailsEntity userDetails, boolean hasBeenRegistered) {
        String messageText = message.getText();
        CommandState commandState = null;

        // Если пользователь не зарегистрирован, доступна только команда /start
        if (!hasBeenRegistered) {
            if (messageText.equals("/start")) {
                commandState = userCommands.get(messageText);
                commandState.handleInput(userId, message, userEntity, hasBeenRegistered);
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_REGISTER());
            }
            return;
        }

        RoleEnum currentUserRole = userDetails.getRole();

        // Команды для пользователей
        if (currentUserRole == RoleEnum.USER) {
            commandState = userCommands.get(messageText);
            if (commandState != null) {
                commandState.handleInput(userId, message, userEntity, hasBeenRegistered);
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Команда не найдена. Если вы не хотели указывать команду, то не начинайте сообщение со знака /");
            }
        }
        // Команды для администраторов
        else if (currentUserRole == RoleEnum.ADMIN || currentUserRole == RoleEnum.SUPER_ADMIN) {
            commandState = userCommands.get(messageText) == null ? adminCommands.get(messageText) : userCommands.get(messageText);

            if (commandState != null) {
                commandState.handleInput(userId, message, userEntity, hasBeenRegistered);
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Команда не найдена. Если вы не хотели указывать команду, то не начинайте сообщение со знака /");
            }
        }
    }

    public void getComplaint(long userId) {
        List<ComplaintEntity> complainEntities = dataBaseService.findAllComplaints();
        Optional<ComplaintEntity> optionalComplain = complainEntities.stream().findAny();
        optionalComplain.ifPresentOrElse(complainEntity -> {
            UserEntity complainUser = dataBaseService.getUserById(complainEntity.getComplaintUserId()).get();
            botFunctions.sendMessageAndRemoveKeyboard(
                    userId,
                    "Жалоба номер: " + complainEntity.getId() +
                            "\nОбщее число жалоб на пользователя: " + dataBaseService.getComplainCountByUser(complainEntity.getComplaintUserId())
            );
            botFunctions.sendDatingProfile(userId, complainUser);
            botFunctions.sendMessageAndKeyboard(userId,
                    "Описание жалобы: " + complainEntity.getDescription(),
                    botFunctions.judgeButtons(complainEntity.getComplaintUserId())
            );
        }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, "Больше жалоб не поступало"));
    }

    private class Start implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            if (hasBeenRegistered) {
                if (userEntity.isActive()) {
                    stateMachine.goToMenu(userId, userEntity);
                }
            } else {
                stateMachine.handleInput(StateEnum.START, userId, null, message, false);
            }
        }
    }

    private class MyId implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            botFunctions.sendMessageNotRemoveKeyboard(userId, String.valueOf(userId));
        }
    }

    private class Menu implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            stateMachine.goToMenu(userId, userEntity);
        }
    }

    private class Error implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            cacheService.setState(userId, StateEnum.SEND_ERROR);
            botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getSEND_ERROR());
        }
    }

    private class FAQ implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            cacheService.setState(userId, StateEnum.FAQ);
            botFunctions.sendMessageAndKeyboard(userId, messages.getFAQ(), botFunctions.faqButtons());
        }
    }

    private class SendComplaint implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutStateAndAssessment(userId);
            Long complaintReceiverId = cacheService.getUserAssessmentId(userId);
            if (complaintReceiverId != null) {
                cacheService.putComplaintUser(userId, complaintReceiverId);
                botFunctions.sendMessageAndKeyboard(userId, messages.getSEND_COMPLAINT(), botFunctions.cancelButton());
                cacheService.setState(userId, StateEnum.FIND_PEOPLES_COMPLAIN);
            } else
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пожаловаться на пользователя можно только во время просмотра анкет");
        }
    }

    private class Analysis implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            botFunctions.sendMessageAndRemoveKeyboard(userId, "Идёт анализ, пожалуйста, подождите...");
            String[] results = dataBaseService.findTop10CitiesByUserCount();
            Long size = dataBaseService.getCountActiveAndNotBannedUsers();
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("Рейтинг топ 10 популярных городов:").append("\n");
            for (int i = 0; i < results.length; i++) {
                String[] cityAndNumber = results[i].split(",");
                String city = cityAndNumber[0];
                String count = cityAndNumber[1];
                stringBuilder.append(i + 1).append(". ").append(city).append(" - ").append(count).append(" ").append(wordParser(count)).append("\n");
            }

            stringBuilder.append("\n").append("Число активных пользователей: ").append(size);

            botFunctions.sendMessageNotRemoveKeyboard(userId, stringBuilder.toString());
        }

        private String wordParser (String count) {
            if (count.equals("1")) {
                return "анкета";
            } else if (count.equals("2") || count.equals("3") || count.equals("4")) {
                return "анкеты";
            } else {
                return "анкет";
            }
        }
    }

    private class ShowErrors implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            List<ErrorEntity> errorEntities = dataBaseService.findAllErrors();
            Optional<ErrorEntity> optionalError = errorEntities.stream().findAny();
            optionalError.ifPresentOrElse(errorEntity -> {
                botFunctions.sendMessageNotRemoveKeyboard(
                        userId,
                        "Ошибка номер: " + errorEntity.getId() + "\nОписание ошибки: " + errorEntity.getDescription()
                );
                dataBaseService.deleteError(errorEntity);
            }, () -> {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Больше жалоб не поступало");
                stateMachine.goToMenu(userId, userEntity);
            });
        }
    }

    private class Complaints implements CommandState {
        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            getComplaint(userId);
        }
    }

    private class RoleController implements CommandState {

        @Override
        public void handleInput(long userId, Message message, UserEntity userEntity, boolean hasBeenRegistered) {
            cacheService.evictAllUserCacheWithoutState(userId);
            botFunctions.sendMessageNotRemoveKeyboard(userId, "Вы переходите в систему контроля ролей...");

            stateMachine.goToRoleController(dataBaseService.getUserDetails(userId).getRole(), userId);
        }


    }
}