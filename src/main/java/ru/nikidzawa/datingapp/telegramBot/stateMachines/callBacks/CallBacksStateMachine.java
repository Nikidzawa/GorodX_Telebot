package ru.nikidzawa.datingapp.telegramBot.stateMachines.callBacks;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.commands.CommandStateMachine;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;

import java.util.HashMap;
import java.util.List;

@Component
public class CallBacksStateMachine {

    private final HashMap<String, CallBack> callBacks;

    @Setter
    private BotFunctions botFunctions;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CommandStateMachine commandStateMachine;

    @Autowired
    private Messages messages;

    @Autowired
    private DataBaseService dataBaseService;

    public CallBacksStateMachine () {
        callBacks = new HashMap<>();
        callBacks.put("complaint", new Complaint());
        callBacks.put("block", new Block());
        callBacks.put("peace", new Peace());
    }

    public void handleCallback (String command, Long myId, Long anotherUserId) {
        CallBack callBack = callBacks.get(command);
        if (callBack != null) {
            callBack.handleCallback(myId, anotherUserId);
        }
    }

    private class Complaint implements CallBack {

        @Override
        public void handleCallback(Long myId, Long anotherUserId) {
            botFunctions.sendMessageAndKeyboard(myId, messages.getSEND_COMPLAINT(), botFunctions.cancelButton());
            cacheService.putComplaintUser(myId, anotherUserId);
            cacheService.setState(myId, StateEnum.CALL_BACK_QUERY_COMPLAIN);
        }
    }
    private class Block implements CallBack {

        @Override
        public void handleCallback(Long myId, Long anotherUserId) {
            botFunctions.sendMessageAndRemoveKeyboard(myId, messages.getBLOCK());
            UserEntity complaintUser = dataBaseService.getUserById(anotherUserId).get();
            List<ComplainEntity> complainEntities = dataBaseService.findByComplaintUser(complaintUser);
            complaintUser.setComplaints(null);
            complaintUser.setBanned(true);
            dataBaseService.saveUser(complaintUser);
            dataBaseService.deleteAllComplainEntities(complainEntities);
            commandStateMachine.getComplaint(myId);
        }
    }
    private class Peace implements CallBack {

        @Override
        public void handleCallback(Long myId, Long anotherUserId) {
            botFunctions.sendMessageAndRemoveKeyboard(myId, messages.getPEACE());
            UserEntity complaintUser = dataBaseService.getUserById(anotherUserId).get();
            List<ComplainEntity> complainEntities = dataBaseService.findByComplaintUser(complaintUser);
            complaintUser.setComplaints(null);
            complaintUser.setBanned(false);
            dataBaseService.saveUser(complaintUser);
            dataBaseService.deleteAllComplainEntities(complainEntities);
            commandStateMachine.getComplaint(myId);
        }
    }
}
