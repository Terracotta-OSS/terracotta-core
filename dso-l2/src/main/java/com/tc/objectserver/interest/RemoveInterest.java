/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

/**
 * @author Eugene Shelestovich
 */
public class RemoveInterest implements Interest {
  private final Object key;

  public RemoveInterest(final Object key) {
    this.key = key;
  }

  public Object getKey() {
    return key;
  }

  @Override
  public String toString() {
    return "RemoveInterest{" +
           "key=" + key +
           '}';
  }
}
