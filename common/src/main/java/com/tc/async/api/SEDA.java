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
 */
package com.tc.async.api;

import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.util.concurrent.QueueFactory;

/**
 * Manages the startup and shutdown of a SEDA environment
 * 
 * @author steve
 */
public class SEDA {
  private final StageManager  stageManager;
  private final TCThreadGroup threadGroup;

  public SEDA(TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.stageManager = new StageManagerImpl(threadGroup, new QueueFactory(), new StageListener() {
      @Override
      public void stageStalled(String name, long delay, int queueDepth) {
        stageWarning(new StallWarning(name, delay, queueDepth));
      }
    });
  }

  public StageManager getStageManager() {
    return stageManager;
  }

  protected TCThreadGroup getThreadGroup() {
    return this.threadGroup;
  }
  
  public void stageWarning(Object description) {
    
  }
  
  private static class StallWarning {
    private final String name;
    private final long delay;
    private final int depth;

    public StallWarning(String name, long delay, int depth) {
      this.name = name;
      this.delay = delay;
      this.depth = depth;
    }

    @Override
    public String toString() {
      return "StallWarning{" + "name=" + name + ", delay=" + delay + ", depth=" + depth + '}';
    }
  }
}
