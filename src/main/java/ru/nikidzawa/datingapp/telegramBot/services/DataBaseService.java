package ru.nikidzawa.datingapp.telegramBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.store.entities.complaint.ComplaintEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.RoleEnum;
import ru.nikidzawa.datingapp.store.entities.user.UserAvatar;
import ru.nikidzawa.datingapp.store.entities.user.UserDetailsEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.*;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;

import java.util.List;
import java.util.Optional;

@Service
public class DataBaseService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ErrorRepository errorRepository;

    @Autowired
    private UserAvatarRepository userAvatarRepository;

    @Autowired
    UserDetailsRepository userDetailsRepository;

    @Autowired
    private CacheService cacheService;

    public Optional<UserEntity> getUserById (Long id) {
        Optional<UserEntity> cachedUser = cacheService.getUserFromCache(id);
        if (cachedUser.isEmpty()) {
            Optional<UserEntity> userEntity = userRepository.findById(id);
            cacheService.putUserInCache(id, userEntity.orElse(new UserEntity()));
            return userEntity;
        } else {
            return cachedUser;
        }
    }

    public UserEntity saveUser(UserEntity user) {
        cacheService.putUserInCache(user.getId(), user);
        return userRepository.saveAndFlush(user);
    }

    public void saveUserDetails(UserDetailsEntity userDetails) {
        userDetailsRepository.saveAndFlush(userDetails);
        cacheService.putUserDetailsInCache(userDetails.getId(), userDetails);
    }

    public UserDetailsEntity getUserDetails(Long userId) {
        Optional<UserDetailsEntity> optionalUserDetails = cacheService.getUserDetailsFromCache(userId);
        if (optionalUserDetails.isEmpty()) {
            Optional<UserDetailsEntity> userDetailsEntity = userDetailsRepository.findById(userId);
            cacheService.putUserDetailsInCache(userId, userDetailsEntity.get());
            return userDetailsEntity.get();
        } else {
            return optionalUserDetails.get();
        }
    }

    public List<UserDetailsEntity> getAllUserDetailsByRole(RoleEnum role) {
        return userDetailsRepository.findAllByRole(role);
    }

    public void saveAllUserAvatars (List<UserAvatar> userAvatars) {
        userAvatarRepository.saveAllAndFlush(userAvatars);
    }

    public Long getCountActiveAndNotBannedUsers () {
        return userRepository.countActiveAndNotBannedUsers();
    }

    public String[] findTop10CitiesByUserCount() {
        return userRepository.findTop10CitiesByUserCount();
    }

    public LikeEntity saveLike (LikeEntity likeEntity) {
        return likeRepository.saveAndFlush(likeEntity);
    }

    public void saveComplain(ComplaintEntity complainEntity) {
        complaintRepository.saveAndFlush(complainEntity);
    }

    public List<ComplaintEntity> findByComplaintUser(Long complaintUserId) {
        return complaintRepository.findAllByComplaintUserId(complaintUserId);
    }

    public void deleteAllComplainEntities(List<ComplaintEntity> complainEntities) {
        complaintRepository.deleteAll(complainEntities);
    }

    public List<ComplaintEntity> findAllComplaints() {
        return complaintRepository.findAll();
    }

    public Long getComplainCountByUser(Long complaintUserId) {
        return complaintRepository.getCountComplaintsByUserId(complaintUserId);
    }

    public void deleteLike(Long likeId) {
        likeRepository.deleteById(likeId);
    }

    public List<UserEntity> getRecommendation(UserEntity myProfile) {
        return userRepository.findAllOrderByDistance(myProfile.getId(), myProfile.getLongitude(), myProfile.getLatitude());
    }

    public Long getAllPeopleCountWhoLikeUserEntity(Long userEntityId) {
        return likeRepository.getCountByLikeReceiver(userEntityId);
    }

    public List<LikeEntity> getAllLikeEntityByUserId(long userId) {
        return likeRepository.findAllByLikeReceiverOrderByIdAsc(userId);
    }

    public void saveError (ErrorEntity errorEntity) {
        errorRepository.saveAndFlush(errorEntity);
    }

    public List<ErrorEntity> findAllErrors () {
        return errorRepository.findAll();
    }

    public void deleteError (ErrorEntity errorEntity) {
        errorRepository.delete(errorEntity);
    }
}