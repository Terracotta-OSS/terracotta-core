/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
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
