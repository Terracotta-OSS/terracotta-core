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
package com.tc.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.tc.async.impl.Event;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class QueueFactory {
  public <E> BlockingQueue<Event> createInstance(Class<E> type, int capacity) {
    return (capacity == 0) ? new SynchronousQueue<>() : 
            (capacity == Integer.MAX_VALUE || capacity < 0) ? new LinkedBlockingQueue<>() :
            (capacity <= 1024) ? new ArrayBlockingQueue<>(capacity) :
            new LinkedBlockingQueue<>(capacity);
  }
}
