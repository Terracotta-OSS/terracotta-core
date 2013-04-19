package com.tc.object;

import com.tc.net.GroupID;

import java.util.Set;

/**
 * @author Eugene Shelestovich
 */
public interface ServerInterestListenerManager {
  void registerL1CacheListener(InterestDestination destination, Set<InterestType> listenTo);
}
