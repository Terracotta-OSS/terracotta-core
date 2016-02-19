/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.persistence;

import com.tc.object.EntityID;
import com.tc.util.Assert;

import java.util.Collection;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


/**
 * Stores the information relating to the entities currently alive on the platform into persistent storage.
 */
public class EntityPersistor {
  private static final String ENTITIES_ALIVE = "entities_alive";
  private static final String COUNTERS = "counters";
  private static final String COUNTERS_CONSUMER_ID = "counters:consumerID";

  private final KeyValueStorage<EntityData.Key, EntityData.Value> entities;
  private final KeyValueStorage<String, Long> counters;

  public EntityPersistor(IPersistentStorage storageManager) {
    this.entities = storageManager.getKeyValueStorage(ENTITIES_ALIVE, EntityData.Key.class, EntityData.Value.class);
    this.counters = storageManager.getKeyValueStorage(COUNTERS, String.class, Long.class);
    // Make sure that the consumerID is initialized to 1 (0 reserved for platform).
    if (!this.counters.containsKey(COUNTERS_CONSUMER_ID)) {
      this.counters.put(COUNTERS_CONSUMER_ID, new Long(1));
    }
  }

  public void clear() {
    entities.clear();
    counters.clear();
    if (!this.counters.containsKey(COUNTERS_CONSUMER_ID)) {
      this.counters.put(COUNTERS_CONSUMER_ID, new Long(1));
    }
  }

  @SuppressWarnings("deprecation")
  public Collection<EntityData.Value> loadEntityData() {
    return this.entities.values();
  }

  public boolean containsEntity(EntityID id) {
    EntityData.Key key = new EntityData.Key();
    key.className = id.getClassName();
    key.entityName = id.getEntityName();
    return this.entities.containsKey(key);
  }

  public void entityCreated(EntityID id, long version, long consumerID, byte[] configuration) {
    String className = id.getClassName();
    String entityName = id.getEntityName();
    
    EntityData.Key key = new EntityData.Key();
    EntityData.Value value = new EntityData.Value();
    key.className = className;
    key.entityName = entityName;
    value.className = className;
    value.version = version;
    value.consumerID = consumerID;
    value.entityName = entityName;
    value.configuration = configuration;
    this.entities.put(key, value);
  }
  
  public void reconfigureEntity(EntityID id, long version, byte[] configuration) {
    String className = id.getClassName();
    String entityName = id.getEntityName();

    EntityData.Key key = new EntityData.Key();
    key.className = className;
    key.entityName = entityName;
    
    EntityData.Value val = this.entities.get(key);
    val.configuration = configuration;
    Assert.assertEquals(version, val.version);
    
    this.entities.put(key, val);
  }
  
  public void entityDeleted(EntityID id) {
    EntityData.Key key = new EntityData.Key();
    key.className = id.getClassName();
    key.entityName = id.getEntityName();
    Assert.assertTrue(this.entities.containsKey(key));
    this.entities.remove(key);
  }

  public long getNextConsumerID() {
    long consumerID = this.counters.get(COUNTERS_CONSUMER_ID);
    this.counters.put(COUNTERS_CONSUMER_ID, new Long(consumerID + 1));
    return consumerID;
  }
}
