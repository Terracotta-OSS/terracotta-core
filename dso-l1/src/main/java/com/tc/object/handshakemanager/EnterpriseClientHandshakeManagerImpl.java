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
 *
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
