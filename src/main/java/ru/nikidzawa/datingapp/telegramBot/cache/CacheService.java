package ru.nikidzawa.datingapp.telegramBot.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.store.entities.user.UserDetailsEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/*
Справочник по кешу.
1. state (Одно значение) тесно связан с работой стейт машин. Благодаря ему, бот понимает на каком этапе сейчас находится пользователь
и что он от него хочет. Не имеет срока истечения. Состояние удаляется из кэша только при отключении анкеты.

2. cached_user (Одно значение) хранит копию обновлённой анкеты пользователя.
Т.е. при создании или редактировании анкеты, промежуточные данные не сразу сохраняются в базу данных, а попадают в этот кеш.
Не имеет срока истечения. Кеш необходимо контролировать вручную.

3. complain_user_id (Одно значение) хранит id пользователя на которого совершается жалоба.
Необходим, чтобы сохранить анкету этого пользователя, пока пользователь отправляющий жалобу не укажет её причину,
или пока он не отменит жалобу. Не имеет срока истечения. Кеш необходимо контролировать вручную.

4. user_assessment_id (Одно значение) хранит id текущего рекомендованного пользователя при просмотре анкет.
Данные используются для дальнейшей оценки анкеты или при отправке жалобы.
Не имеет срока истечения. Кеш необходимо контролировать вручную.

5. excluded_user_ids (Список) хранит список id рекомендованных пользователей, которые уже были оценены.
Необходим для формирования рекомендаций. Срок обновления - 24 часа с момента добавления первой записи,
или в том случае, если все рекомендации просмотрены.

6. user (Одно значение, JSON) хранит актуальную копию анкеты пользователя из базы данных. Удаляется автоматически в
течение 1 часа после последнего вызова из базы данных. Никогда не удаляется вручную.
 */
@Service
public class CacheService {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    static String STATE_CACHE_NAME = "state";
    static String CACHED_USER_CACHE_NAME = "cached_user";
    static String COMPLAIN_USER_CACHE_NAME = "complain_user_id";
    static String USER_ASSESSMENT_CACHE_NAME = "user_assessment_id";
    static String EXCLUDED_USERS_CACHE_NAME = "excluded_user_ids";
    static String USER_CACHE_NAME = "user::";
    static String USER_DETAILS_CACHE_NAME = "user_details::";

    public Cache.ValueWrapper getCurrentState (Long userId) {
        return cacheManager.getCache(STATE_CACHE_NAME).get(userId);
    }

    public void setState (Long userId, StateEnum stateEnum) {
        cacheManager.getCache(STATE_CACHE_NAME).put(userId, stateEnum);
    }

    public void evictState (Long userId) {
        cacheManager.getCache(STATE_CACHE_NAME).evict(userId);
    }

    @SneakyThrows
    public UserEntity getCachedUser(Long userId) {
        String cachedUserJson = redisTemplate.opsForValue().get(CACHED_USER_CACHE_NAME + userId);
        if (cachedUserJson != null) {
            return objectMapper.readValue(cachedUserJson, UserEntity.class);
        }
        return null;
    }

    @SneakyThrows
    public void putCachedUser(Long userId, UserEntity user) {
        String cachedUserJson = objectMapper.writeValueAsString(user);
        redisTemplate.opsForValue().set(CACHED_USER_CACHE_NAME + userId, cachedUserJson);
    }

    public void evictCachedUser(Long userId) {
        redisTemplate.delete(CACHED_USER_CACHE_NAME + userId);
    }

    public Long getComplaintUserId(Long complainSenderId) {
        return (Long) cacheManager.getCache(COMPLAIN_USER_CACHE_NAME).get(complainSenderId).get();
    }

    public void putComplaintUser(Long complainSenderId, Long complaintUserId) {
        cacheManager.getCache(COMPLAIN_USER_CACHE_NAME).put(complainSenderId, complaintUserId);
    }

    public void evictComplaintUser(Long complainSenderId) {
        cacheManager.getCache(COMPLAIN_USER_CACHE_NAME).evict(complainSenderId);
    }

