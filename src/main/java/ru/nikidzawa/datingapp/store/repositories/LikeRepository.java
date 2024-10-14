package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    List<LikeEntity> findAllByLikeReceiverOrderByIdAsc(long likeReceiver);

    @Query("SELECT COUNT(le) FROM LikeEntity le WHERE le.likeReceiver = :userEntityId")
    Long getCountByLikeReceiver(@Param("userEntityId") Long userEntityId);
}
