package ru.nikidzawa.datingapp.telegramBot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class BotData {
    @Value("${BOT_LOGIN}")
    private String login;

    @Value("${BOT_TOKEN}")
    private String token;
}
