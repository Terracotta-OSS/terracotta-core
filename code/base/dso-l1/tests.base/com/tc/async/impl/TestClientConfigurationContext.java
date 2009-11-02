/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
    super(null, null, null, null, null, null);
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
