package com.tc.object;

import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.msg.ServerEventMessage;

import java.util.Set;

/**
 * Manages subscriptions and delivery of server events for L1 clients.
 *
 * @author Eugene Shelestovich
 */
public interface ServerEventListenerManager extends ClientHandshakeCallback {

  void registerListener(ServerEventDestination destination, Set<ServerEventType> listenTo);

  void unregisterListener(ServerEventDestination destination);

  void dispatch(ServerEventMessage message);
}
