/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  public synchronized OnceAndOnlyOnceProtocolNetworkLayer createNewClientInstance(ReconnectConfig reconnectConfig) {
    OOOProtocolMessageFactory messageFactory = new OOOProtocolMessageFactory();
    OOOProtocolMessageParser messageParser = new OOOProtocolMessageParser(messageFactory);
    return new OnceAndOnlyOnceProtocolNetworkLayerImpl(messageFactory, messageParser, reconnectConfig, true);
  }

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
