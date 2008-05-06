/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.api;

public interface StringIndex {
  public long getOrCreateIndexFor(String string);
  public String getStringFor(long index);
}
