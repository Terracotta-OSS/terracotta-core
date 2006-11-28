/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io.serializer.api;

public interface StringIndex {
  public long getOrCreateIndexFor(String string);
  public String getStringFor(long index);
}
