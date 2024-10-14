package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u " +
            "WHERE u.isActive = true " +
            "AND u.isBanned = false " +
            "AND u.id != :userId " +
            "AND SQRT(POWER(u.longitude - :givenLongitude, 2) + POWER(u.latitude - :givenLatitude, 2)) <= 0.045" +
            "ORDER BY SQRT(POWER(u.longitude - :givenLongitude, 2) + POWER(u.latitude - :givenLatitude, 2))")
    List<UserEntity> findAllOrderByDistance(@Param("userId") Long userId,
                                            @Param("givenLongitude") double givenLongitude,
                                            @Param("givenLatitude") double givenLatitude);

    @Query("SELECT u.location, COUNT(u) AS user_count " +
            "FROM UserEntity u " +
            "WHERE u.isActive = true AND u.isBanned = false " +
            "GROUP BY u.location " +
            "ORDER BY user_count DESC " +
            "LIMIT 10")
    String[] findTop10CitiesByUserCount();

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isActive = true AND u.isBanned = false")
    Long countActiveAndNotBannedUsers();
}
