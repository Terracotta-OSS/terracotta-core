/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.clientgroup;

import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageFactoryImpl;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.object.session.SessionProvider;

public class ClientGroupCommunicationsManagerImpl  extends CommunicationsManagerImpl implements ClientGroupCommunicationsManager {
  private final MessageMonitor                   monitor;

  public ClientGroupCommunicationsManagerImpl(MessageMonitor monitor, NetworkStackHarnessFactory stackHarnessFactory,
                                              ConnectionPolicy connectionPolicy) {
    super(monitor, stackHarnessFactory, connectionPolicy);
    this.monitor = monitor;
  }


  public ClientGroupCommunicationsManagerImpl(MessageMonitor monitor, NetworkStackHarnessFactory stackHarnessFactory,
                                             TCConnectionManager connMgr, ConnectionPolicy connectionPolicy,
                                             int workerCommCount, HealthCheckerConfig healthCheckerConfig) {
    super(monitor, stackHarnessFactory, connMgr, connectionPolicy, workerCommCount, healthCheckerConfig);
    this.monitor = monitor;
  }

  public ClientGroupMessageChannel createClientGroupChannel(final SessionProvider sessionProvider,
                                                            final int maxReconnectTries, final int timeout,
                                                            ConnectionAddressProvider[] addressProviders) {
    
    ClientGroupMessageChannel clientGroup = new ClientGroupMessageChannelImpl(new TCMessageFactoryImpl(sessionProvider,
                                                                                                     monitor),
                                                                            sessionProvider, maxReconnectTries, this,
                                                                            addressProviders);
    return (clientGroup);
  }

}
