package com.tc.object;

import com.tc.object.msg.ServerInterestMessage;

import java.util.Set;

/**
 * @author Eugene Shelestovich
 */
public interface ServerInterestListenerManager {

  void registerL1CacheListener(InterestDestination destination, Set<InterestType> listenTo);

  void unregisterL1CacheListener(InterestDestination destination);

  void dispatchInterest(ServerInterestMessage message);
}
