/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.object.context.PauseContext;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tcclient.cluster.ClusterInternalEventsGun;

import java.util.Collection;


public class EnterpriseClientHandshakeManagerImpl extends ClientHandshakeManagerImpl {

  public EnterpriseClientHandshakeManagerImpl(TCLogger logger, ClientHandshakeMessageFactory chmf, Sink<PauseContext> pauseSink,
                                              SessionManager sessionManager, ClusterInternalEventsGun cClusterEventsGun,
                                              String clientVersion, Collection<ClientHandshakeCallback> callbacks) {
    super(logger, chmf, sessionManager, cClusterEventsGun, clientVersion, callbacks);
  }

  @Override
  protected boolean isEnterpriseClient() {
    return true;
  }
}
