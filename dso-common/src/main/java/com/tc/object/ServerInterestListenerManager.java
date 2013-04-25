package com.tc.object;

import com.tc.object.msg.ServerInterestMessage;

import java.util.Set;

/**
 * @author Eugene Shelestovich
 */
public interface ServerInterestListenerManager {

  void registerInterestListener(InterestDestination destination, Set<InterestType> listenTo);

  void unregisterInterestListener(InterestDestination destination);

  void dispatchInterest(ServerInterestMessage message);
}
