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
package com.tc.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class DaemonThreadFactory implements ThreadFactory {
  private final AtomicInteger count = new AtomicInteger();
  private final String name;

  public DaemonThreadFactory(String name) {
    this.name = name;
  }
  
  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r, name + count.incrementAndGet());
    t.setDaemon(true);
    return t;
  }

}
