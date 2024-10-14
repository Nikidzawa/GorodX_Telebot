package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.complaint.ComplaintEntity;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {

    List<ComplaintEntity> findAllByComplaintUserId(Long userId);

    @Query("SELECT COUNT(ce) FROM ComplaintEntity ce WHERE ce.complaintUserId = :complaintUserId")
    Long getCountComplaintsByUserId(Long complaintUserId);
}
