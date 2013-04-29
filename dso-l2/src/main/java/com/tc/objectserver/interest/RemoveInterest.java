/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.InterestType;

/**
 * @author Eugene Shelestovich
 */
public class RemoveInterest extends CacheInterest {
  private final Object key;

  public RemoveInterest(final Object key, final String cacheName) {
    super(cacheName);
    this.key = key;
  }

  @Override
  public InterestType getType() {
    return InterestType.REMOVE;
  }

  @Override
  public Object getKey() {
    return key;
  }

  @Override
  public String toString() {
    return "RemoveInterest{" +
           "key=" + key +
           ", cacheName=" + cacheName +
           '}';
  }
}
