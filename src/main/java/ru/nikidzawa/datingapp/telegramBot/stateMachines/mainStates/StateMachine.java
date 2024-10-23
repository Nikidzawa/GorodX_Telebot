package ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.api.external.ExternalApi;
import ru.nikidzawa.datingapp.store.entities.complaint.ComplaintEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.like.LikeContentType;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.*;
import ru.nikidzawa.datingapp.store.repositories.LikeRepository;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.messages.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.Geocode;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.JsonParser;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.commands.RoleState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private ExternalApi externalApi;

    @Setter
    @Getter
    private BotFunctions botFunctions;

    private final HashMap<StateEnum, State> textStates;
    private final HashMap<StateEnum, State> photoStates;
    private final HashMap<StateEnum, State> locationStates;
    private final HashMap<StateEnum, State> audioStates;
    private final HashMap<StateEnum, State> videoStates;
    private final HashMap<StateEnum, State> videoNoteStates;
    @Autowired
    private LikeRepository likeRepository;

    public StateMachine() {
        textStates = new HashMap<>();
        photoStates = new HashMap<>();
        locationStates = new HashMap<>();
        audioStates = new HashMap<>();
        videoStates = new HashMap<>();
        videoNoteStates = new HashMap<>();

        textStates.put(StateEnum.START, new Start());
        textStates.put(StateEnum.START_HANDLE, new StartHandle());

        textStates.put(StateEnum.ASK_BEFORE_OFF, new AskBeforeOff());
        textStates.put(StateEnum.LEFT, new Left());

        textStates.put(StateEnum.ASK_NAME, new AskName());
        textStates.put(StateEnum.ASK_AGE, new AskAge());
        textStates.put(StateEnum.ASK_GENDER, new AskGender());
        textStates.put(StateEnum.ASK_GENDER_SEARCH, new AskGenderSearch());
        textStates.put(StateEnum.ASK_CITY, new AskCity());
        textStates.put(StateEnum.ASK_ABOUT_ME, new AskAboutMe());
        textStates.put(StateEnum.ASK_AVATAR, new FinishRegisterAddAvatar());
        textStates.put(StateEnum.RESULT, new Result());
        textStates.put(StateEnum.ADD_ADMINISTRATOR_ROLE, new AddAdministratorRole());
        textStates.put(StateEnum.REMOVE_ROLE, new RemoveRole());

        textStates.put(StateEnum.MENU, new Menu());
        textStates.put(StateEnum.SUPER_MENU, new SuperMenu());

        textStates.put(StateEnum.MY_PROFILE, new MyProfile());
        textStates.put(StateEnum.EDIT_NAME, new EditName());
        textStates.put(StateEnum.EDIT_AGE, new EditAge());
        textStates.put(StateEnum.EDIT_CITY, new EditCity());
        textStates.put(StateEnum.EDIT_ABOUT_ME, new EditAboutMe());
        textStates.put(StateEnum.EDIT_AVATAR, new FinishEditAvatar());
        textStates.put(StateEnum.EDIT_RESULT, new EditResult());

        textStates.put(StateEnum.FIND_PEOPLES, new FindPeoples());
        textStates.put(StateEnum.SHOW_WHO_LIKED_ME, new ShowWhoLikedMe());
        textStates.put(StateEnum.SHOW_PROFILES_WHO_LIKED_ME, new ShowProfilesWhoLikedMe());
        textStates.put(StateEnum.STOP_SHOW_PROFILES_WHO_LIKED_ME, new StopShowProfilesWhoLikedMe());
        textStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageText());
        textStates.put(StateEnum.CALL_BACK_QUERY_COMPLAIN, new CallbackQueryComplain());
        textStates.put(StateEnum.FAQ, new FAQ());
        textStates.put(StateEnum.FAQ_RESPONSE, new FaqResponse());
        textStates.put(StateEnum.SEND_ERROR, new SendError());
        textStates.put(StateEnum.FIND_PEOPLES_COMPLAIN, new FindPeopleComplain());
        textStates.put(StateEnum.ERROR, new Error());
        textStates.put(StateEnum.ROLES_CONTROLLER, new RolesController());
        textStates.put(StateEnum.ADD_SUPER_ADMINISTRATOR_ROLE, new AddSuperAdministratorRole());

        photoStates.put(StateEnum.ASK_AVATAR, new AddAvatarPhoto());
        photoStates.put(StateEnum.EDIT_AVATAR, new AddAvatarPhoto());
        photoStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessagePhoto());

        locationStates.put(StateEnum.ASK_CITY, new AskCityGeo());
        locationStates.put(StateEnum.EDIT_CITY, new EditCityGeo());

        audioStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageVoice());
        videoStates.put(StateEnum.ASK_AVATAR, new AddAvatarVideo());
        videoStates.put(StateEnum.EDIT_AVATAR, new AddAvatarVideo());
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

    public void likeChecker(Long userId, UserEntity myProfile) {
        List<LikeEntity> likeList = dataBaseService.getAllLikeEntityByUserId(userId);
        if (likeList.isEmpty()) {
            botFunctions.sendMessageAndKeyboard(userId, "На этом всё, продолжить просмотр анкет?", botFunctions.stopShowProfilesWhoLikedMeButtons());
            cacheService.setState(userId, StateEnum.STOP_SHOW_PROFILES_WHO_LIKED_ME);
        } else {
            LikeEntity like = likeList.getFirst();
            if (like.isReciprocity()) {
                UserEntity likedUser = dataBaseService.getUserById(like.getLikeSender()).get();
                botFunctions.sendMessageAndRemoveKeyboard(userId, "Есть взаимная симпатия!");
                botFunctions.sendDatingProfile(userId, likedUser, myProfile);
                String userName = botFunctions.getUsernameByUserId(likedUser.getId());
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

    private class StartHandle implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Создать анкету")) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_NAME(), botFunctions.customButton(message.getFrom().getFirstName()));
            }
        }
    }

    public void goToMenu(Long userId, UserEntity userEntity) {
        long likedMeCount = dataBaseService.getAllPeopleCountWhoLikeUserEntity(userId);
        if (likedMeCount == 0) {
            botFunctions.sendMessageAndKeyboard(userId,
                    """
                            1. Начать поиск новых знакомств ✨
                            2. Моя анкета
                            3. Выключить анкету
                            """
                    , botFunctions.menuButtons(userEntity.getGender().getSmile()));
            cacheService.setState(userId, StateEnum.MENU);
        } else {
            String likeCountText;
            if (likedMeCount == 1) {
                likeCountText = "1. Твоя анкета понравилась 1 " + userEntity.getGenderSearch().getSinglePrefix() + ", показать " + (userEntity.getGenderSearch() == GenderSearchEnum.FEMALE ? "её" : "его") + "?\n";
            } else {
                likeCountText = "1. Твоя анкета понравилась " + likedMeCount + " " + userEntity.getGenderSearch().getMultiPrefix() + ", показать их?\n";
            }
            botFunctions.sendMessageAndKeyboard(userId,
                    likeCountText +
                            """
                            2. Начать поиск новых знакомств ✨
                            3. Моя анкета
                            4. Выключить анкету
                            """,
                    botFunctions.superMenuButtons(userEntity.getGender().getSmile()));
            cacheService.setState(userId, StateEnum.SUPER_MENU);
        }
    }

    public void showNextUser(Long userId, UserEntity userEntity) {
        List<Long> excludedUserIds = cacheService.getExcludedUserIds(userId);
        List<UserEntity> recommendations = dataBaseService.getRecommendation(userEntity);
        if (!recommendations.isEmpty()) {
            List<UserEntity> filteredList = recommendations.stream()
                    .filter(user -> !excludedUserIds.contains(user.getId()))
                    .toList();
            if (!filteredList.isEmpty()) {
                UserEntity recommendationUser = filteredList.getFirst();
                botFunctions.sendDatingProfile(userId, recommendationUser, userEntity);
                cacheService.putUserAssessmentId(userId, recommendationUser.getId());
                cacheService.setState(userId, StateEnum.FIND_PEOPLES);
            } else {
                cacheService.evictExcludedUserIds(userId);
                showNextUser(userId, userEntity);
            }
        } else {
            botFunctions.sendMessageNotRemoveKeyboard(userId, "К сожалению, поблизости никого не нашли. Попробуйте изменить населённый пункт");
            goToMenu(userId, userEntity);
        }
    }

    private void startSearch(Long userId, UserEntity userEntity) {
        botFunctions.sendMessageAndKeyboard(userId, "\uD83D\uDD0D✨", botFunctions.searchButtons());
        showNextUser(userId, userEntity);
    }

    private class Left implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(
                    userId,
                    userEntity.getGender() == GenderEnum.MALE ? messages.getLEFT_MAN() : messages.getLEFT_WOMEN(),
                    botFunctions.restartButton()
            );
            dataBaseService.getUserById(userId).ifPresent(user -> {
                user.setActive(false);
                dataBaseService.saveUser(user);
            });
            cacheService.evictAllUserCache(userId);
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
            } else {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_CITY(), botFunctions.locationButton());
            }

            cacheService.setState(userId, StateEnum.ASK_CITY);
            UserEntity user = cacheService.getCachedUser(userId);
            user.setName(message.getText());
            cacheService.putCachedUser(userId, user);
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

            UserEntity user = cacheService.getCachedUser(userId);
            if (hasBeenRegistered && messageText.equals(userEntity.getLocation())) {
                user.setLocation(userEntity.getLocation());
                user.setLongitude(userEntity.getLongitude());
                user.setLatitude(userEntity.getLatitude());
                user.setShowGeo(userEntity.isShowGeo());
            } else {
                try {
                    user.setLocation(messageText);
                    Geocode coordinates = jsonParser.parseGeocode(externalApi.getCoordinates(messageText));
                    user.setLongitude(coordinates.getLon());
                    user.setLatitude(coordinates.getLat());
                    user.setShowGeo(false);
                } catch (Exception ex) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, "Сервисы геокодирования пока не доступны. Пожалуйста, повторите попытку позже");
                    return;
                }
            }
            cacheService.putCachedUser(userId, user);

            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            } else {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_AGE());
            }
            cacheService.setState(userId, StateEnum.ASK_AGE);
        }
    }

    private class AskCityGeo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            Location location = message.getLocation();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            try {
                String city = jsonParser.getName(externalApi.getCityName(latitude, longitude));
                cachedUser.setLocation(city);
                cachedUser.setShowGeo(true);

                cachedUser.setLongitude(longitude);
                cachedUser.setLatitude(latitude);
            } catch (Exception exception) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Сервисы геокодирования пока не доступны. Пожалуйста, повторите попытку позже");
                return;
            }

            cacheService.putCachedUser(userId, cachedUser);
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            } else {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_AGE());
            }
            cacheService.setState(userId, StateEnum.ASK_AGE);
        }
    }

    private class AskAge implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
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
            UserEntity user = cacheService.getCachedUser(userId);
            user.setAge(age);
            cacheService.putCachedUser(userId, user);

            botFunctions.sendMessageAndKeyboard(userId, messages.getASK_GENDER(),
                    botFunctions.customButton(
                            GenderEnum.MALE.getSmileAndName(),
                            GenderEnum.FEMALE.getSmileAndName()
                    )
            );

            cacheService.setState(userId, StateEnum.ASK_GENDER);
        }
    }

    private class AskGender implements State {

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String userResponse = message.getText();
            GenderEnum selectedGender = null;
            if (userResponse.equals(GenderEnum.MALE.getSmileAndName())) {
                selectedGender = GenderEnum.MALE;
            } else if (userResponse.equals(GenderEnum.FEMALE.getSmileAndName())) {
                selectedGender = GenderEnum.FEMALE;
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getINVALID_VALUE());
                return;
            }

            UserEntity user = cacheService.getCachedUser(userId);
            user.setGender(selectedGender);

            cacheService.putCachedUser(userId, user);

            botFunctions.sendMessageAndKeyboard(userId, messages.getASK_GENDER_SEARCH(),
                    botFunctions.customButton(
                            GenderSearchEnum.MALE.getSmileAndName(),
                            GenderSearchEnum.FEMALE.getSmileAndName(),
                            GenderSearchEnum.NO_DIFFERENCE.getSmileAndName()
                    )
            );
            cacheService.setState(userId, StateEnum.ASK_GENDER_SEARCH);
        }
    }

    private class AskGenderSearch implements State {

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String userResponse = message.getText();
            GenderSearchEnum selectedGenderSearch = null;

            if (userResponse.equals(GenderSearchEnum.MALE.getSmileAndName())) {
                selectedGenderSearch = GenderSearchEnum.MALE;
            } else if (userResponse.equals(GenderSearchEnum.FEMALE.getSmileAndName())) {
                selectedGenderSearch = GenderSearchEnum.FEMALE;
            } else if (userResponse.equals(GenderSearchEnum.NO_DIFFERENCE.getSmileAndName())) {
                selectedGenderSearch = GenderSearchEnum.NO_DIFFERENCE;
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getINVALID_VALUE());
                return;
            }

            UserEntity user = cacheService.getCachedUser(userId);
            user.setGenderSearch(selectedGenderSearch);

            cacheService.putCachedUser(userId, user);

            if (hasBeenRegistered && userEntity.getAboutMe() != null) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_ABOUT_ME(), botFunctions.skipAndCustomButtons(messages.getSKIP_EDIT()));
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
                if (messageText.equals("Не изменять") && hasBeenRegistered) {
                    user.setAboutMe(userEntity.getAboutMe());
                } else {
                    user.setAboutMe(message.getText());
                }
                cacheService.putCachedUser(userId, user);
            }
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getASK_AVATAR(), botFunctions.customButton(messages.getSKIP_EDIT()));
            } else {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_AVATAR());
            }
            cacheService.setState(userId, StateEnum.ASK_AVATAR);
        }
    }

    private class AddAvatarPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            List<UserAvatar> avatars = cachedUser.getUserAvatars();
            if (avatars == null) {
                avatars = new ArrayList<>();
                cachedUser.setUserAvatars(avatars);
            }
            avatars.add(UserAvatar.builder()
                    .file(botFunctions.loadPhoto(message.getPhoto()))
                    .isPhoto(true)
                    .build());
            int avatarSize = avatars.size();
            cacheService.putCachedUser(userId, cachedUser);
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, "Фотография загружена ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSKIP_EDIT(), messages.getSTOP_ADD_AVATAR()));
            } else {
                botFunctions.sendMessageAndKeyboard(userId, "Фотография загружена ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSTOP_ADD_AVATAR()));
            }
            if (avatarSize == 3) {
                if (hasBeenRegistered) {
                    goToEditResult(userId, cachedUser);
                } else {
                    botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getRESULT());
                    botFunctions.sendDatingProfile(userId, cachedUser);
                    botFunctions.sendMessageAndKeyboard(userId, "Всё верно?", botFunctions.resultButtons());
                    cacheService.setState(userId, StateEnum.RESULT);
                }
            } else {
                if (hasBeenRegistered) {cacheService.setState(userId, StateEnum.EDIT_AVATAR);
                } else cacheService.setState(userId, StateEnum.ASK_AVATAR);
            }
        }
    }

    private class AddAvatarVideo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            List<UserAvatar> avatars = cachedUser.getUserAvatars();
            if (avatars == null) {
                avatars = new ArrayList<>();
                cachedUser.setUserAvatars(avatars);
            }
            avatars.add(UserAvatar.builder()
                    .file(message.getVideo().getFileId())
                    .isPhoto(false)
                    .build());
            int avatarSize = avatars.size();
            cacheService.putCachedUser(userId, cachedUser);
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, "Видео загружено ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSKIP_EDIT(), messages.getSTOP_ADD_AVATAR()));
            } else {
                botFunctions.sendMessageAndKeyboard(userId, "Видео загружено ( " + avatarSize + " из 3 )", botFunctions.customButton(messages.getSTOP_ADD_AVATAR()));
            }
            if (avatarSize == 3) {
                if (hasBeenRegistered) {
                    goToEditResult(userId, cachedUser);
                } else {
                    botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getRESULT());
                    botFunctions.sendDatingProfile(userId, cachedUser);
                    botFunctions.sendMessageAndKeyboard(userId, "Всё верно?", botFunctions.resultButtons());
                    cacheService.setState(userId, StateEnum.RESULT);
                }
            } else {
                if (hasBeenRegistered) {cacheService.setState(userId, StateEnum.EDIT_AVATAR);
                } else cacheService.setState(userId, StateEnum.ASK_AVATAR);
            }
        }
    }

    private class FinishRegisterAddAvatar implements State {

        private final HashMap<String, State> states;

        public FinishRegisterAddAvatar() {
            states = new HashMap<>();
            states.put("Не изменять", new SkipAddAvatar());
            states.put("Готово", new StopAddAvatar());
        }

        private class SkipAddAvatar implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                if (hasBeenRegistered) {
                    UserEntity cachedUser = cacheService.getCachedUser(userId);
                    cachedUser.setUserAvatars(userEntity.getUserAvatars());
                    goToEditResult(userId, cachedUser);
                } else {
                    UserEntity cachedUser = cacheService.getCachedUser(userId);
                    cachedUser.setUserAvatars(userEntity.getUserAvatars());
                    cacheService.putCachedUser(userId, cachedUser);
                    botFunctions.sendMessageAndKeyboard(userId, messages.getRESULT(), botFunctions.resultButtons());
                    botFunctions.sendDatingProfile(userId, cachedUser);
                    cacheService.setState(userId, StateEnum.RESULT);
                }
            }
        }
        private class StopAddAvatar implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                UserEntity cachedUser = cacheService.getCachedUser(userId);
                List<UserAvatar> userAvatars = cachedUser.getUserAvatars();
                if (!userAvatars.isEmpty()) {
                    if (hasBeenRegistered) {
                        goToEditResult(userId, cachedUser);
                    } else {
                        botFunctions.sendMessageAndKeyboard(userId, messages.getRESULT(), botFunctions.resultButtons());
                        botFunctions.sendDatingProfile(userId, cachedUser);
                        cacheService.setState(userId, StateEnum.RESULT);
                    }
                } else {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, "Необходимо добавить хотя бы одну фотографию");
                }
            }
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            State state = states.get(message.getText());
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getINVALID_FORMAT_EXCEPTION());
            }
        }
    }

    public void showWhoLikedMe(Long userId, UserEntity myProfile, LikeEntity like) {
        UserEntity likeSender = dataBaseService.getUserById(like.getLikeSender()).get();
        if (likeSender.isActive() && !likeSender.isBanned()) {
            botFunctions.sendMessageAndKeyboard(userId, "Твоя анкета кому-то понравилась!", botFunctions.reciprocityButtons());
            botFunctions.sendDatingProfile(userId, likeSender, myProfile);
            if (like.getLikeContentType() != null) {
                botFunctions.sendMessage.get(like.getLikeContentType()).handleInput(userId, like);
            }
            cacheService.setState(userId, StateEnum.SHOW_PROFILES_WHO_LIKED_ME);
        } else {
            dataBaseService.deleteLike(like.getId());
            botFunctions.sendMessageNotRemoveKeyboard(userId, "К сожалению, уже не актуально :(");
            likeChecker(userId, myProfile);
        }
    }
    private class Menu implements State {

        HashMap<String, State> responses;

        public Menu() {
            responses = new HashMap<>();
            responses.put("1 \uD83D\uDE80", new FindPeoples());
            responses.put("2 \uD83D\uDC68", new MyProfile());
            responses.put("2 \uD83D\uDC69", new MyProfile());
            responses.put("3 \uD83D\uDCA4", new OffProfile());
        }

        private class FindPeoples implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                startSearch(userId, userEntity);
            }
        }

        private class MyProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToProfile(userId, userEntity);
            }
        }

        private class OffProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(
                        userId,
                        userEntity.getGender() == GenderEnum.MALE ? messages.getASK_BEFORE_OFF_MAN() : messages.getASK_BEFORE_OFF_WOMEN(),
                        botFunctions.askBeforeOffButtons()
                );
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
            superMenuStates.put("1 ❤", new ShowWhoLikedMe());
            superMenuStates.put("2 \uD83D\uDE80", new FindPeoples());
            superMenuStates.put("3 \uD83D\uDC68", new MyProfile());
            superMenuStates.put("3 \uD83D\uDC69", new MyProfile());
            superMenuStates.put("4 \uD83D\uDCA4", new OffProfile());
        }

        private class ShowWhoLikedMe implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                likeChecker(userId, userEntity);
            }
        }

        private class FindPeoples implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                startSearch(userId, userEntity);
            }
        }

        private class MyProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToProfile(userId, userEntity);
            }
        }

        private class OffProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(
                        userId,
                        userEntity.getGender() == GenderEnum.MALE ? messages.getASK_BEFORE_OFF_MAN() : messages.getASK_BEFORE_OFF_WOMEN(),
                        botFunctions.askBeforeOffButtons());
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

    public void sendLike(UserEntity likeSender, Long likeReceiverId, boolean isReciprocity, LikeContentType likeContentType, String content) {
        if (!likeSender.isBanned()) {
            UserEntity likeReceiver = dataBaseService.getUserById(likeReceiverId).get();
            if (likeReceiver.isActive() && !likeReceiver.isBanned()) {
                List<LikeEntity> ReceiverLikeEntity = dataBaseService.getAllLikeEntityByUserId(likeReceiver.getId());
                if (ReceiverLikeEntity.stream().noneMatch(like -> like.getLikeSender() == likeSender.getId())) {
                    Cache.ValueWrapper optionalState = cacheService.getCurrentState(likeReceiverId);

                    if (optionalState.get() == StateEnum.MENU || optionalState.get() == StateEnum.SUPER_MENU || optionalState.get() == StateEnum.MY_PROFILE) {
                        if (ReceiverLikeEntity.isEmpty()) {
                            botFunctions.sendMessageAndKeyboard(likeReceiverId, "твоя анкета кому-то понравилась", botFunctions.showWhoLikedMeButtons());
                        } else {
                            botFunctions.sendMessageAndKeyboard(likeReceiverId, "твоя анкета понравилась " + (ReceiverLikeEntity.size() + 1) + " " + likeReceiver.getGenderSearch().getMultiPrefix(), botFunctions.showWhoLikedMeButtons());
                        }
                        cacheService.setState(likeReceiverId, StateEnum.SHOW_WHO_LIKED_ME);
                    } else if (optionalState.get() == StateEnum.FIND_PEOPLES) {
                        botFunctions.sendMessageNotRemoveKeyboard(likeReceiverId, "Заканчивай с просмотром анкет, твоя анкета кому-то понравилась!");
                    }
                    dataBaseService.saveLike(
                            LikeEntity.builder()
                                    .isReciprocity(isReciprocity)
                                    .likeContentType(likeContentType)
                                    .content(content)
                                    .likeReceiver(likeReceiver.getId())
                                    .likeSender(likeSender.getId())
                                    .build()
                    );
                }
            }
        }
    }

    private void goToEditResult(Long userId, UserEntity cachedUser) {
        botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
        botFunctions.sendDatingProfile(userId, cachedUser);
        cacheService.putCachedUser(userId, cachedUser);
        cacheService.setState(userId, StateEnum.EDIT_RESULT);
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
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            if (!messageText.equals(String.valueOf(userEntity.getAge()))) {
                int age;
                try {
                    age = Integer.parseInt(message.getText());
                    if (age >= 100 || age <= 6) {
                        botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getAGE_LIMIT_SYMBOLS_EXCEPTIONS());
                        return;
                    }
                    cachedUser.setAge(age);
                } catch (Exception ex) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getIS_NOT_A_NUMBER_EXCEPTION());
                    return;
                }
            }
            if (cachedUser.getName().equals(userEntity.getName()) && cachedUser.getAge() == userEntity.getAge()) {
                returnProfileWithoutChanges(userId, userEntity);
            } else {
                goToEditResult(userId, cachedUser);
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
                Geocode geocode = jsonParser.parseGeocode(externalApi.getCoordinates(messageText));
                userEntity.setLongitude(geocode.getLon());
                userEntity.setLatitude(geocode.getLat());
                dataBaseService.saveUser(userEntity);
            }
            goToProfile(userId, userEntity);
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
            String city = jsonParser.getName(externalApi.getCityName(latitude, longitude));
            userEntity.setLocation(city);
            userEntity.setShowGeo(true);

            dataBaseService.saveUser(userEntity);
            goToProfile(userId, userEntity);
        }
    }

    private class EditAboutMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (!messageText.equals(messages.getSKIP_EDIT())) {
                if (messageText.length() >= 1000) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getABOUT_ME_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                if (messageText.equals("Убрать")) {
                    userEntity.setAboutMe(null);
                } else {
                    userEntity.setAboutMe(messageText);
                }
                dataBaseService.saveUser(userEntity);
            }
            goToProfile(userId, userEntity);
        }
    }

    private class FinishEditAvatar implements State {

        private final HashMap<String, State> states;

        public FinishEditAvatar() {
            states = new HashMap<>();
            states.put("Не изменять", new SkipAddAvatar());
            states.put("Готово", new StopAddAvatar());
        }

        private class SkipAddAvatar implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToProfile(userId, userEntity);
            }
        }

        private class StopAddAvatar implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                UserEntity cachedUser = cacheService.getCachedUser(userId);
                List<UserAvatar> userAvatars = cachedUser.getUserAvatars();
                if (!userAvatars.isEmpty()) {
                    userEntity.setUserAvatars(userAvatars);
                    dataBaseService.saveUser(userEntity);
                    goToProfile(userId, userEntity);
                    cacheService.evictCachedUser(userId);
                } else {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, "Необходимо добавить хотя бы одну фотографию");
                }
            }
        }
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            State state = states.get(message.getText());
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class EditResult implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Сохранить")) {
                UserEntity cachedUser = cacheService.getCachedUser(userId);
                cachedUser.setActive(true);
                goToProfile(userId, cachedUser);
                new Thread(() -> {
                    dataBaseService.saveAllUserAvatars(cachedUser.getUserAvatars());
                    dataBaseService.saveUser(cachedUser);
                }).start();
            }
            else if (messageText.equals("Отменить")) {
                goToProfile(userId, userEntity);
            }
        }
    }

    private void goToProfile(Long userId, UserEntity userEntity) {
        botFunctions.sendMessageNotRemoveKeyboard(userId, "⭐ Так выглядит твоя анкета");
        botFunctions.sendDatingProfile(userId, userEntity);
        botFunctions.sendMessageAndKeyboard(userId, messages.getMY_PROFILE(), botFunctions.myProfileButtons());
        cacheService.setState(userId, StateEnum.MY_PROFILE);
        cacheService.evictCachedUser(userId);
    }

    private class SendLikeAndMessagePhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            Long userAssessmentId = cacheService.getUserAssessmentId(userId);

            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            removeRecommendUser(userId, userAssessmentId);
            new Thread(() -> {
                String fileId = botFunctions.loadPhoto(message.getPhoto());
                sendLike(userEntity, userAssessmentId, false, LikeContentType.PHOTO, fileId);
            }).start();

            showNextUser(userId, userEntity);
        }
    }

    private class SendLikeAndMessageVoice implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            Long userAssessmentId = cacheService.getUserAssessmentId(userId);

            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            removeRecommendUser(userId, userAssessmentId);
            new Thread(() -> {
                String fileId = message.getVoice().getFileId();
                sendLike(userEntity, userAssessmentId, false, LikeContentType.VOICE, fileId);
            }).start();

            showNextUser(userId, userEntity);
        }
    }

    private class SendLikeAndMessageVideo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            Long userAssessmentId = cacheService.getUserAssessmentId(userId);

            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            removeRecommendUser(userId, userAssessmentId);
            new Thread(() -> {
                String fileId = message.getVideo().getFileId();
                sendLike(userEntity, userAssessmentId, false, LikeContentType.VIDEO, fileId);
            }).start();

            showNextUser(userId, userEntity);
        }
    }

    private void returnProfileWithoutChanges(Long userId, UserEntity userEntity) {
        botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getNULL_DATA_EDIT());
        goToProfile(userId, userEntity);
    }

    public void removeRecommendUser(Long myId, Long recommendUserId) {
        List<Long> excludedUserIds = cacheService.getExcludedUserIds(myId);
        excludedUserIds.add(recommendUserId);
        cacheService.putExcludedUserIds(myId, excludedUserIds);
    }

    private class Start implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndKeyboard(userId,
                    "Привет " + message.getFrom().getFirstName() + ", добро пожаловать в наше сообщество!" +
                            "\nGorodX - это №1 бот для поиска знакомств в ХМАО ЮГРЕ." +
                            "\nДавай создадим тебе анкету и найдём новые знакомства!", botFunctions.startButton());
            cacheService.evictAllUserCache(userId);
            cacheService.setState(userId, StateEnum.START_HANDLE);
        }
    }

    private class SendLikeAndMessageVideoNote implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            Long userAssessmentId = cacheService.getUserAssessmentId(userId);

            botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
            removeRecommendUser(userId, userAssessmentId);
            new Thread(() -> {
                String fileId = message.getVideoNote().getFileId();
                sendLike(userEntity, userAssessmentId, false, LikeContentType.VIDEO_NOTE, fileId);
            }).start();

            showNextUser(userId, userEntity);
        }
    }

    private class Result implements State {

        private final HashMap<String, State> states;

        public Result() {
            states = new HashMap<>();
            states.put("Продолжить", new Continue());
            states.put("Заполнить анкету заново", new Return());
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            State state = states.get(message.getText());
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }

        private class Return implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getASK_NAME());
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
            }
        }

        private class Continue implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                userEntity = cacheService.getCachedUser(userId);

                dataBaseService.saveAllUserAvatars(userEntity.getUserAvatars());
                userEntity.setActive(true);
                userEntity = dataBaseService.saveUser(userEntity);

                startSearch(userId, userEntity);
                cacheService.evictCachedUser(userId);
            }
        }
    }

    private class AskBeforeOff implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Выключить анкету")) {
                botFunctions.sendMessageAndKeyboard(
                        userId,
                        userEntity.getGender() == GenderEnum.MALE ? messages.getLEFT_MAN() : messages.getLEFT_WOMEN(),
                        botFunctions.restartButton()
                );
                userEntity.setActive(false);
                dataBaseService.saveUser(userEntity);
                cacheService.evictState(userId);
                cacheService.evictCachedUser(userId);
            } else if (messageText.equals("Вернуться назад")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class MyProfile implements State {
        final HashMap<String, State> answers;

        public MyProfile() {
            answers = new HashMap<>();
            answers.put("БИО", new BIO());
            answers.put("О себе", new AboutMe());
            answers.put("Город", new City());
            answers.put("Фото", new Photo());
            answers.put("Изменить анкету полностью", new FullEdit());
            answers.put("\uD83D\uDEAA Вернуться в меню", new Menu());
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = answers.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }

        public class BIO implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_NAME(), botFunctions.customButton(userEntity.getName()));
                cacheService.setState(userId, StateEnum.EDIT_NAME);
                cacheService.putCachedUser(userId, userEntity);
            }
        }

        public class AboutMe implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                if (userEntity.getAboutMe() == null) {
                    botFunctions.sendMessageAndKeyboard(userId, messages.getASK_ABOUT_ME(), botFunctions.customButton(messages.getSKIP_EDIT()));
                } else {
                    botFunctions.sendMessageAndKeyboard(userId, messages.getASK_ABOUT_ME(), botFunctions.removeAndCustomButtons(messages.getSKIP_EDIT()));
                }
                cacheService.setState(userId, StateEnum.EDIT_ABOUT_ME);
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
                botFunctions.sendMessageAndKeyboard(userId, messages.getEDIT_PHOTO(), botFunctions.customButton(messages.getSKIP_EDIT()));
                cacheService.setState(userId, StateEnum.EDIT_AVATAR);
                userEntity.setUserAvatars(new ArrayList<>());
                cacheService.putCachedUser(userId, userEntity);
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
    }

    private class ShowWhoLikedMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Посмотреть")) {
                likeChecker(userId, userEntity);
            } else if (messageText.equals("В другой раз")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class SendLikeAndMessageText implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            Long userAssessmentId = cacheService.getUserAssessmentId(userId);
            String messageText = message.getText();
            if (!messageText.equals("Отменить")) {
                botFunctions.sendMessageAndKeyboard(userId, "Сообщение отправлено", botFunctions.searchButtons());
                removeRecommendUser(userId, userAssessmentId);
                new Thread(() -> sendLike(userEntity, userAssessmentId, false, LikeContentType.TEXT, messageText)).start();
            } else {
                botFunctions.sendMessageAndKeyboard(userId, "Отмена отправки", botFunctions.searchButtons());
            }
            showNextUser(userId, userEntity);
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

        private UserEntity removeLike(UserEntity userEntity) {
            List<LikeEntity> likeEntityList = likeRepository.findAllByLikeReceiverOrderByIdAsc(userEntity.getId());
            LikeEntity like = likeEntityList.getFirst();
            UserEntity likedUser = dataBaseService.getUserById(like.getLikeSender()).get();
            dataBaseService.deleteLike(like.getId());
            return likedUser;
        }

        private class SendDislike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                removeLike(userEntity);
                likeChecker(userId, userEntity);
            }
        }

        private class SendReciprocity implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                UserEntity likeSender = removeLike(userEntity);
                String userName = botFunctions.getUsernameByUserId(likeSender.getId());
                botFunctions.sendMessageAndComplainButton(userId, likeSender.getId(), "Желаю вам хорошо провести время :)\nhttps://t.me/" + userName);
                likeChecker(userId, userEntity);
                new Thread(() -> sendLike(userEntity, likeSender.getId(), true, null, null)).start();
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

    private class StopShowProfilesWhoLikedMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Продолжить смотреть анкеты")) {
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
                Long userAssessmentId = cacheService.getUserAssessmentId(userId);

                botFunctions.sendMessageNotRemoveKeyboard(userId, "Лайк отправлен, ждём ответа");
                removeRecommendUser(userId, userAssessmentId);
                new Thread(() -> sendLike(userEntity, userAssessmentId, false, null, null)).start();

                showNextUser(userId, userEntity);
            }
        }
        private class SendLikeAndMessage implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndKeyboard(userId, messages.getSEND_LIKE_AND_MESSAGE(), botFunctions.cancelButton());
                cacheService.setState(userId, StateEnum.SEND_LIKE_AND_MESSAGE);
            }
        }

        private class SendDislike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                Long userAssessmentId = cacheService.getUserAssessmentId(userId);
                removeRecommendUser(userId, userAssessmentId);
                showNextUser(userId, userEntity);
            }
        }

        private class GoToMenu implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
                cacheService.evictUserAssessmentId(userId);
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
            botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getSUCCESS_SEND_ERROR());
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
                ComplaintEntity complainEntity = ComplaintEntity.builder()
                        .complainSenderId(userId)
                        .description(message.getText())
                        .complaintUserId(complaintUserId)
                        .build();
                dataBaseService.saveComplain(complainEntity);
                botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getSUCCESS_SEND_COMPLAINT());
            }
            goToMenu(userId, userEntity);
            cacheService.evictComplaintUser(userId);
        }
    }

    private class FindPeopleComplain implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();

            if (!messageText.equals("Отменить")) {
                Long complaintUserId = cacheService.getComplaintUserId(userId);
                ComplaintEntity complainEntity = ComplaintEntity.builder()
                        .complainSenderId(userId)
                        .description(message.getText())
                        .complaintUserId(complaintUserId)
                        .build();
                dataBaseService.saveComplain(complainEntity);

                List<Long> excludedUserIds = cacheService.getExcludedUserIds(userId);
                excludedUserIds.add(complaintUserId);
                cacheService.putExcludedUserIds(userId, excludedUserIds);

                botFunctions.sendMessageAndKeyboard(userId, messages.getSUCCESS_SEND_COMPLAINT(), botFunctions.searchButtons());
            }
            startSearch(userId, userEntity);
            cacheService.evictComplaintUser(userId);
        }
    }

    private class Error implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getERROR());
            cacheService.evictAllUserCache(userId);
            goToMenu(userId, userEntity);
        }
    }

    private class RolesController implements State {

        HashMap<String, RoleState> commands;

        public RolesController() {
            commands = new HashMap<>();
            commands.put("\uD83D\uDC51 Назначить создателя", new CreateSupeAdmin());
            commands.put("\uD83D\uDC6E Назначить администратора", new CreateAdmin());
            commands.put("⛔ Разжаловать", new RemoveRole());
        }

        private class CreateSupeAdmin implements RoleState {

            @Override
            public void handleInput(Long userId, UserDetailsEntity userDetailsEntity) {
                if (userDetailsEntity.getRole() != RoleEnum.SUPER_ADMIN) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, "Недостаточно прав");
                    return;
                }
                cacheService.setState(userId, StateEnum.ADD_SUPER_ADMINISTRATOR_ROLE);
                botFunctions.sendMessageAndRemoveKeyboard(userId, "Хорошо, теперь пришли id пользователя." +
                        "\nЧтобы его узнать, попроси пользователя ввести команду /myId, или самостоятельно найди человека в базе данных"
                );
            }
        }

        private class CreateAdmin implements RoleState {

            @Override
            public void handleInput(Long userId, UserDetailsEntity userDetailsEntity) {
                if (userDetailsEntity.getRole() != RoleEnum.SUPER_ADMIN) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, "Недостаточно прав");
                    return;
                }
                cacheService.setState(userId, StateEnum.ADD_ADMINISTRATOR_ROLE);
                botFunctions.sendMessageAndRemoveKeyboard(userId, "Хорошо, теперь пришли id пользователя." +
                        "\nЧтобы его узнать, попроси пользователя ввести команду /myId, или самостоятельно найди человека в базе данных"
                );
            }
        }

        private class RemoveRole implements RoleState {

            @Override
            public void handleInput(Long userId, UserDetailsEntity userDetailsEntity) {
                if (userDetailsEntity.getRole() != RoleEnum.SUPER_ADMIN) {
                    botFunctions.sendMessageNotRemoveKeyboard(userId, "Недостаточно прав");
                    return;
                }
                cacheService.setState(userId, StateEnum.REMOVE_ROLE);
                botFunctions.sendMessageAndRemoveKeyboard(userId, "Хорошо, теперь пришли id пользователя." +
                        "\nЧтобы его узнать, попроси пользователя ввести команду /myId, или самостоятельно найди человека в базе данных"
                );
            }
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            RoleState state = commands.get(messageText);
            if (state != null) {
                UserDetailsEntity userDetails = dataBaseService.getUserDetails(userId);
                state.handleInput(userId, userDetails);
            }
        }
    }

    private class AddSuperAdministratorRole implements State {

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserDetailsEntity userDetails;

            try {
                Long.valueOf(messageText);
                userDetails = dataBaseService.getUserDetails(Long.valueOf(messageText));
            } catch (Exception ex) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пользователя с таким id не существует");
                return;
            }

            UserDetailsEntity creator = dataBaseService.getUserDetails(userId);
            if (creator.getRole() != RoleEnum.SUPER_ADMIN) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Недостаточно прав");
            } else if (userDetails.getRole() == RoleEnum.SUPER_ADMIN) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пользователь уже имеет эту роль");
            } else {
                userDetails.setRole(RoleEnum.SUPER_ADMIN);
                dataBaseService.saveUserDetails(userDetails);
                botFunctions.sendMessageNotRemoveKeyboard(userDetails.getId(),
                        "Вы теперь " + RoleEnum.SUPER_ADMIN.getName() + " " + RoleEnum.SUPER_ADMIN.getSmile() +
                                "\nПодробнее /roleController" +
                                "\nНа эту должность вы были назначены пользователем - https://t.me/" + botFunctions.getUsernameByUserId(userId));
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Роль назначена ✅");
                goToRoleController(creator.getRole(), userId);
            }
        }
    }

    private class AddAdministratorRole implements State {

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserDetailsEntity userDetails;

            try {
                Long.valueOf(messageText);
                userDetails = dataBaseService.getUserDetails(Long.valueOf(messageText));
            } catch (Exception ex) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пользователя с таким id не существует");
                return;
            }

            UserDetailsEntity creator = dataBaseService.getUserDetails(userId);
            if (creator.getRole() != RoleEnum.SUPER_ADMIN) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Недостаточно прав");
            } else if (userDetails.getRole() == RoleEnum.ADMIN) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пользователь уже имеет эту роль");
            } else {
                userDetails.setRole(RoleEnum.ADMIN);
                dataBaseService.saveUserDetails(userDetails);
                botFunctions.sendMessageNotRemoveKeyboard(userDetails.getId(),
                        "Вы теперь " + RoleEnum.ADMIN.getName() + " " + RoleEnum.ADMIN.getSmile() +
                                "\nПодробнее /roleController" +
                                "\nНа эту должность вы были назначены пользователем - https://t.me/" + botFunctions.getUsernameByUserId(userId));
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Роль назначена ✅");
                goToRoleController(creator.getRole(), userId);
            }
        }
    }

    private class RemoveRole implements State {

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserDetailsEntity userDetails;

            try {
                Long.valueOf(messageText);
                userDetails = dataBaseService.getUserDetails(Long.valueOf(messageText));
            } catch (Exception ex) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пользователя с таким id не существует");
                return;
            }

            UserDetailsEntity creator = dataBaseService.getUserDetails(userId);
            if (creator.getRole() != RoleEnum.SUPER_ADMIN) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Недостаточно прав");
            } else if (userDetails.getRole() == RoleEnum.USER) {
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пользователь не имеет роли");
            } else {
                userDetails.setRole(RoleEnum.USER);
                dataBaseService.saveUserDetails(userDetails);
                botFunctions.sendMessageNotRemoveKeyboard(userDetails.getId(), "Вы были разжалованы в должности пользователем https://t.me/" + botFunctions.getUsernameByUserId(userId));
                botFunctions.sendMessageNotRemoveKeyboard(userId, "Пользователь разжалован ✅");
                goToRoleController(creator.getRole(), userId);
            }
        }
    }

    public void goToRoleController(RoleEnum roleEnum, Long userId) {
        if (roleEnum == RoleEnum.SUPER_ADMIN) {
            botFunctions.sendMessageAndKeyboard(userId,
                    "Ваша роль: " + roleEnum.getSmile() + " " + roleEnum.getName()
                            + "\n\n"
                            + RoleEnum.SUPER_ADMIN.getSmile() + " Список создателей:" + parseLinks(dataBaseService.getAllUserDetailsByRole(RoleEnum.SUPER_ADMIN))
                            + "\n\n"
                            + RoleEnum.ADMIN.getSmile() + " Список администраторов:" + parseLinks(dataBaseService.getAllUserDetailsByRole(RoleEnum.ADMIN)),
                    botFunctions.superAdminButtons());
            botFunctions.sendMessageNotRemoveKeyboard(userId,
                    """
                            ⭐ Ваши привилегии:
                            1. /show_errors - Показать ошибки отправленные пользователями
                            
                            2. /analysis - Анализ по анкетам
                            
                            3. /complaints - Жалобы на других пользователей, возможность бана
                            
                            4. /roleController - система контроля ролей (C возможностью добавлять и убирать роли)
                            """
            );
            cacheService.setState(userId, StateEnum.ROLES_CONTROLLER);
        } else if (roleEnum == RoleEnum.ADMIN) {
            botFunctions.sendMessageAndRemoveKeyboard(userId,
                    "Ваша роль: " + roleEnum.getSmile() + " " + roleEnum.getName()
                            + "\n\n"
                            + RoleEnum.SUPER_ADMIN.getSmile() + " Список создателей:" + parseLinks(dataBaseService.getAllUserDetailsByRole(RoleEnum.SUPER_ADMIN))
                            + "\n\n"
                            + RoleEnum.ADMIN.getSmile() + " Список администраторов:" + parseLinks(dataBaseService.getAllUserDetailsByRole(RoleEnum.ADMIN))
                    );
            botFunctions.sendMessageNotRemoveKeyboard(userId,
                    """
                            ⭐ Ваши привилегии:
                            1. /show_errors - Показать ошибки отправленные пользователями
                            
                            2. /analysis - Анализ по анкетам
                            
                            3. /complaints - Жалобы на других пользователей, возможность бана
                            
                            4. /roleController - система контроля ролей (Без возможности добавлять и убирать роли)
                            """
            );
        }
    }

    private String parseLinks(List<UserDetailsEntity> admins) {
        if (admins.isEmpty()) {
            return "\nНе назначены";
        }

        AtomicInteger i = new AtomicInteger(0);
        return admins.stream()
                .map(admin -> {
                    StringBuilder parsedLink = new StringBuilder();
                    String link = "https://t.me/" + botFunctions.getUsernameByUserId(admin.getId());
                    parsedLink.append("\n")
                            .append(i.incrementAndGet())
                            .append(". ")
                            .append(link)
                            .append("\nid: ")
                            .append(admin.getId());
                    return parsedLink.toString();
                })
                .collect(Collectors.joining("\n"));
    }
}
