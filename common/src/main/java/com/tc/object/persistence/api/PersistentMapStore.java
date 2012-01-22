/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.persistence.api;

public interface PersistentMapStore {
  
  public String get(String key);
  
  public boolean remove(String key);
  
  public void put(String key, String value);

}
