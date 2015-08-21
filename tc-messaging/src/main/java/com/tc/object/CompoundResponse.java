/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;


/**
 *
 * @author mscott
 */
public class CompoundResponse {
    private final long creationTime;
    private final long lastAccessedTime;
    private final long timeToIdle;
    private final long timeToLive;
    private final long version;
    private Object data;

    public CompoundResponse(Object data, long creationTime, long lastAccessedTime,
                            long timeToIdle, long timeToLive, long version) {
      this.creationTime = creationTime;
      this.lastAccessedTime = lastAccessedTime;
      this.timeToIdle = timeToIdle;
      this.timeToLive = timeToLive;
      this.data = data;
      this.version = version;
    }
    
    public long getVersion() {
      return this.version;
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

    public Object getData() {
      return data;
    }
    
    public void setData(Object value) {
      data = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final CompoundResponse that = (CompoundResponse)o;

      if (creationTime != that.creationTime) return false;
      if (lastAccessedTime != that.lastAccessedTime) return false;
      if (timeToIdle != that.timeToIdle) return false;
      if (timeToLive != that.timeToLive) return false;
      if (!data.equals(that.data)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = (int)(creationTime ^ (creationTime >>> 32));
      result = 31 * result + (int)(lastAccessedTime ^ (lastAccessedTime >>> 32));
      result = 31 * result + (int)(timeToIdle ^ (timeToIdle >>> 32));
      result = 31 * result + (int)(timeToLive ^ (timeToLive >>> 32));
      result = 31 * result + data.hashCode();
      return result;
    }
}
