package ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates;


public enum StateEnum {
    //Регистрация и заполнение анкеты с нуля
    START,
    START_HANDLE,
    ASK_NAME,
    ASK_AGE,
    ASK_GENDER,
    ASK_GENDER_SEARCH,
    ASK_CITY,
    ASK_ABOUT_ME,
    ASK_AVATAR,
    RESULT,

    //Профиль и редактирование
    MY_PROFILE,
    //
    EDIT_NAME,
    EDIT_AGE,
    //
    EDIT_CITY,
    //
    EDIT_ABOUT_ME,
    //
    EDIT_AVATAR,
    //
    EDIT_RESULT,

    //Выход из группы или отключение анкеты
    LEFT,
    ASK_BEFORE_OFF,
    WELCOME_BACK,
    WELCOME_BACK_HANDLE,
    LIKE_ACCEPT,

    //Просмотр анкет
    FIND_PEOPLES,
    SEND_LIKE_AND_MESSAGE,
    SHOW_WHO_LIKED_ME,
    SHOW_PROFILES_WHO_LIKED_ME,
    STOP_SHOW_PROFILES_WHO_LIKED_ME,

    //меню
    MENU,
    SUPER_MENU,

    //Команды
    FAQ,
    FAQ_RESPONSE,
    SEND_ERROR,
    CALL_BACK_QUERY_COMPLAIN,
    FIND_PEOPLES_COMPLAIN,
    ERROR
}
