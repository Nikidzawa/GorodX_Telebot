package ru.nikidzawa.datingapp.store.entities.user;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "user_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntity implements Serializable {

    public UserEntity (Long id) {
        this.id = id;
    }

    @Id
    Long id;

    @Column(length = 100)
    String name;

    int age;

    String location;

    @Column(length = 150)
    String hobby;

    @Column(length = 1000)
    String aboutMe;

    double longitude;

    double latitude;

    boolean isShowGeo = false;

    boolean isActive = false;

    boolean isBanned = false;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    List<UserAvatar> userAvatars;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity user = (UserEntity) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
