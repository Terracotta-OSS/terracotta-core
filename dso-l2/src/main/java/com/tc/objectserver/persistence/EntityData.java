/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import java.io.Serializable;

/**
 * The data required to persist an entity in persistent storage.  It is broken down into key and value components to allow
 * for bulk retrieval of all entity data (this is why key data may be redundantly stored in the value) as well as quick
 * simple lookup in a key-value mapping.
 */
public class EntityData {
  private EntityData() {}
  
  public static class Key implements Serializable {
    public String className;
    public String entityName;
    
    @Override
    public int hashCode() {
      return this.className.hashCode() ^ this.entityName.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
      boolean isEqual = false;
      if (obj instanceof EntityData.Key) {
        EntityData.Key other = (EntityData.Key)obj;
        isEqual = this.className.equals(other.className) && this.entityName.equals(other.entityName); 
      }
      return isEqual;
    }
  }
  
  public static class Value implements Serializable {
    public String className;
    public long version;
    public long consumerID;
    public String entityName;
    public byte[] configuration;
  }
}
