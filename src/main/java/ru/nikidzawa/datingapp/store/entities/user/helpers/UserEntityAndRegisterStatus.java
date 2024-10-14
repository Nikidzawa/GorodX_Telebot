package ru.nikidzawa.datingapp.store.entities.user.helpers;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntityAndRegisterStatus {

    UserEntity userEntity;

    boolean hasBeenRegistered;
}
