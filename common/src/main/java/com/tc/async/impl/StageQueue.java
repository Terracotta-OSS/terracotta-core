/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
