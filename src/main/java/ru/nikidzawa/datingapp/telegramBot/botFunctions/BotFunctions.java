package ru.nikidzawa.datingapp.telegramBot.botFunctions;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import ru.nikidzawa.datingapp.store.entities.like.LikeContentType;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.RoleEnum;
import ru.nikidzawa.datingapp.store.entities.user.UserAvatar;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.TelegramBot;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.*;

public class BotFunctions {

    public final HashMap<LikeContentType, SendMessageType> sendMessage;

    private final TelegramBot telegramBot;

    private class SendMessageTypeText implements SendMessageType {
        @Override
        @SneakyThrows
        public void handleInput(Long userId, LikeEntity like) {
            sendMessageNotRemoveKeyboard(userId, "\uD83D\uDC8CСообщение для тебя: " + like.getContent());
        }
    }

    private class SendMessageTypePhoto implements SendMessageType {
        @Override
        @SneakyThrows
        public void handleInput(Long userId, LikeEntity like) {
            sendMessageNotRemoveKeyboard(userId, "\uD83D\uDC8CСообщение для тебя:");
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(userId);
            sendPhoto.setPhoto(getInputFile(like.getContent()));
            telegramBot.execute(sendPhoto);
        }
    }

    private class SendMessageTypeVideo implements SendMessageType {
        @Override
        @SneakyThrows
        public void handleInput(Long userId, LikeEntity like) {
            sendMessageNotRemoveKeyboard(userId, "\uD83D\uDC8CСообщение для тебя:");
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(userId);
            sendVideo.setVideo(getInputFile(like.getContent()));
            telegramBot.execute(sendVideo);
        }
    }

    private class SendMessageTypeVideoNote implements SendMessageType {
        @Override
        @SneakyThrows
        public void handleInput(Long userId, LikeEntity like) {
            sendMessageNotRemoveKeyboard(userId, "\uD83D\uDC8CСообщение для тебя:");
            SendVideoNote sendVideoNote = new SendVideoNote();
            sendVideoNote.setChatId(userId);
            sendVideoNote.setVideoNote(getInputFile(like.getContent()));
            telegramBot.execute(sendVideoNote);
        }
    }

    private class SendMessageTypeVoice implements SendMessageType {
        @Override
        @SneakyThrows
        public void handleInput(Long userId, LikeEntity like) {
            sendMessageNotRemoveKeyboard(userId, "\uD83D\uDC8CСообщение для тебя:");
            SendVoice sendVoice = new SendVoice();
            sendVoice.setChatId(userId);
            sendVoice.setVoice(getInputFile(like.getContent()));
            telegramBot.execute(sendVoice);
        }
    }


    public BotFunctions (TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
        sendMessage = new HashMap<>();
        sendMessage.put(LikeContentType.TEXT, new SendMessageTypeText());
        sendMessage.put(LikeContentType.PHOTO, new SendMessageTypePhoto());
        sendMessage.put(LikeContentType.VIDEO, new SendMessageTypeVideo());
        sendMessage.put(LikeContentType.VIDEO_NOTE, new SendMessageTypeVideoNote());
        sendMessage.put(LikeContentType.VOICE, new SendMessageTypeVoice());
    }

