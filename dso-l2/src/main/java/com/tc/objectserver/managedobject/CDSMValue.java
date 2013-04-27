package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author tim
 */
public class CDSMValue implements EvictableEntry {
  private final ObjectID objectID;
  private final long creationTime;
  private final long timeToIdle;
  private final long timeToLive;

  private long lastAccessedTime;

  public CDSMValue(final ObjectID objectID, final long creationTime, final long lastAccessedTime, final long timeToIdle, final long timeToLive) {
    checkArgument(lastAccessedTime >= creationTime);
    this.objectID = objectID;
    this.creationTime = creationTime;
    this.lastAccessedTime = lastAccessedTime;
    this.timeToIdle = timeToIdle;
    this.timeToLive = timeToLive;
  }

  @Override
  public ObjectID getObjectID() {
    return objectID;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public long getLastAccessedTime() {
    return lastAccessedTime;
  }

  public long getTimeToIdle() {
    return timeToIdle;
  }

  public long getTimeToLive() {
    return timeToLive;
  }

  public void setLastAccessedTime(final long lastAccessedTime) {
    checkArgument(lastAccessedTime >= creationTime);
    this.lastAccessedTime = lastAccessedTime;
  }

  @Override
  public long expiresIn(final long now, final long ttiSeconds, final long ttlSeconds) {
    return computeExpiresIn(now, timeToIdle != 0 ? timeToIdle : ttiSeconds, timeToLive != 0 ? timeToLive : ttlSeconds);
  }

  protected long computeExpiresIn(long now, long ttiSeconds, long ttlSeconds) {
    if (ttiSeconds <= 0 && ttlSeconds <= 0) {
      // No expiration.
      return Long.MAX_VALUE;
    }
    long expiresAtTTI = ttiSeconds <= 0 ? Long.MAX_VALUE : lastAccessedTime + ttiSeconds;
    long expiresAtTTL = ttlSeconds <= 0 ? Long.MAX_VALUE : creationTime + ttlSeconds;
    return Math.min(expiresAtTTI, expiresAtTTL) - now;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final CDSMValue cdsmValue = (CDSMValue)o;

    if (creationTime != cdsmValue.creationTime) return false;
    if (lastAccessedTime != cdsmValue.lastAccessedTime) return false;
    if (timeToIdle != cdsmValue.timeToIdle) return false;
    if (timeToLive != cdsmValue.timeToLive) return false;
    if (!objectID.equals(cdsmValue.objectID)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = objectID.hashCode();
    result = 31 * result + (int)(creationTime ^ (creationTime >>> 32));
    result = 31 * result + (int)(timeToIdle ^ (timeToIdle >>> 32));
    result = 31 * result + (int)(timeToLive ^ (timeToLive >>> 32));
    result = 31 * result + (int)(lastAccessedTime ^ (lastAccessedTime >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "CDSMValue{" + "objectID=" + objectID + ", creationTime=" + creationTime + ", timeToIdle=" + timeToIdle + ", timeToLive=" + timeToLive + ", lastAccessedTime=" + lastAccessedTime + '}';
  }
}
