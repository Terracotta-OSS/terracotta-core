/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

public interface PersistentSequence {

  // This provide a unique id for this instance of the sequence. (ideally unique for every sequence starting at 0)
  public String getUID();

  public long next();

  public long nextBatch(int batchSize);

}