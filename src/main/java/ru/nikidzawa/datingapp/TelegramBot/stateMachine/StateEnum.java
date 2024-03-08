package ru.nikidzawa.datingapp.TelegramBot.stateMachine;


public enum StateEnum {
    //Ошибки
    CHECK_GROUP_MEMBER,

    //Регистрация
    START,
    ASK_NAME,
    ASK_AGE,
    ASK_CITY,
    ASK_HOBBY,
    ASK_SPEND_TIME,
    ASK_ABOUT_ME,
    ASK_PHOTO,
    RESULT,


    //Выход
    LEFT,

    //Действия из меню
    MENU,
}
