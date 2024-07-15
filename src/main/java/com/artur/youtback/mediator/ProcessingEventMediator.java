package com.artur.youtback.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

//@Component
public class ProcessingEventMediator {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingEventMediator.class);

    private boolean videoEventResult = false;
    private boolean thumbnailEventResult = false;
    private boolean userPictureEventResult = false;

    private final Map<String, CyclicBarrier> videoCyclicBarrier = new ConcurrentHashMap<>();
    private final Map<String, CyclicBarrier> thumbnailCyclicBarrier = new ConcurrentHashMap<>();
    private final Map<String, CyclicBarrier> userPictureCyclicBarrier = new ConcurrentHashMap<>();


    public boolean videoProcessingWait(String id) throws BrokenBarrierException, InterruptedException, TimeoutException {
        processingWait(id, videoCyclicBarrier);
        return videoEventResult;
    }

    public void videoProcessingResultNotice(String id, boolean value) throws BrokenBarrierException, InterruptedException, TimeoutException {
        videoEventResult = value;
       processingNotice(id, videoCyclicBarrier);
    }

    public boolean thumbnailProcessingWait(String id) throws BrokenBarrierException, InterruptedException, TimeoutException {
        processingWait(id, thumbnailCyclicBarrier);
        return thumbnailEventResult;
    }

    public void thumbnailProcessingResultNotice(String id, boolean value) throws BrokenBarrierException, InterruptedException, TimeoutException {
        thumbnailEventResult = value;
        processingNotice(id, thumbnailCyclicBarrier);
    }

    public boolean userPictureProcessingWait(String id) throws BrokenBarrierException, InterruptedException, TimeoutException {
        processingWait(id, userPictureCyclicBarrier);
        return userPictureEventResult;
    }

    public void userPictureProcessingResultNotice(String id, boolean value) throws BrokenBarrierException, InterruptedException, TimeoutException {
        userPictureEventResult = value;
        processingNotice(id, userPictureCyclicBarrier);
    }

    private void processingWait(String id, Map<String, CyclicBarrier> map) throws BrokenBarrierException, InterruptedException, TimeoutException {
        if(!map.containsKey(id)){
            //if not processed yet we need to wait
            map.put(id, new CyclicBarrier(2));
            map.get(id).await(3, TimeUnit.MINUTES);
        } else {
            //if already processed removing this id
            map.remove(id);
        }
    }

    private void processingNotice(String id, Map<String, CyclicBarrier> map) throws BrokenBarrierException, InterruptedException, TimeoutException {
        if(map.containsKey(id)){
            //if waiting, need to notify about completion
            map.get(id).await(3, TimeUnit.MINUTES);
            map.remove(id);
        } else{
            //if not waiting yet, need to notify that it is already processed. Guaranteed that this entry will be deleted
            map.put(id, new CyclicBarrier(2));
        }
    }
}
