/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;

import java.util.Set;

public class NullMessageRecycler<K> implements MessageRecycler<K> {

  @Override
  public void addMessage(Recyclable message, Set<K> keys) {
    return;
  }

  @Override
  public boolean recycle(K key) {
    return false;
  }

}
