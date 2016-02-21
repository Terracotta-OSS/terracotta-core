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

import com.tc.net.ClientID;
import com.tc.object.EntityID;
import com.tc.util.Assert;

import java.util.Collection;

import org.terracotta.exception.EntityException;
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

  public boolean containsEntity(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id) {
    EntityData.Key key = new EntityData.Key();
    key.className = id.getClassName();
    key.entityName = id.getEntityName();
    return this.entities.containsKey(key);
  }

  /**
   * Consults the journal to see if there already was an entity created with this transactionID from the referenced clientID.
   * If an attempt was made, true is returned (on success) or EntityException is thrown (if it was a failure).
   * False is returned if this clientID and transactionID seem new.
   */
  public boolean wasEntityCreatedInJournal(ClientID clientID, long transactionID) throws EntityException {
    // Currently returns false - this is just a placeholder to get the API in place.
    return false;
  }

  public void entityCreateFailed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityException error) {
    // Currently does nothing - this is just a placeholder to get the API in place.
  }

  public void entityCreated(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id, long version, long consumerID, byte[] configuration) {
    addNewEntityToMap(id, version, consumerID, configuration);
  }

  public void entityCreatedNoJournal(EntityID id, long version, long consumerID, byte[] configuration) {
    addNewEntityToMap(id, version, consumerID, configuration);
    // (Note that we don't store this into the journal - this is used for passive sync).
  }

  /**
   * Consults the journal to see if there already was an entity destroyed with this transactionID from the referenced clientID.
   * If an attempt was made, true is returned (on success) or EntityAlreadyExistsException is thrown.
   * False is returned if this clientID and transactionID seem new.
   */
  public boolean wasEntityDestroyedInJournal(ClientID clientID, long transactionID) throws EntityException {
    // Currently returns false - this is just a placeholder to get the API in place.
    return false;
  }

  public void entityDestroyFailed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityException error) {
    // Currently does nothing - this is just a placeholder to get the API in place.
  }

  public void entityDestroyed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id) {
    EntityData.Key key = new EntityData.Key();
    key.className = id.getClassName();
    key.entityName = id.getEntityName();
    Assert.assertTrue(this.entities.containsKey(key));
    this.entities.remove(key);
  }

  public byte[] reconfiguredResultInJournal(ClientID clientID, long transactionID) {
    // Currently returns null - this is just a placeholder to get the API in place.
    return null;
  }

  public void entityReconfigureFailed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityException error) {
    // Currently does nothing - this is just a placeholder to get the API in place.
  }

  public void entityReconfigureSucceeded(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id, long version, byte[] configuration) {
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

  public long getNextConsumerID() {
    long consumerID = this.counters.get(COUNTERS_CONSUMER_ID);
    this.counters.put(COUNTERS_CONSUMER_ID, new Long(consumerID + 1));
    return consumerID;
  }

  public void removeTrackingForClient(ClientID sourceNodeID) {
    // Currently does nothing - this is just a placeholder to get the API in place.
  }


  private void addNewEntityToMap(EntityID id, long version, long consumerID, byte[] configuration) {
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
}
