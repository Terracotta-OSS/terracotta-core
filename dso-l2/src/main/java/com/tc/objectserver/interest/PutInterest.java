/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.InterestType;

/**
 * @author Eugene Shelestovich
 */
public class PutInterest extends CacheInterest {
  private final Object key;
  private final byte[] value;

  public PutInterest(final Object key, final byte[] value, final String cacheName) {
    super(cacheName);
    this.key = key;
    this.value = value;
  }

  @Override
  public InterestType getType() {
    return InterestType.PUT;
  }

  @Override
  public Object getKey() {
    return key;
  }

  public byte[] getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "PutInterest{" +
           "key=" + key +
           ", value=" + value +
           ", cacheName=" + cacheName +
           '}';
  }
}
