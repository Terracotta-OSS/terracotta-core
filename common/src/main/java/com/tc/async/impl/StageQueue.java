package com.tc.async.impl;

import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.async.api.SpecializedEventContext;
import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.util.concurrent.QueueFactory;

/**
 * Created by cschanck on 5/23/2017.
 */
public interface StageQueue<EC> extends Sink<EC> {

  StageQueueFactory FACTORY = new StageQueueFactory();

  Source<ContextWrapper<EC>> getSource(int index);

  @Override
  void setClosed(boolean closed);

  @Override
  void addSingleThreaded(EC context);

  @Override
  void addMultiThreaded(EC context);

  @Override
  void addSpecialized(SpecializedEventContext specialized);

  // Used for testing
  @Override
  int size();

  @Override
  String toString();

  @Override
  void clear();

  /*********************************************************************************************************************
   * Monitorable Interface
   * @param enable
   */

  @Override
  void enableStatsCollection(boolean enable);

  @Override
  Stats getStats(long frequency);

  @Override
  Stats getStatsAndReset(long frequency);

  @Override
  boolean isStatsCollectionEnabled();

  @Override
  void resetStats();

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
                                            QueueFactory<ContextWrapper<C>> queueFactory,
                                            TCLoggerProvider loggerProvider,
                                            String stageName,
                                            int queueSize) {
      if (queueCount == 1) {
        return new SingletonStageQueueImpl<C>(queueFactory, loggerProvider, stageName, queueSize);
      } else {
        return new MultiStageQueueImpl<C>(queueCount, queueFactory, loggerProvider, stageName, queueSize);
      }
    }
  }
}
