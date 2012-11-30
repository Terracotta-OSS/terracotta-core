package com.tc.object;

import com.tc.net.GroupID;

/**
 * @author tim
 */
public interface RemoteResourceManager {
  void handleThrottleMessage(GroupID groupID, boolean exception, float throttle);

  void throttleIfMutationIfNecessary(ObjectID parentObject);
}
