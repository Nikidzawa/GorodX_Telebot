package ru.nikidzawa.datingapp.telegramBot.stateMachines.states;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.like.LikeContentType;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserAvatar;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserSiteAccount;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.services.api.GeocodingApi;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.Geocode;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Component
public class StateMachine {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private DataBaseService dataBaseService;

    @Autowired
    private Messages messages;

    @Autowired
    private JsonParser jsonParser;

    @Autowired
    private GeocodingApi geocodingApi;

    private final HashMap<StateEnum, State> textStates;
    private final HashMap<StateEnum, State> photoStates;
    private final HashMap<StateEnum, State> locationStates;
    private final HashMap<StateEnum, State> audioStates;
    private final HashMap<StateEnum, State> videoStates;
    private final HashMap<StateEnum, State> videoNoteStates;

    @Setter
    public BotFunctions botFunctions;


    public StateMachine() {
        textStates = new HashMap<>();
        photoStates = new HashMap<>();
        locationStates = new HashMap<>();
        audioStates = new HashMap<>();
        videoStates = new HashMap<>();
        videoNoteStates = new HashMap<>();

        textStates.put(StateEnum.START, new Start());
        textStates.put(StateEnum.START_HANDLE, new StartHandle());
        textStates.put(StateEnum.WELCOME_BACK, new WelcomeBack());
        textStates.put(StateEnum.WELCOME_BACK_HANDLE, new WelcomeBackHandle());

        textStates.put(StateEnum.LEFT, new Left());

        textStates.put(StateEnum.ASK_NAME, new AskName());
        textStates.put(StateEnum.ASK_AGE, new AskAge());
        textStates.put(StateEnum.ASK_CITY, new AskCity());
        textStates.put(StateEnum.ASK_HOBBY, new AskHobby());
        textStates.put(StateEnum.ASK_ABOUT_ME, new AskAboutMe());
        textStates.put(StateEnum.RESULT, new Result());

        textStates.put(StateEnum.MENU, new Menu());
        textStates.put(StateEnum.SUPER_MENU, new SuperMenu());

        textStates.put(StateEnum.EDIT_NAME, new EditName());
        textStates.put(StateEnum.EDIT_AGE, new EditAge());
        textStates.put(StateEnum.EDIT_CITY, new EditCity());
        textStates.put(StateEnum.EDIT_RESULT, new EditResult());
        textStates.put(StateEnum.MY_PROFILE, new MyProfile());
        textStates.put(StateEnum.EDIT_HOBBY, new EditHobby());
        textStates.put(StateEnum.EDIT_ABOUT_ME, new EditAboutMe());
        textStates.put(StateEnum.ASK_BEFORE_OFF, new AskBeforeOff());
        textStates.put(StateEnum.ASK_AVATAR, new SkipAddAvatar());
        textStates.put(StateEnum.FIND_PEOPLES, new FindPeoples());
        textStates.put(StateEnum.SHOW_WHO_LIKED_ME, new ShowWhoLikedMe());
        textStates.put(StateEnum.SHOW_PROFILES_WHO_LIKED_ME, new ShowProfilesWhoLikedMe());
        textStates.put(StateEnum.STOP_SHOW_PROFILES_WHO_LIKED_ME, new StopShowProfilesWhoLikedMe());
        textStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageText());
        textStates.put(StateEnum.CALL_BACK_QUERY_COMPLAIN, new CallbackQueryComplain());
        textStates.put(StateEnum.FAQ, new FAQ());
        textStates.put(StateEnum.FAQ_RESPONSE, new FaqResponse());
        textStates.put(StateEnum.SEND_ERROR, new SendError());

