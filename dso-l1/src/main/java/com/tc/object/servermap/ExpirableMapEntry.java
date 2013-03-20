package com.tc.object.servermap;

/**
 * @author tim
 */
public interface ExpirableMapEntry {
  long getLastAccessedTime();
  void setLastAccessedTime(long lastAccessedTime);

  long getCreationTime();
  void setCreationTime(long creationTime);

  long getTimeToIdle();
  void setTimeToIdle(long timeToIdle);

  long getTimeToLive();
  void setTimeToLive(long timeToLive);
}
