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
package com.tc.net.protocol.delivery;

import com.tc.properties.ReconnectConfig;

import java.util.Timer;

/**
 * Creates new instances of OnceAndOnlyOnceProtocolNetworkLayers. This is used so that a mock one may be injected into
 * the once and only once network stack harness for testing.
 */
public class OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl implements OnceAndOnlyOnceProtocolNetworkLayerFactory {

  public static final String RESTORE_TIMERTHREAD_NAME = "OOO Connection Restore Timer";
  private Timer              restoreConnectTimer      = null;

  @Override
  public synchronized OnceAndOnlyOnceProtocolNetworkLayer createNewClientInstance(ReconnectConfig reconnectConfig) {
    OOOProtocolMessageFactory messageFactory = new OOOProtocolMessageFactory();
    OOOProtocolMessageParser messageParser = new OOOProtocolMessageParser(messageFactory);
    return new OnceAndOnlyOnceProtocolNetworkLayerImpl(messageFactory, messageParser, reconnectConfig, true);
  }

  @Override
  public synchronized OnceAndOnlyOnceProtocolNetworkLayer createNewServerInstance(ReconnectConfig reconnectConfig) {
    // ooo connection restore timers are needed only for servers
    if (restoreConnectTimer == null) {
      restoreConnectTimer = new Timer(RESTORE_TIMERTHREAD_NAME, true);
    }

    OOOProtocolMessageFactory messageFactory = new OOOProtocolMessageFactory();
    OOOProtocolMessageParser messageParser = new OOOProtocolMessageParser(messageFactory);
    return new OnceAndOnlyOnceProtocolNetworkLayerImpl(messageFactory, messageParser, reconnectConfig, false,
                                                       restoreConnectTimer);
  }
}
