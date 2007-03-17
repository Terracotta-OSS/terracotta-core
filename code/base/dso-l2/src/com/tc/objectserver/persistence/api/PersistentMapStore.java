/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

public interface PersistentMapStore {
  
  public String get(String key);
  
  public boolean remove(String key);
  
  public void put(String key, String value);

}
