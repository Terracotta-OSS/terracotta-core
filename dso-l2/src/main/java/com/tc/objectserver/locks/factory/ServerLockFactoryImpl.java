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
package com.tc.objectserver.locks.factory;

import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.ServerLock;
import com.tc.objectserver.locks.LockFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ServerLockFactoryImpl implements LockFactory {
  private final static boolean GREEDY_LOCKS_ENABLED = TCPropertiesImpl
                                                     .getProperties()
                                                     .getBoolean(TCPropertiesConsts.L2_LOCKMANAGER_GREEDY_LOCKS_ENABLED);
  private final LockFactory    factory;

  public ServerLockFactoryImpl() {
    if (GREEDY_LOCKS_ENABLED) {
      factory = new GreedyPolicyFactory();
    } else {
      factory = new NonGreedyLockPolicyFactory();
    }
  }

  @Override
  public ServerLock createLock(LockID lid) {
    return factory.createLock(lid);
  }
}
