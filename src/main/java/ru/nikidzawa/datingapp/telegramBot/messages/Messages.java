package ru.nikidzawa.datingapp.telegramBot.messages;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Messages {

    //REGISTRATION
    @Value("${ASK_NAME}")
    String ASK_NAME;
    @Value("${ASK_AGE}")
    String ASK_AGE;
    @Value("${ASK_GENDER}")
    String ASK_GENDER;
    @Value("${ASK_GENDER_SEARCH}")
    String ASK_GENDER_SEARCH;
    @Value("${ASK_CITY}")
    String ASK_CITY;
    @Value("${ASK_ABOUT_ME}")
    String ASK_ABOUT_ME;
    @Value("${ASK_AVATAR}")
    String ASK_AVATAR;
    @Value("${RESULT}")
    String RESULT;

    //EDIT
    @Value("${EDIT_NAME}")
    String EDIT_NAME;
    @Value("${EDIT_AGE}")
    String EDIT_AGE;
    @Value("${EDIT_CITY}")
    String EDIT_CITY;
    @Value("${EDIT_PHOTO}")
    String EDIT_PHOTO;
    @Value("${NULL_DATA_EDIT}")
    String NULL_DATA_EDIT;
    @Value("${EDIT_RESULT}")
    String EDIT_RESULT;
    @Value("${STOP_ADD_AVATAR}")
    String STOP_ADD_AVATAR;
    @Value("${SKIP_ADD_AVATAR}")
    String SKIP_EDIT;

    //MENU
    @Value("${MENU}")
    String MENU;
    @Value("${MY_PROFILE}")
    String MY_PROFILE;

    //LEFT GROUP OR DISABLE PROFILE
    @Value("${LEFT_MAN}")
    String LEFT_MAN;
    @Value("${LEFT_WOMEN}")
    String LEFT_WOMEN;

    @Value("${ASK_BEFORE_OFF_MAN}")
    String ASK_BEFORE_OFF_MAN;
    @Value("${ASK_BEFORE_OFF_WOMEN}")
    String ASK_BEFORE_OFF_WOMEN;

    //EXCEPTIONS
    @Value("${INVALID_VALUE}")
    String INVALID_VALUE;
    @Value("${INVALID_FORMAT_EXCEPTION}")
    String INVALID_FORMAT_EXCEPTION;
    @Value("${IS_NOT_A_NUMBER_EXCEPTION}")
    String IS_NOT_A_NUMBER_EXCEPTION;
    @Value("${AGE_LIMIT_SYMBOLS_EXCEPTIONS}")
    String AGE_LIMIT_SYMBOLS_EXCEPTIONS;
    @Value("${NAME_LIMIT_SYMBOLS_EXCEPTIONS}")
    String NAME_LIMIT_SYMBOLS_EXCEPTIONS;
    @Value("${ABOUT_ME_LIMIT_SYMBOLS_EXCEPTIONS}")
    String ABOUT_ME_LIMIT_SYMBOLS_EXCEPTIONS;
    @Value("${CITY_LIMIT_SYMBOLS_EXCEPTIONS}")
    String CITY_LIMIT_SYMBOLS_EXCEPTIONS;
    @Value("${NOT_REGISTER}")
    String NOT_REGISTER;
    @Value("${ROLE_EXCEPTION}")
    String ROLE_EXCEPTION;
    @Value("${NOT_ENOUGH}")
    String NOT_ENOUGH;
    @Value("${ERROR}")
    String ERROR;

    //COMMANDS
    @Value("${FAQ}")
    String FAQ;
    @Value("${FAQ_1}")
    String FAQ_1;
    @Value("${FAQ_2}")
    String FAQ_2;
    @Value("${SEND_COMPLAINT}")
    String SEND_COMPLAINT;
    @Value("${SUCCESS_SEND_COMPLAINT}")
    String SUCCESS_SEND_COMPLAINT;
    @Value("${BLOCK}")
    String BLOCK;
    @Value("${PEACE}")
    String PEACE;
    @Value("${SEND_ERROR}")
    String SEND_ERROR;
    @Value("${SUCCESS_SEND_ERROR}")
    String SUCCESS_SEND_ERROR;

    //FIND_PEOPLES
    @Value("${SEND_LIKE_AND_MESSAGE}")
    String SEND_LIKE_AND_MESSAGE;
}
