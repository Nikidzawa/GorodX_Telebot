package ru.nikidzawa.datingapp.api.internal.controllers.users;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.store.entities.user.RoleEnum;
import ru.nikidzawa.datingapp.store.entities.user.UserDetailsEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.UserDetailsRepository;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;
import ru.nikidzawa.datingapp.telegramBot.messages.Messages;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateMachine;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/users/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserRepository userRepository;

    UserDetailsRepository userDetailsRepository;

    StateMachine stateMachine;

    @Cacheable(cacheNames = "user", key = "#userId")
    @GetMapping("{userId}")
    public UserEntity getUser (@PathVariable Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    @PostMapping("verifyAccount/{userId}/{nickname}")
    public void verifyAccount (@PathVariable Long userId, @PathVariable String nickname) {
        System.out.println("Никнейм " + nickname);

        userDetailsRepository.saveAndFlush(
                UserDetailsEntity.builder()
                        .id(userId)
                        .role(RoleEnum.USER)
                        .isVerify(true)
                        .build()
        );

        Message message = new Message();
        User user = new User();
        user.setFirstName(nickname);
        message.setFrom(user);

        stateMachine.handleInput(StateEnum.START, userId, null, message, false);
    }
}
