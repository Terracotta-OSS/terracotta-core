/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.memorydatastore.client;

import java.util.Collection;

public interface MemoryDataMap {
  void put(byte[] key, byte[] value);
  
  byte[] get(byte[] key);
  
  Collection getAll(byte[] key);
  
  void remove(byte[] key);
  
  void removeAll(byte[] key);
}
