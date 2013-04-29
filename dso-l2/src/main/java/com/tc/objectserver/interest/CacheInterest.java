package com.tc.objectserver.interest;

/**
 * Base class for all typed events.
 *
 * @author Eugene Shelestovich
 */
public abstract class CacheInterest implements Interest {

  protected final String cacheName;

  protected CacheInterest(final String cacheName) {
    this.cacheName = cacheName;
  }

  @Override
  public String getCacheName() {
    return cacheName;
  }

}
