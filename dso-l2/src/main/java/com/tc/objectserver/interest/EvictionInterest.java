/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.InterestType;

/**
 * @author Eugene Shelestovich
 */
public class EvictionInterest extends CacheInterest {
  private final Object key;

  public EvictionInterest(final Object key, final String cacheName) {
    super(cacheName);
    this.key = key;
  }

  @Override
  public InterestType getType() {
    return InterestType.EVICT;
  }

  @Override
  public Object getKey() {
    return key;
  }

  @Override
  public String toString() {
    return "EvictionInterest{" +
           "key=" + key +
           ", cacheName='" + cacheName + '\'' +
           '}';
  }
}
