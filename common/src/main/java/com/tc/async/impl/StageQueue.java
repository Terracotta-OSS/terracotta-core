package com.tc.async.impl;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.concurrent.QueueFactory;
import java.util.Map;

/**
 * Created by cschanck on 5/23/2017.
 */
public interface StageQueue<EC> extends Sink<EC> {

  StageQueueFactory FACTORY = new StageQueueFactory();

  Source getSource(int index);

  void close();

  @Override
  String toString();

  int clear();
  
  Map<String, ?> getState();
  
  void enableAdditionalStatistics(boolean track);

  class StageQueueFactory {
    /**
     * The StageQueue factory.
     *
     * @param queueCount : Number of queues working on this stage
     * @param queueFactory : Factory used to create the queues
     * @param loggerProvider : logger
     * @param stageName : The stage name
     * @param queueSize : Max queue Size allowed
     */
    public static <C> StageQueue<C> factory(int queueCount,
                                            QueueFactory queueFactory,
                                            Class<C> type, 
                                            EventCreator<C> creator,
                                            TCLoggerProvider loggerProvider,
                                            String stageName,
                                            int queueSize) {
      if (!MultiThreadedEventContext.class.isAssignableFrom(type)) {
        return new SingletonStageQueueImpl(queueFactory, type, creator, loggerProvider, stageName, queueSize);
      } else {
        return new MultiStageQueueImpl(queueCount, queueFactory, type, creator, loggerProvider, stageName, queueSize);
      }
    }
  }
}