        photoStates.put(StateEnum.ASK_AVATAR, new AddAvatarPhoto());
        photoStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessagePhoto());

        locationStates.put(StateEnum.ASK_CITY, new AskCityGeo());
        locationStates.put(StateEnum.EDIT_CITY, new EditCityGeo());

        audioStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageAudio());
        videoStates.put(StateEnum.ASK_AVATAR, new AddAvatarVideo());
        videoStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageVideo());
        videoNoteStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageVideoNote());
    }

    public void handleInput(StateEnum currentState, Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
        State state = null;
        if (message.hasText() || message.isSuperGroupMessage()) {state = textStates.get(currentState);}
        else if (message.hasPhoto()) {
            state = photoStates.get(currentState);
        } else if (message.hasLocation()) {
            state = locationStates.get(currentState);
        } else if (message.hasVoice()) {
            state = audioStates.get(currentState);
        } else if (message.hasVideo()) {
            state = videoStates.get(currentState);
        } else if (message.hasVideoNote()) {
            state = videoNoteStates.get(currentState);
        }

        if (state != null) {
            state.handleInput(userId, userEntity, message, hasBeenRegistered);
        } else {
            botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getINVALID_FORMAT_EXCEPTION());
        }
    }

    private class Start implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveKeyboard(userId,
                    "Привет, " + message.getFrom().getFirstName() + "\n" +
                            "Я рада, что ты присоединилась к нашему сообществу \uD83D\uDC96\n" +
                            "\n" +
                            "Здесь ты найдешь не только подруг, но и множество возможностей для личного роста и творческого самовыражения на наших мероприятиях.\n" +
                            "Давай вместе создадим яркие и запоминающиеся моменты!");
            botFunctions.sendMessageAndKeyboard(userId, "Давай заполним тебе анкету?", botFunctions.startButton());
            cacheService.setState(userId, StateEnum.START_HANDLE);

        }
    }

    private class StartHandle implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Начнём!")) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_NAME());
            }
        }
    }

    private class WelcomeBack implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(userId,
                    "Привет, " + message.getFrom().getFirstName() + "\n" +
                            "Я рада, что ты вернулась в наше сообщество! \uD83D\uDC96\n" +
                            "\n" +
                            "Давай включим тебе анкету?\n", botFunctions.welcomeBackButton());
            cacheService.setState(userId, StateEnum.WELCOME_BACK_HANDLE);
        }
    }

    private class WelcomeBackHandle implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Включить анкету")) {
                goToMenu(userId, userEntity);
                userEntity.setActive(true);
                dataBaseService.saveUser(userEntity);
            }
        }
    }

    private class Left implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(userId, messages.getLEFT(), botFunctions.restartButton());
            dataBaseService.getUserById(userId).ifPresent(user -> {
                user.setActive(false);
                dataBaseService.saveUser(user);
            });
            cacheService.evictState(userId);
        }
    }

    private class AskName implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 100) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNAME_LIMIT_SYMBOLS_EXCEPTIONS());
                return;
            }

            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_CITY(), botFunctions.customLocationButtons(userEntity.getLocation()));
            }
            else {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_CITY(), botFunctions.locationButton());
            }

            new Thread(() -> {
                cacheService.setState(userId, StateEnum.ASK_CITY);
                UserEntity user = cacheService.getCachedUser(userId);
                user.setName(message.getText());
                cacheService.putCachedUser(userId, user);
            }).start();
        }
    }

    private class AskCity implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 100) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getCITY_LIMIT_SYMBOLS_EXCEPTIONS());
                return;
            }

            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            } else {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_AGE());
            }
            cacheService.setState(userId, StateEnum.ASK_AGE);

            new Thread(() -> {
                UserEntity user = cacheService.getCachedUser(userId);
                user.setLocation(messageText);
                Geocode coordinates = jsonParser.parseGeocode(geocodingApi.getCoordinates(messageText));
                user.setLongitude(coordinates.getLon());
                user.setLatitude(coordinates.getLat());
                cacheService.putCachedUser(userId, user);
            }).start();
        }
    }
    private class AskCityGeo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            } else {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_AGE());
            }
            cacheService.setState(userId, StateEnum.ASK_AGE);

            new Thread(() -> {
                UserEntity cachedUser = cacheService.getCachedUser(userId);
                Location location = message.getLocation();
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();

                cachedUser.setLongitude(longitude);
                cachedUser.setLatitude(latitude);
                cachedUser.setLocation(jsonParser.getName(geocodingApi.getCityName(latitude, longitude)));

                cacheService.putCachedUser(userId, cachedUser);
            }).start();
        }
    }

    private class AskAge implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            UserEntity user = cacheService.getCachedUser(userId);
            int age;
            try {
                age = Integer.parseInt(message.getText());
                if (age >= 100 || age < 6) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getAGE_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
            } catch (Exception ex) {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getIS_NOT_A_NUMBER_EXCEPTION());
                return;
            }
            user.setAge(age);
            cacheService.putCachedUser(userId, user);
            if (hasBeenRegistered && userEntity.getHobby() != null) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_HOBBY(), botFunctions.skipAndCustomButtons(messages.getUNEDITED_HOBBY()));
            } else {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_HOBBY(), botFunctions.skipButton());
            }
            cacheService.setState(userId, StateEnum.ASK_HOBBY);
        }
    }
    private class AskHobby implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 150) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getASK_HOBBY());
                return;
            }
            if (!messageText.equals("Пропустить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                if (messageText.equals("Оставить текущие хобби") && userEntity != null) {
                    user.setHobby(userEntity.getHobby());
                } else {
                    user.setHobby(messageText);
                }
                cacheService.putCachedUser(userId, user);
            }
            if (hasBeenRegistered && userEntity.getAboutMe() != null) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_ABOUT_ME(), botFunctions.skipAndCustomButtons(messages.getUNEDITED_ABOUT_ME()));
            } else {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_ABOUT_ME(), botFunctions.skipButton());
            }
            cacheService.setState(userId, StateEnum.ASK_ABOUT_ME);
        }
    }

    private class AskAboutMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 1000) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getABOUT_ME_LIMIT_SYMBOLS_EXCEPTIONS());
                return;
            }
            if (!messageText.equals("Пропустить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                if (messageText.equals("Оставить текущую информацию") && hasBeenRegistered) {
                    user.setAboutMe(userEntity.getAboutMe());
                } else {
                    user.setAboutMe(message.getText());
                }
                cacheService.putCachedUser(userId, user);
            }
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_PHOTO(), botFunctions.customButton(messages.getSKIP_ADD_AVATAR()));
            } else {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_PHOTO());
            }
            cacheService.setState(userId, StateEnum.ASK_AVATAR);
        }
    }

    private class AddAvatarPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            List<UserAvatar> avatars = cacheService.getUserAvatars(userId);
            int avatarSize = avatars.size() + 1;
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, "Фотография загружена ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSKIP_ADD_AVATAR(), messages.getSTOP_ADD_AVATAR()));
            } else {
                botFunctions.sendMessageAndKeyboard(userId, "Фотография загружена ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSTOP_ADD_AVATAR()));
            }
            avatars.add(UserAvatar.builder()
                    .file(botFunctions.loadPhoto(message.getPhoto()))
                    .isPhoto(true)
                    .build());
            cacheService.putUserAvatars(userId, avatars);
            if (avatarSize == 3) {
                UserEntity user = cacheService.getCachedUser(userId);
                user.setUserAvatars(avatars);
                if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, user);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
                } else {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getRESULT());
                botFunctions.sendDatingProfile(userId, user);
                botFunctions.sendMessageAndKeyboard(userId, "Всё верно?", botFunctions.resultButtons());
                cacheService.setState(userId, StateEnum.RESULT);
                }
            } else {
                cacheService.setState(userId, StateEnum.ASK_AVATAR);
            }
        }
    }

    private class AddAvatarVideo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            List<UserAvatar> avatars = cacheService.getUserAvatars(userId);
            int avatarSize = avatars.size() + 1;
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, "Видео загружено ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSKIP_ADD_AVATAR(), messages.getSTOP_ADD_AVATAR()));
            } else {
                botFunctions.sendMessageAndKeyboard(userId, "Видео загружено ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSTOP_ADD_AVATAR()));
            }
            avatars.add(UserAvatar.builder()
                    .file(message.getVideo().getFileId())
                    .isPhoto(false)
                    .build());
            cacheService.putUserAvatars(userId, avatars);
            if (avatarSize == 3) {
                UserEntity user = cacheService.getCachedUser(userId);
                user.setUserAvatars(avatars);
                if (hasBeenRegistered) {
                    botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                    botFunctions.sendDatingProfile(userId, user);
                    cacheService.setState(userId, StateEnum.EDIT_RESULT);
                } else {
                    botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getRESULT());
                    botFunctions.sendDatingProfile(userId, user);
                    botFunctions.sendMessageAndKeyboard(userId, "Всё верно?", botFunctions.resultButtons());
                    cacheService.setState(userId, StateEnum.RESULT);
                }
            } else {
                cacheService.setState(userId, StateEnum.ASK_AVATAR);
            }
        }
    }

    private class SkipAddAvatar implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals(messages.getSKIP_ADD_AVATAR()) && hasBeenRegistered) {
                goToProfile(userId, userEntity);
            } else if (messageText.equals(messages.getSTOP_ADD_AVATAR())) {
                List<UserAvatar> userAvatars = cacheService.getUserAvatars(userId);
                if (!userAvatars.isEmpty()) {
                    UserEntity user = cacheService.getCachedUser(userId);
                    user.setUserAvatars(userAvatars);
                    cacheService.putCachedUser(userId, user);
                    if (hasBeenRegistered) {
                        botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                        botFunctions.sendDatingProfile(userId, user);
                        cacheService.setState(userId, StateEnum.EDIT_RESULT);
                    } else {
                        botFunctions.sendMessageAndKeyboard(userId, messages.getRESULT(), botFunctions.resultButtons());
                        botFunctions.sendDatingProfile(userId, user);
                        cacheService.setState(userId, StateEnum.RESULT);
                    }
                } else {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, "Необходимо добавить хотя бы одну фотографию");
                }
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getINVALID_FORMAT_EXCEPTION());
            }
        }
    }

    private class Result implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Заполнить анкету заново")) {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_NAME());
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
                cacheService.evictUserAvatars(userId);
            } else if (message.getText().equals("Продолжить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                goToMenu(userId, user);
                new Thread(() -> {
                    List<UserAvatar> userAvatars = dataBaseService.saveAllUserAvatars(cacheService.getUserAvatars(userId));
                    user.setUserAvatars(userAvatars);
                    user.setActive(true);
                    dataBaseService.saveUser(user);
                    UserSiteAccount userSiteAccount = dataBaseService.saveUserSiteAccount(
                            UserSiteAccount.builder()
                                    .id(userId)
                                    .latitude(user.getLatitude())
                                    .longitude(user.getLongitude())
                                    .userEntity(user)
                                    .build()
                    );
                    user.setSiteAccount(userSiteAccount);
                    dataBaseService.saveUser(user);
                    cacheService.evictUserAvatars(userId);
                    cacheService.evictCachedUser(userId);
                }).start();
            }
        }
    }
    private class Menu implements State {

        HashMap<String, State> responses;

        public Menu() {
            responses = new HashMap<>();
            responses.put("1", new FindPeoples());
            responses.put("2", new EditProfile());
            responses.put("3", new OffProfile());
        }

        private class FindPeoples implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, "\uD83D\uDD0D✨", botFunctions.searchButtons());
                startSearch(userId, userEntity);
            }
        }

        private class EditProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_PROFILE(), botFunctions.myProfileButtons());
                cacheService.setState(userId, StateEnum.MY_PROFILE);
            }
        }

        private class OffProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_BEFORE_OFF(), botFunctions.askBeforeOffButtons());
                cacheService.setState(userId, StateEnum.ASK_BEFORE_OFF);
            }
        }
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = responses.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class SuperMenu implements State {
        public HashMap<String, State> superMenuStates;

        public SuperMenu () {
            superMenuStates = new HashMap<>();
            superMenuStates.put("1", new ShowWhoLikedMe());
            superMenuStates.put("2", new FindPeople());
            superMenuStates.put("3", new MyProfile());
            superMenuStates.put("4", new OffProfile());
        }

        private class ShowWhoLikedMe implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                likeChecker(userId, userEntity);
            }
        }

        private class FindPeople implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, "\uD83D\uDD0D✨", botFunctions.searchButtons());
                startSearch(userId, userEntity);
            }
        }

        private class MyProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_PROFILE(), botFunctions.myProfileButtons());
                cacheService.setState(userId, StateEnum.MY_PROFILE);
            }
        }

        private class OffProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_BEFORE_OFF(), botFunctions.askBeforeOffButtons());
                cacheService.setState(userId, StateEnum.ASK_BEFORE_OFF);
            }
        }
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = superMenuStates.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }
    private class AskBeforeOff implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Выключить анкету")) {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getLEFT());
                userEntity.setActive(false);
                dataBaseService.saveUser(userEntity);
                cacheService.evictState(userId);
            } else if (messageText.equals("Я передумала")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class MyProfile implements State {
        final HashMap<String, State> answers;
        public MyProfile() {
            answers = new HashMap<>();
            answers.put("БИО", new BIO());
            answers.put("Хобби, о себе", new Hobby());
            answers.put("Город", new City());
            answers.put("Фото", new Photo());
            answers.put("Изменить анкету полностью", new FullEdit());
            answers.put("Вернуться в меню", new Menu());
        }
        public class BIO implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_NAME(), botFunctions.skipButton());
                cacheService.setState(userId, StateEnum.EDIT_NAME);
                cacheService.putCachedUser(userId, userEntity);
            }
        }

        public class Hobby implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                if (userEntity.getHobby() == null) {
                    botFunctions.sendMessageAndKeyboard(userId, messages.getASK_HOBBY(), botFunctions.skipButton());
                } else {
                    botFunctions.sendMessageAndKeyboard(userId, messages.getASK_HOBBY(), botFunctions.removeAndCustomButtons(messages.getUNEDITED_HOBBY()));
                }
                cacheService.setState(userId, StateEnum.EDIT_HOBBY);
                cacheService.putCachedUser(userId, userEntity);
            }
        }

        public class City implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_CITY(), botFunctions.customLocationButtons(userEntity.getLocation()));
                cacheService.setState(userId, StateEnum.EDIT_CITY);
            }
        }

        public class Photo implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_PHOTO(), botFunctions.customButton(messages.getSKIP_ADD_AVATAR()));
                userEntity.setUserAvatars(new ArrayList<>());
                cacheService.putCachedUser(userId, userEntity);
                cacheService.setState(userId, StateEnum.ASK_AVATAR);
            }
        }

        public class FullEdit implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_NAME(), botFunctions.customButton(userEntity.getName()));
            }
        }

        public class Menu implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = answers.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class EditName implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (!messageText.equals("Пропустить")) {
                if (messageText.length() >= 100) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNAME_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                UserEntity user = cacheService.getCachedUser(userId);
                user.setName(messageText);
                cacheService.putCachedUser(userId, user);
            }
            botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            cacheService.setState(userId, StateEnum.EDIT_AGE);
        }
    }

    private class EditAge implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserEntity user = cacheService.getCachedUser(userId);
            if (!messageText.equals(String.valueOf(userEntity.getAge()))) {
                int age;
                try {
                    age = Integer.parseInt(message.getText());
                    if (age >= 100 || age <= 6) {
                        botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getAGE_LIMIT_SYMBOLS_EXCEPTIONS());
                        return;
                    }
                    user.setAge(age);
                } catch (Exception ex) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getIS_NOT_A_NUMBER_EXCEPTION());
                    return;
                }
            }
            if (!user.getName().equals(userEntity.getName()) || user.getAge() != userEntity.getAge()) {
                cacheService.putCachedUser(userId, user);
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, user);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                goToProfile(userId, userEntity);
            }
        }
    }
    private class EditCity implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (!messageText.equals(userEntity.getLocation())) {
                if (messageText.length() >= 100) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getCITY_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                userEntity.setLocation(messageText);
                Geocode geocode = jsonParser.parseGeocode(geocodingApi.getCoordinates(messageText));
                userEntity.setLongitude(geocode.getLon());
                userEntity.setLatitude(geocode.getLat());
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, userEntity);
                cacheService.putCachedUser(userId, userEntity);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                goToProfile(userId, userEntity);
            }
        }
    }
    private class EditCityGeo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            Location location = message.getLocation();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            userEntity.setLongitude(longitude);
            userEntity.setLatitude(latitude);
            userEntity.setLocation(jsonParser.getName(geocodingApi.getCityName(latitude, longitude)));
            userEntity.setShowGeo(true);

            botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
            botFunctions.sendDatingProfile(userId, userEntity);
            cacheService.putCachedUser(userId, userEntity);
            cacheService.setState(userId, StateEnum.EDIT_RESULT);
        }
    }

    private class EditHobby implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            if (!messageText.equals(messages.getUNEDITED_HOBBY()) && !messageText.equals("Пропустить")) {
                if (messageText.length() >= 150) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getHOBBY_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                if (messageText.equals("Убрать")) {
                    cachedUser.setHobby(null);
                } else {
                    cachedUser.setHobby(messageText);
                }
            }
            if (userEntity.getAboutMe() == null) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_ABOUT_ME(), botFunctions.skipButton());
            } else {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_ABOUT_ME(), botFunctions.removeAndCustomButtons(messages.getUNEDITED_ABOUT_ME()));
            }
            cacheService.putCachedUser(userId, cachedUser);
            cacheService.setState(userId, StateEnum.EDIT_ABOUT_ME);
        }
    }
    private class EditAboutMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            if (!messageText.equals(messages.getUNEDITED_ABOUT_ME()) && !messageText.equals("Пропустить")) {
                if (messageText.length() >= 1000) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getABOUT_ME_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                if (messageText.equals("Убрать")) {
                    cachedUser.setAboutMe(null);
                } else {
                    cachedUser.setAboutMe(messageText);
                }
            }
            if (!Objects.equals(cachedUser.getHobby(), userEntity.getHobby()) || !Objects.equals(cachedUser.getAboutMe(), userEntity.getAboutMe())) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, cachedUser);
                cacheService.putCachedUser(userId, cachedUser);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                goToProfile(userId, userEntity);
            }
        }
    }

    private class EditResult implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Сохранить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                botFunctions.sendDatingProfile(userId, user);
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_PROFILE(), botFunctions.myProfileButtons());
                cacheService.setState(userId, StateEnum.MY_PROFILE);
                cacheService.evictCachedUser(userId);
                dataBaseService.saveUser(user);
            }
            else if (messageText.equals("Отменить")) {
                goToProfile(userId, userEntity);
            }
        }
    }

    private class ShowWhoLikedMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Посмотреть")) {
                likeChecker(userId, userEntity);
            }
            else if (messageText.equals("В другой раз")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class SendLikeAndMessagePhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = botFunctions.loadPhoto(message.getPhoto());
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.PHOTO, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            startSearch(userId, userEntity);
        }
    }

    private class SendLikeAndMessageAudio implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = message.getVoice().getFileId();
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.VOICE, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            startSearch(userId, userEntity);
        }
    }

    private class SendLikeAndMessageVideo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = message.getVideo().getFileId();
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.VIDEO, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            startSearch(userId, userEntity);
        }
    }

    private class SendLikeAndMessageText implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            if (!messageText.equals("Отменить")) {
                botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
                cacheService.evictCachedProfiles(userId, anotherUser, profiles);
                startSearch(userId, userEntity);
                new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.TEXT, messageText)).start();
            } else {
                botFunctions.sendMessageAndKeyboard(userId, "Отмена отправки", botFunctions.searchButtons());
                botFunctions.sendOtherProfile(userId, anotherUser, userEntity);
            }
            cacheService.setState(userId, StateEnum.FIND_PEOPLES);
        }
    }

    private class SendLikeAndMessageVideoNote implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = message.getVideoNote().getFileId();
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.VIDEO_NOTE, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            startSearch(userId, userEntity);
        }
    }

    private class ShowProfilesWhoLikedMe implements State {
        HashMap<String, State> response;
        public ShowProfilesWhoLikedMe () {
            response = new HashMap<>();
            response.put("❤", new SendReciprocity());
            response.put("\uD83D\uDC4E", new SendDislike());
            response.put("\uD83D\uDCA4", new GoToMenu());
        }

        private class SendReciprocity implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                UserEntity likedUser = removeLike(userEntity);
                String userName = botFunctions.getChatMember(likedUser.getId()).getUser().getUserName();
                botFunctions.sendMessageAndComplainButton(userId, likedUser.getId(), "Желаю вам хорошо провести время :)\nhttps://t.me/" + userName);
                likeChecker(userId, userEntity);
                new Thread(() -> sendLike(userEntity, likedUser, true, null, null)).start();
            }
        }

        private class SendDislike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                removeLike(userEntity);
                likeChecker(userId, userEntity);
            }
        }

        private class GoToMenu implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }

        private UserEntity removeLike (UserEntity userEntity) {
            List<LikeEntity> likeEntityList = userEntity.getLikesGiven();
            LikeEntity like = likeEntityList.getFirst();
            UserEntity likedUser = dataBaseService.getUserById(like.getLikerUserId()).get();
            likeEntityList.remove(like);
            dataBaseService.saveUser(userEntity);
            dataBaseService.deleteLike(like.getId());
            return likedUser;
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            State state = response.get(message.getText());
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class StopShowProfilesWhoLikedMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Продолжить смотреть анкеты")) {
                botFunctions.sendMessageAndKeyboard(userId, "\uD83D\uDD0D✨", botFunctions.searchButtons());
                startSearch(userId, userEntity);
            }
            else if (messageText.equals("Вернуться в меню")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class FindPeoples implements State {

        HashMap<String, State> response;

        public FindPeoples() {
            response = new HashMap<>();
            response.put("❤", new SendLike());
            response.put("\uD83D\uDC8C", new SendLikeAndMessage());
            response.put("\uD83D\uDC4E", new SendDislike());
            response.put("\uD83D\uDCA4", new GoToMenu());
        }

        private class SendLike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                List<UserEntity> profiles = getRecommendation(userEntity, userId);
                UserEntity anotherUser = profiles.getFirst();

                botFunctions.sendMessageNotRemoveKeyboard(userId, "Лайк отправлен, ждём ответа");
                new Thread(() -> sendLike(userEntity, anotherUser, false, null, null)).start();
                cacheService.evictCachedProfiles(userId, anotherUser, profiles);

                startSearch(userId, userEntity);
            }
        }
        private class SendLikeAndMessage implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, "Можешь отправить сообщение, кружок, голосовое, фото или видео. Я пришлю его этому человеку", botFunctions.cancelButton());
                cacheService.setState(userId, StateEnum.SEND_LIKE_AND_MESSAGE);
            }
        }

        private class SendDislike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                List<UserEntity> profiles = getRecommendation(userEntity, userId);
                UserEntity anotherUser = profiles.getFirst();

                cacheService.evictCachedProfiles(userId, anotherUser, profiles);

                startSearch(userId, userEntity);
            }
        }

        private class GoToMenu implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = response.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class SendError implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageNotRemoveKeyboard(userId, "Благодарим за сотрудничество! Мы обязательно рассмотрим вашу проблему");
            goToMenu(userId, userEntity);
            new Thread(() -> {
                ErrorEntity errorEntity = ErrorEntity.builder()
                        .errorSenderId(userId)
                        .description(message.getText())
                        .build();
                dataBaseService.saveError(errorEntity);
            }).start();
        }
    }

    private class FaqResponse implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Назад")) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getFAQ(), botFunctions.faqButtons());
                cacheService.setState(userId, StateEnum.FAQ);
            } else if (messageText.equals("Вернуться в меню")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class FAQ implements State {

        private final HashMap<String, State> response;

        public FAQ () {
            response = new HashMap<>();
            response.put("1", new FAQ1());
            response.put("2", new FAQ2());
            response.put("Вернуться в меню", new GoToMenu());
        }

        private class FAQ1 implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getFAQ_1(), botFunctions.faqResponseButtons());
                cacheService.setState(userId, StateEnum.FAQ_RESPONSE);
            }
        }

        private class FAQ2 implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getFAQ_2(), botFunctions.faqResponseButtons());
                cacheService.setState(userId, StateEnum.FAQ_RESPONSE);
            }
        }

        private class GoToMenu implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            State state = response.get(message.getText());
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class CallbackQueryComplain implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            Long complaintUserId = cacheService.getComplaintUserId(userId);

            if (!messageText.equals("Отменить")) {
                UserEntity complaintUser = dataBaseService.getUserById(complaintUserId).get();
                ComplainEntity complainEntity = ComplainEntity.builder()
                        .complainSenderId(userId)
                        .description(message.getText())
                        .complaintUser(complaintUser)
                        .build();
                dataBaseService.saveComplain(complainEntity);
                botFunctions.sendMessageAndRemoveKeyboard(userId, "Жалоба отправлена, мы её внимательно изучим");
            }
            goToMenu(userId, userEntity);
            cacheService.evictComplaintUser(userId);
        }
    }

    public void likeChecker (Long userId, UserEntity myProfile) {
        List<LikeEntity> likeList = myProfile.getLikesGiven();
        if (likeList.isEmpty()) {
            botFunctions.sendMessageAndKeyboard(userId, "На этом всё, продолжить просмотр анкет?", botFunctions.stopShowProfilesWhoLikedMeButtons());
            cacheService.setState(userId, StateEnum.STOP_SHOW_PROFILES_WHO_LIKED_ME);
        } else {
            LikeEntity like = likeList.getFirst();
            if (like.isReciprocity()) {
                UserEntity likedUser = dataBaseService.getUserById(like.getLikerUserId()).get();
                botFunctions.sendMessageAndRemoveKeyboard(userId, "Есть взаимная симпатия!");
                botFunctions.sendOtherProfile(userId, likedUser, myProfile);
                String userName = botFunctions.getChatMember(likedUser.getId()).getUser().getUserName();
                botFunctions.sendMessageAndComplainButton(userId, likedUser.getId(), "Желаю вам хорошо провести время :)\nhttps://t.me/" + userName);
                likeList.remove(like);
                dataBaseService.saveUser(myProfile);
                dataBaseService.deleteLike(like.getId());
                likeChecker(userId, myProfile);
            } else {
                showWhoLikedMe(userId, myProfile, like);
            }
        }
    }

    public void startSearch (Long userId, UserEntity userEntity) {
        UserEntity anotherUser = getRecommendation(userEntity, userId).getFirst();
        botFunctions.sendOtherProfile(userId, anotherUser, userEntity);
        cacheService.setState(userId, StateEnum.FIND_PEOPLES);
    }

    public void goToMenu (Long userId, UserEntity userEntity) {
        int likedMeCount = userEntity.getLikesGiven().size();
        if (likedMeCount == 0) {
            botFunctions.sendMessageAndKeyboard(userId, messages.getMENU(), botFunctions.menuButtons());
            cacheService.setState(userId, StateEnum.MENU);
        } else {
            String likeCountText;
            if (likedMeCount == 1) {
                likeCountText = "1. Посмотреть, кому я понравилась\n";
            } else {
                likeCountText = "1. Твоя анкета понравилась " + likedMeCount + " людям, показать их?\n";
            }
            botFunctions.sendMessageAndKeyboard(userId,
                    likeCountText +
                            "2. Начать поиск подруг ✨\n" +
                            "3. Моя анкета\n" +
                            "4. Выключить анкету",
                    botFunctions.superMenuButtons());
            cacheService.setState(userId, StateEnum.SUPER_MENU);
        }
    }

    private void goToProfile(Long userId, UserEntity userEntity) {
        botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getNULL_DATA_EDIT());
        botFunctions.sendDatingProfile(userId, userEntity);
        botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_PROFILE(), botFunctions.myProfileButtons());
        cacheService.setState(userId, StateEnum.MY_PROFILE);
        cacheService.evictCachedUser(userId);
        cacheService.evictUserAvatars(userId);
    }

    public List<UserEntity> getRecommendation(UserEntity myProfile, Long userId) {
        List<UserEntity> profiles = cacheService.getCachedProfiles(userId);
        if (profiles == null || profiles.isEmpty()) {
            profiles = dataBaseService.getProfiles(myProfile);
            cacheService.putCachedProfiles(userId, profiles);
        }
        return profiles;
    }

    public void showWhoLikedMe (Long userId, UserEntity myProfile, LikeEntity like) {
        botFunctions.sendMessageAndKeyboard(userId,"Твоя анкета кому-то понравилась!", botFunctions.reciprocityButtons());
        UserEntity anotherUser = dataBaseService.getUserById(like.getLikerUserId()).get();
        botFunctions.sendOtherProfile(userId, anotherUser, myProfile);
        if (like.getLikeContentType() != null) {
            botFunctions.sendMessage.get(like.getLikeContentType()).handleInput(userId, like);
        }
        cacheService.setState(userId, StateEnum.SHOW_PROFILES_WHO_LIKED_ME);
    }

    public void sendLike(UserEntity myProfile, UserEntity anotherUser, boolean isReciprocity, LikeContentType likeContentType, String content) {
        if (!myProfile.isBanned()) {
            Long anotherUserId = anotherUser.getId();
            UserEntity realAnotherUser = dataBaseService.getUserById(anotherUserId).get();
            List<LikeEntity> likedUsers = realAnotherUser.getLikesGiven();
            if (likedUsers.stream().noneMatch(like -> like.getLikerUserId() == myProfile.getId())) {
                Cache.ValueWrapper optionalState = cacheService.getCurrentState(anotherUserId);
                if (optionalState == null || optionalState.get() == StateEnum.MENU) {
                    if (likedUsers.isEmpty()) {
                        botFunctions.sendMessageAndKeyboard(anotherUserId, "твоя анкета кому-то понравилась", botFunctions.showWhoLikedMeButtons());
                    } else {
                        botFunctions.sendMessageAndKeyboard(anotherUserId, "твоя анкета понравилась " + (likedUsers.size() + 1) + " людям", botFunctions.showWhoLikedMeButtons());
                    }
                    cacheService.setState(anotherUserId, StateEnum.SHOW_WHO_LIKED_ME);
                } else if (optionalState.get() == StateEnum.FIND_PEOPLES) {
                    botFunctions.sendMessageNotRemoveKeyboard(anotherUserId, "Заканчивай с просмотром анкет, ты кому-то понравилась!");
                }
                LikeEntity like = dataBaseService.saveLike(
                        LikeEntity.builder()
                                .isReciprocity(isReciprocity)
                                .likeContentType(likeContentType)
                                .content(content)
                                .likedUser(realAnotherUser)
                                .likerUserId(myProfile.getId())
                                .build()
                );
                realAnotherUser.getLikesGiven().add(like);
                dataBaseService.saveUser(realAnotherUser);
            }
        }
    }
}