    @SneakyThrows
    public void sendMessageAndRemoveKeyboard(Long id, String message){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));
        telegramBot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageNotRemoveKeyboard(Long id, String message){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        telegramBot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageAndComplainButton (Long id, Long complaintUserId, String message){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(complainButton(complaintUserId));
        telegramBot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageAndKeyboard(Long id, String message, ReplyKeyboard replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        telegramBot.execute(sendMessage);
    }


    private static SendMediaGroup getSendMediaGroup(Long userId, List<UserAvatar> userAvatars) {
        List<InputMedia> inputMedia = new ArrayList<>();
        userAvatars.forEach(userAvatar -> {
            if (userAvatar.isPhoto()) {
                inputMedia.add(new InputMediaPhoto(userAvatar.getFile()));
            } else {
                inputMedia.add(new InputMediaVideo(userAvatar.getFile()));
            }
        });
        return SendMediaGroup.builder()
                .medias(inputMedia)
                .chatId(userId)
                .build();
    }

    public ReplyKeyboardMarkup menuButtons(String smile) {return keyboardMarkupBuilder(List.of("1 \uD83D\uDE80", "2 " + smile, "3 \uD83D\uDCA4"));}

    public ReplyKeyboardMarkup superMenuButtons(String smile) {return keyboardMarkupBuilder(List.of("1 ❤", "2 \uD83D\uDE80", "3 " + smile, "4 \uD83D\uDCA4"));}

    public ReplyKeyboardMarkup resultButtons() {return keyboardMarkupBuilder(List.of("Заполнить анкету заново", "Продолжить"));}

    public ReplyKeyboardMarkup skipButton() {return keyboardMarkupBuilder(List.of("Пропустить"));}

    public ReplyKeyboardMarkup startButton() {return keyboardMarkupBuilder(List.of("Создать анкету"));}

    public ReplyKeyboardMarkup myProfileButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("БИО");
        firstRow.add("О себе");
        firstRow.add("Город");
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("Фото");
        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add("Изменить анкету полностью");
        thirdRow.add("\uD83D\uDEAA Вернуться в меню");
        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);
        keyboardRows.add(thirdRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup superAdminButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(RoleEnum.SUPER_ADMIN.getSmile() + " Назначить создателя");
        firstRow.add(RoleEnum.ADMIN.getSmile() + " Назначить администратора");
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("⛔ Разжаловать");
        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup stopShowProfilesWhoLikedMeButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("Продолжить смотреть анкеты");
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("Вернуться в меню");
        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup askBeforeOffButtons() {return keyboardMarkupBuilder(List.of("Выключить анкету", "Вернуться назад"));}

    public ReplyKeyboardMarkup editResultButtons() {return keyboardMarkupBuilder(List.of("Сохранить", "Отменить"));}

    public ReplyKeyboardMarkup customButton(String button) {return keyboardMarkupBuilder(List.of(button));}

    public ReplyKeyboardMarkup customButton(String button1, String button2) {return keyboardMarkupBuilder(List.of(button1, button2));}

    public ReplyKeyboardMarkup customButton(String button1, String button2, String button3) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(button1);
        firstRow.add(button2);
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(button3);
        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup locationButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow secondRow = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton();
        locationButton.setRequestLocation(true);
        locationButton.setText("\uD83D\uDCCDОтправить мою геолокацию");
        secondRow.add(locationButton);
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;}

    public ReplyKeyboardMarkup customLocationButtons(String button) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(button);
        keyboardRows.add(firstRow);

        KeyboardRow secondRow = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton();
        locationButton.setRequestLocation(true);
        locationButton.setText("\uD83D\uDCCDОтправить мою геолокацию");
        secondRow.add(locationButton);
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup webAppButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton webAppButton = new KeyboardButton();
        WebAppInfo webAppInfo = new WebAppInfo();
        webAppInfo.setUrl("https://gorodx-hmao.ru");
        webAppButton.setText("Получить доступ \uD83D\uDD11");
        webAppButton.setWebApp(webAppInfo);
        row.add(webAppButton);
        keyboardRows.add(row);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup skipAndCustomButtons(String button) {return keyboardMarkupBuilder(List.of(button, "Пропустить"));}

    public ReplyKeyboardMarkup cancelButton() {return keyboardMarkupBuilder(List.of("Отменить"));}

    public ReplyKeyboardMarkup removeAndCustomButtons(String button) {return keyboardMarkupBuilder(List.of(button, "Убрать"));}

    public ReplyKeyboardMarkup showWhoLikedMeButtons() {return keyboardMarkupBuilder(List.of("Посмотреть", "В другой раз"));}

    public ReplyKeyboardMarkup faqButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("1");
        firstRow.add("2");
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("Вернуться в меню");
        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;}

    public ReplyKeyboardMarkup faqResponseButtons() {return keyboardMarkupBuilder(List.of("Назад", "Вернуться в меню"));}

    public ReplyKeyboardMarkup restartButton() {return keyboardMarkupBuilder(List.of("Включить анкету"));}

    public ReplyKeyboardMarkup searchButtons() {return keyboardMarkupBuilder(List.of("❤", "\uD83D\uDC8C", "\uD83D\uDC4E", "\uD83D\uDCA4"));}

    public ReplyKeyboardMarkup reciprocityButtons() {return keyboardMarkupBuilder(List.of("❤", "\uD83D\uDC4E", "\uD83D\uDCA4"));}

    public InlineKeyboardMarkup complainButton (Long complaintUserId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Пожаловаться");
        button.setCallbackData("complaint," + complaintUserId);
        row.add(button);
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        return markup;
    }

    public InlineKeyboardMarkup judgeButtons (Long userId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton block = new InlineKeyboardButton();
        block.setText("\uD83D\uDEABЗаблокировать");
        block.setCallbackData("block," + userId);
        InlineKeyboardButton peace = new InlineKeyboardButton();
        peace.setText("\uD83D\uDD4AПомиловать");
        peace.setCallbackData("peace," + userId);
        row.add(block);
        row.add(peace);
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private ReplyKeyboardMarkup keyboardMarkupBuilder(List<String> buttonLabels) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        for (String label : buttonLabels) {
            keyboardRow.add(label);
        }
        keyboardRows.add(keyboardRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public String loadPhoto (List<PhotoSize> photos) {
        return photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null)
                .getFileId();
    }

    @SneakyThrows
    private InputFile getInputFile(String fileId) {
        GetFile getFile = new GetFile(fileId);
        File file = telegramBot.execute(getFile);
        String filePath = file.getFilePath();
        URL fileUrl = new URL("https://api.telegram.org/file/bot" + telegramBot.getBotToken() + "/" + filePath);
        InputStream inputStream = fileUrl.openStream();
        return new InputFile(inputStream, "file");
    }

    @SneakyThrows
    public void sendDatingProfile(Long userId, UserEntity userEntity) {
        sendUserProfile(userId, userEntity, getUserInfo(userEntity, ""));
    }

    @SneakyThrows
    public void sendDatingProfile(Long userId, UserEntity anotherUser, UserEntity myProfile) {
        String distance = "";
        if (myProfile.isShowGeo() && anotherUser.isShowGeo()) {
            distance = getDistance(anotherUser, myProfile);
        }
        sendUserProfile(userId, anotherUser, getUserInfo(anotherUser, distance));
    }


    private String getUserInfo(UserEntity userEntity, String distance) {
        String aboutMe = userEntity.getAboutMe();
        String userName = userEntity.getName();
        String age = String.valueOf(userEntity.getAge());
        String location = userEntity.getLocation();
        return userName + ", " + age + ", " + location + distance + (aboutMe == null ? "" : "\n" + aboutMe);
    }

    @SneakyThrows
    private void sendUserProfile(Long userId, UserEntity userEntity, String profileInfo) {
        List<UserAvatar> userAvatars = userEntity.getUserAvatars();
        if (userAvatars.size() > 1) {
            SendMediaGroup sendMediaGroup = getSendMediaGroup(userId, userAvatars);
            sendMediaGroup.getMedias().getFirst().setCaption(profileInfo);
            telegramBot.execute(sendMediaGroup);
        } else {
            UserAvatar userAvatar = userAvatars.getFirst();
            if (userAvatar.isPhoto()) {
                telegramBot.execute(SendPhoto.builder()
                        .chatId(userId)
                        .photo(getInputFile(userAvatar.getFile()))
                        .caption(profileInfo)
                        .build());
            } else {
                telegramBot.execute(SendVideo.builder()
                        .chatId(userId)
                        .video(getInputFile(userAvatar.getFile()))
                        .caption(profileInfo)
                        .build());
            }
        }
    }

    private String getDistance(UserEntity anotherUser, UserEntity me) {
        double lat1 = anotherUser.getLatitude();
        double lon1 = anotherUser.getLongitude();

        double lat2 = me.getLatitude();
        double lon2 = me.getLongitude();

        double dLat = toRadians(lat2 - lat1);
        double dLon = toRadians(lon2 - lon1);

        double a = pow(sin(dLat / 2), 2) + pow(sin(dLon / 2), 2) * cos(toRadians(lat1)) * cos(toRadians(lat2));
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));
        double distance = 6371 * c;
        if (distance < 250) {
            return formatDistance(distance);
        }
        return "";
    }


    @SneakyThrows
    public ChatMember getChatMember (Long userId) {
        return telegramBot.execute(new GetChatMember(telegramBot.getBotUsername(), userId));
    }

    private static String formatDistance(double distance) {
        int meters;
        if (distance < 1) {
            meters = max(100, min(900, (int) round(distance * 100)));
            return " \uD83D\uDCCD" + meters + " м";
        } else {
            meters = (int) round(distance);
            return " \uD83D\uDCCD" + meters + " км";
        }
    }

    @SneakyThrows
    public String getUsernameByUserId(Long userId) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(userId);
        getChatMember.setUserId(userId);
        ChatMember chatMember = telegramBot.execute(getChatMember);
        return chatMember.getUser().getUserName();
    }
}