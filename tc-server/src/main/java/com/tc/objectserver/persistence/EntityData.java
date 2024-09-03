/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.persistence;

import java.io.Serializable;



/**
 * The data required to persist an entity in persistent storage.  It is broken down into key and value components to allow
 * for bulk retrieval of all entity data (this is why key data may be redundantly stored in the value) as well as quick
 * simple lookup in a key-value mapping.
 */
public class EntityData {
  /**
   * Empty constructor required due to the inner classes being Serializable.
   */
  private EntityData() {}

  public static class Key implements Serializable {
    private static final long serialVersionUID = 1L;
    
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
    private static final long serialVersionUID = 1L;
    
    public String className;
    public long version;
    public long consumerID;
    public boolean canDelete;
    public String entityName;
    public byte[] configuration;
  }

  public static enum Operation implements Serializable {
    CREATE,
    DESTROY,
    RECONFIGURE,
  }

  public static class JournalEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // This data is meant to be written into a transaction journal so we need to record everything related to the activity.
    public Operation operation;
    public long transactionID;
    // reconfigureResponse is only used to store the result of RECONFIGURE.
    public byte[] reconfigureResponse;
    // The exception in CREATE/DESTROY is saved here, null on success.
    public Exception failure;

    @Override
    public String toString() {
      return "JournalEntry{" + "operation=" + operation + ", transactionID=" + transactionID + '}';
    }
  }
}
