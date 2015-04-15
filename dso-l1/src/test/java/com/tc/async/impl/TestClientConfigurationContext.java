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
package com.tc.async.impl;

import com.tc.exception.ImplementMe;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.RemoteObjectManager;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.tx.ClientTransactionManager;

public class TestClientConfigurationContext extends ClientConfigurationContext {
  public ClientLockManager clientLockManager;

  public TestClientConfigurationContext() {
    super(null, null, null, null, null, null, null, null);
  }

  @Override
  public ClientLockManager getLockManager() {
    return clientLockManager;
  }

  @Override
  public TCLogger getLogger(final Class clazz) {
    return new NullTCLogger();
  }

  @Override
  public RemoteObjectManager getObjectManager() {
    throw new ImplementMe();
  }

  @Override
  public ClientTransactionManager getTransactionManager() {
    throw new ImplementMe();
  }

  @Override
  public ClientHandshakeManager getClientHandshakeManager() {
    throw new ImplementMe();
  }

}
