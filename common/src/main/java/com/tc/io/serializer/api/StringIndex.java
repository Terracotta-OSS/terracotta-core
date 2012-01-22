/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.api;

public interface StringIndex {
  public long getOrCreateIndexFor(String string);
  public String getStringFor(long index);
}
