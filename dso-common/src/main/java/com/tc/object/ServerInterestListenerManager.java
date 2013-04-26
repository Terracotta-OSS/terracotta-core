package com.tc.object;

import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.msg.ServerInterestMessage;

import java.util.Set;

/**
 * Manages subscriptions and delivery of server events for L1 clients.
 *
 * @author Eugene Shelestovich
 */
public interface ServerInterestListenerManager extends ClientHandshakeCallback {

  void registerInterestListener(InterestDestination destination, Set<InterestType> listenTo);

  void unregisterInterestListener(InterestDestination destination);

  void dispatchInterest(ServerInterestMessage message);
}
