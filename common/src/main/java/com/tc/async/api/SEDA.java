/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.async.api;

import com.tc.async.impl.StageManagerImpl;
import com.tc.bytes.TCByteBufferFactory;
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

  public SEDA(final TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.stageManager = new StageManagerImpl(threadGroup, new QueueFactory());
    TCByteBufferFactory.registerThreadGroup(threadGroup);
  }

  public StageManager getStageManager() {
    return stageManager;
  }

  protected TCThreadGroup getThreadGroup() {
    return this.threadGroup;
  }
}
