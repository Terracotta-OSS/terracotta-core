/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

/**
 * @author Eugene Shelestovich
 */
public class PutInterest implements Interest {
  private final Object key;
  private final byte[] value;

  public PutInterest(final Object key, final byte[] value) {
    this.key = key;
    this.value = value;
  }

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
           '}';
  }
}
