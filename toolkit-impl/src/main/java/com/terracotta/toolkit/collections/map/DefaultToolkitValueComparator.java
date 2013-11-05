/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;

public class DefaultToolkitValueComparator<V> implements ToolkitValueComparator<V> {

  public static DefaultToolkitValueComparator INSTANCE = new DefaultToolkitValueComparator();

  @Override
  public boolean equals(V v1, V v2) {
    return v1.equals(v2);
  }


}