    @SneakyThrows
    public Long getUserAssessmentId(Long myId) {
        String excludedUserIdsJson = redisTemplate.opsForValue().get(USER_ASSESSMENT_CACHE_NAME + myId);
        if (excludedUserIdsJson != null) {
            return objectMapper.readValue(excludedUserIdsJson, Long.class);
        }
        return null;
    }
    @SneakyThrows
    public void putUserAssessmentId(Long myId, Long userAssessmentId) {
        String excludedUserIdsJson = objectMapper.writeValueAsString(userAssessmentId);
        redisTemplate.opsForValue().set(USER_ASSESSMENT_CACHE_NAME + myId, excludedUserIdsJson);
        redisTemplate.expire(USER_ASSESSMENT_CACHE_NAME + myId, 3, TimeUnit.HOURS);
    }

    public void evictUserAssessmentId(Long myId) {
        redisTemplate.delete(USER_ASSESSMENT_CACHE_NAME + myId);
    }


    @SneakyThrows
    public List<Long> getExcludedUserIds(Long myId) {
        String excludedUserIdsJson = redisTemplate.opsForValue().get(EXCLUDED_USERS_CACHE_NAME + myId);
        if (excludedUserIdsJson != null) {
            Long[] excludedUserIds = objectMapper.readValue(excludedUserIdsJson, Long[].class);
            return new ArrayList<>(Arrays.asList(excludedUserIds));
        }
        return new ArrayList<>();
    }

    @SneakyThrows
    public void putExcludedUserIds(Long myId, List<Long> excludedUserIds) {
        String excludedUserIdsJson = objectMapper.writeValueAsString(excludedUserIds);
        boolean cacheExists = Boolean.TRUE.equals(redisTemplate.hasKey(EXCLUDED_USERS_CACHE_NAME + myId));
        if (cacheExists) {
            redisTemplate.opsForValue().set(EXCLUDED_USERS_CACHE_NAME + myId, excludedUserIdsJson);
        } else {
            redisTemplate.opsForValue().set(EXCLUDED_USERS_CACHE_NAME + myId, excludedUserIdsJson);
            redisTemplate.expire(EXCLUDED_USERS_CACHE_NAME + myId, 24, TimeUnit.HOURS);
        }
    }

    public void evictExcludedUserIds(Long myId) {
        redisTemplate.delete(EXCLUDED_USERS_CACHE_NAME + myId);
    }

    @SneakyThrows
    public void putUserInCache(Long userId, UserEntity user) {
        String cachedUserJson = objectMapper.writeValueAsString(user);
        redisTemplate.opsForValue().set(USER_CACHE_NAME + userId, cachedUserJson);
        redisTemplate.expire(USER_CACHE_NAME + userId, 1, TimeUnit.HOURS);
    }

    public Optional<UserEntity> getUserFromCache(Long userId) {
        try {
            String cachedUserJson = redisTemplate.opsForValue().get(USER_CACHE_NAME + userId);
            if (cachedUserJson != null) {
                return Optional.ofNullable(objectMapper.readValue(cachedUserJson, UserEntity.class));
            }
        } catch (Exception e) {
            redisTemplate.delete(USER_CACHE_NAME + userId);
            return Optional.empty();
        }
        return Optional.empty();
    }

    @SneakyThrows
    public void putUserDetailsInCache(Long userId, UserDetailsEntity userDetails) {
        String cachedUserJson = objectMapper.writeValueAsString(userDetails);
        redisTemplate.opsForValue().set(USER_DETAILS_CACHE_NAME + userId, cachedUserJson);
        redisTemplate.expire(USER_DETAILS_CACHE_NAME + userId, 1, TimeUnit.HOURS);
    }

    public Optional<UserDetailsEntity> getUserDetailsFromCache(Long userId) {
        try {
            String cachedUserJson = redisTemplate.opsForValue().get(USER_DETAILS_CACHE_NAME + userId);
            if (cachedUserJson != null) {
                return Optional.ofNullable(objectMapper.readValue(cachedUserJson, UserDetailsEntity.class));
            }
        } catch (Exception e) {
            redisTemplate.delete(USER_DETAILS_CACHE_NAME + userId);
            return Optional.empty();
        }
        return Optional.empty();
    }

    public void evictAllUserCache(Long userId) {
        evictState(userId);
        evictCachedUser(userId);
        evictComplaintUser(userId);
        evictUserAssessmentId(userId);
    }

    public void evictAllUserCacheWithoutState(Long userId) {
        evictCachedUser(userId);
        evictComplaintUser(userId);
        evictUserAssessmentId(userId);
    }
}