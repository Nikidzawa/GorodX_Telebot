package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nikidzawa.datingapp.store.entities.user.RoleEnum;
import ru.nikidzawa.datingapp.store.entities.user.UserDetailsEntity;

import java.util.List;

public interface UserDetailsRepository extends JpaRepository<UserDetailsEntity, Long> {
    List<UserDetailsEntity> findAllByRole(RoleEnum role);
}
