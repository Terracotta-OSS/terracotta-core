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

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.EntityID;
import com.tc.objectserver.persistence.EntityData.JournalEntry;
import com.tc.objectserver.persistence.EntityData.Key;
import com.tc.objectserver.persistence.EntityData.Value;
import com.tc.util.Assert;
import com.tc.util.State;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.terracotta.exception.EntityException;
import org.terracotta.persistence.IPlatformPersistence;


/**
 * Stores the information relating to the entities currently alive on the platform into persistent storage.
 */
public class EntityPersistor {
  private static final TCLogger LOGGER = TCLogging.getLogger(EntityPersistor.class);
  
  private static final String ENTITIES_ALIVE_FILE_NAME = "entities_alive.map";
  private static final String JOURNAL_CONTAINER_FILE_NAME = "journal_container.map";
  private static final String COUNTERS_FILE_NAME = "counters.map";
  private static final String COUNTERS_CONSUMER_ID = "counters:consumerID";

  private final IPlatformPersistence storageManager;
  private final HashMap<EntityData.Key, EntityData.Value> entities;
  private final HashMap<ClientID, List<EntityData.JournalEntry>> entityLifeJournal;
  private final HashMap<String, Long> counters;

  @SuppressWarnings({ "unchecked" })
  public EntityPersistor(IPlatformPersistence storageManager) {
    this.storageManager = storageManager;
    try {
      HashMap<EntityData.Key, EntityData.Value> entities = (HashMap<Key, Value>) this.storageManager.loadDataElement(ENTITIES_ALIVE_FILE_NAME);
      this.entities = (null != entities) ? entities : new HashMap<>();
      HashMap<ClientID, List<EntityData.JournalEntry>> entityLifeJournal = (HashMap<ClientID, List<JournalEntry>>) this.storageManager.loadDataElement(JOURNAL_CONTAINER_FILE_NAME);
      this.entityLifeJournal = (null != entityLifeJournal) ? entityLifeJournal : new HashMap<>();
      HashMap<String, Long> counters = (HashMap<String, Long>) this.storageManager.loadDataElement(COUNTERS_FILE_NAME);
      this.counters = (null != counters) ? counters : new HashMap<>();
      // Make sure that the consumerID is initialized to 1 (0 reserved for platform).
      if (!this.counters.containsKey(COUNTERS_CONSUMER_ID)) {
        this.counters.put(COUNTERS_CONSUMER_ID, new Long(1));
      }
    } catch (IOException e) {
      // We don't expect this during startup so just throw it as runtime.
      throw new RuntimeException("Failure reading EntityPersistor map files", e);
    }
  }

  public synchronized void clear() {
    this.entities.clear();
    this.entityLifeJournal.clear();
    this.counters.clear();
    if (!this.counters.containsKey(COUNTERS_CONSUMER_ID)) {
      this.counters.put(COUNTERS_CONSUMER_ID, new Long(1));
    }
    // We can destroy the backing for these objects.
    try {
      this.storageManager.storeDataElement(ENTITIES_ALIVE_FILE_NAME, null);
      this.storageManager.storeDataElement(JOURNAL_CONTAINER_FILE_NAME, null);
      this.storageManager.storeDataElement(COUNTERS_FILE_NAME, null);
    } catch (IOException e) {
      // In general, we have no way of solving this problem so throw it.
      throw new RuntimeException("Failure storing EntityPersistor map files", e);
    }
  }

  public Collection<EntityData.Value> loadEntityData() {
    return this.entities.values();
  }

  public boolean containsEntity(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id) {
    LOGGER.debug("containsEntity " + clientID + " " + transactionID + " " + id);
    // This is new so look up the answer and store it in the journal.
    EntityData.Key key = new EntityData.Key();
    key.className = id.getClassName();
    key.entityName = id.getEntityName();
    // Make sure that the EntityID makes sense.
    Assert.assertNotNull(key.className);
    Assert.assertNotNull(key.entityName);
    return this.entities.containsKey(key);
  }

  /**
   * Consults the journal to see if there already was an entity created with this transactionID from the referenced clientID.
   * If an attempt was made, true is returned (on success) or EntityException is thrown (if it was a failure).
   * False is returned if this clientID and transactionID seem new.
   */
  public boolean wasEntityCreatedInJournal(ClientID clientID, long transactionID) throws EntityException {
    boolean didSucceed = false;
    LOGGER.debug("wasEntityCreatedInJournal " + clientID + " " + transactionID);
    EntityData.JournalEntry entry = getEntryForTransaction(clientID, transactionID);
    if (null != entry) {
      if (null == entry.failure) {
        didSucceed = true;
      } else {
        throw entry.failure;
      }
    }
    return didSucceed;
  }

  public void entityCreateFailed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityException error) {
    LOGGER.debug("createFailed " + clientID + " " + transactionID, error);
    addToJournal(clientID, transactionID, oldestTransactionOnClient, EntityData.Operation.CREATE, null, error);
  }

  public void entityCreated(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id, long version, long consumerID, boolean canDelete, byte[] configuration) {
    LOGGER.debug("entityCreated " + clientID + " " + transactionID + " " + id + " " + version);
    addNewEntityToMap(id, version, consumerID, canDelete, configuration);
    
    // Record this in the journal - null error on success.
    addToJournal(clientID, transactionID, oldestTransactionOnClient, EntityData.Operation.CREATE, null, null);
  }
  
  public void entityCreatedNoJournal(EntityID id, long version, long consumerID, boolean canDelete, byte[] configuration) {
    LOGGER.debug("entityCreatedNoJournal " + id);
    addNewEntityToMap(id, version, consumerID, canDelete, configuration);
    // (Note that we don't store this into the journal - this is used for passive sync).
  }

  /**
   * Consults the journal to see if there already was an entity destroyed with this transactionID from the referenced clientID.
   * If an attempt was made, true is returned (on success) or EntityAlreadyExistsException is thrown.
   * False is returned if this clientID and transactionID seem new.
   */
  public boolean wasEntityDestroyedInJournal(ClientID clientID, long transactionID) throws EntityException {
    LOGGER.debug("wasEntityDestroyedInJournal " + clientID + " " + transactionID);
    boolean didSucceed = false;
    EntityData.JournalEntry entry = getEntryForTransaction(clientID, transactionID);
    if (null != entry) {
      if (null == entry.failure) {
        didSucceed = true;
      } else {
        throw entry.failure;
      }
    }
    return didSucceed;
  }

  public void entityDestroyFailed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityException error) {
    LOGGER.debug("entityDestroyFailed " + clientID + " " + transactionID);
    addToJournal(clientID, transactionID, oldestTransactionOnClient, EntityData.Operation.DESTROY, null, error);
  }

  public void entityDestroyed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id) {
    LOGGER.debug("entityDestroyed " + clientID + " " + transactionID + " " + id);
    EntityData.Key key = new EntityData.Key();
    key.className = id.getClassName();
    key.entityName = id.getEntityName();
    Assert.assertTrue(this.entities.containsKey(key));
    this.entities.remove(key);
    storeToDisk(ENTITIES_ALIVE_FILE_NAME, this.entities);
    
    // Record this in the journal - null error on success.
    addToJournal(clientID, transactionID, oldestTransactionOnClient, EntityData.Operation.DESTROY, null, null);
  }

  public byte[] reconfiguredResultInJournal(ClientID clientID, long transactionID) throws EntityException {
    LOGGER.debug("reconfiguredResultInJournal " + clientID + " " + transactionID);
    byte[] cachedResult = null;
    EntityData.JournalEntry entry = getEntryForTransaction(clientID, transactionID);
    if (null != entry) {
      if (null == entry.failure) {
        Assert.assertNotNull(entry.reconfigureResponse);
        cachedResult = entry.reconfigureResponse;
      } else {
        throw entry.failure;
      }
    }
    return cachedResult;
  }

  public void entityReconfigureFailed(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityException error) {
    LOGGER.debug("entityReconfigureFailed " + clientID + " " + transactionID);
    addToJournal(clientID, transactionID, oldestTransactionOnClient, EntityData.Operation.RECONFIGURE, null, error);
  }

  /**
   * @return The over-written configuration value.
   */
  public byte[] entityReconfigureSucceeded(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityID id, long version, byte[] configuration) {
    LOGGER.debug("entityReconfigureSucceeded " + clientID + " " + transactionID);
    String className = id.getClassName();
    String entityName = id.getEntityName();

    EntityData.Key key = new EntityData.Key();
    key.className = className;
    key.entityName = entityName;
    
    EntityData.Value val = this.entities.get(key);
    byte[] previousConfiguration = val.configuration;
    Assert.assertNotNull(previousConfiguration);
    val.configuration = configuration;
    Assert.assertEquals(version, val.version);
    
    this.entities.put(key, val);
    storeToDisk(ENTITIES_ALIVE_FILE_NAME, this.entities);
    
    // Record this in the journal.
    addToJournal(clientID, transactionID, oldestTransactionOnClient, EntityData.Operation.RECONFIGURE, previousConfiguration, null);
    
    // Return what we over-wrote.
    return previousConfiguration;
  }

  public long getNextConsumerID() {
    long consumerID = this.counters.get(COUNTERS_CONSUMER_ID);
    this.counters.put(COUNTERS_CONSUMER_ID, new Long(consumerID + 1));
    storeToDisk(COUNTERS_FILE_NAME, this.counters);
    return consumerID;
  }

  public void setNextConsumerID(long consumerID) {
    long checkID = this.counters.get(COUNTERS_CONSUMER_ID);
    if (consumerID >= checkID) {
      this.counters.put(COUNTERS_CONSUMER_ID, new Long(consumerID + 1));
      storeToDisk(COUNTERS_FILE_NAME, this.counters);
    }
  }
  
  public synchronized void removeTrackingForClient(ClientID sourceNodeID) {
    this.entityLifeJournal.remove(sourceNodeID);
  }


  private List<JournalEntry> filterJournal(List<JournalEntry> list, long oldestTransactionOnClient) {
    Assert.assertNotNull(list);
    List<JournalEntry> newList = new Vector<>();
    for (JournalEntry entry : list) {
      if (entry.transactionID >= oldestTransactionOnClient) {
        newList.add(entry);
      }
    }
    return newList;
  }

  private synchronized void addToJournal(ClientID clientID, long transactionID, long oldestTransactionOnClient, EntityData.Operation operation, byte[] reconfigureResult, EntityException error) {
    List<EntityData.JournalEntry> rawJournal = this.entityLifeJournal.get(clientID);
    // Note that this may be the first time we encountered this client.
    if (null == rawJournal) {
      rawJournal = new Vector<>();
    }
    List<EntityData.JournalEntry> clientJournal = filterJournal(rawJournal, oldestTransactionOnClient);
    JournalEntry newEntry = new JournalEntry();
    newEntry.operation = operation;
    newEntry.transactionID = transactionID;
    newEntry.failure = error;
    newEntry.reconfigureResponse = reconfigureResult;
    clientJournal.add(newEntry);
    this.entityLifeJournal.put(clientID, clientJournal);
    storeToDisk(JOURNAL_CONTAINER_FILE_NAME, this.entityLifeJournal);
  }

  private synchronized JournalEntry getEntryForTransaction(ClientID clientID, long transactionID) {
    JournalEntry foundEntry = null;
    List<EntityData.JournalEntry> clientJournal =  this.entityLifeJournal.get(clientID);
    // Note that we may not know anything about this client.
    LOGGER.debug("checking " + clientID + " " + clientJournal);
    if (null != clientJournal) {
      for (JournalEntry entry : clientJournal) {
        if (entry.transactionID == transactionID) {
          foundEntry = entry;
          break;
        }
      }
    }
    return foundEntry;
  }

  private void addNewEntityToMap(EntityID id, long version, long consumerID, boolean canDelete, byte[] configuration) {
    String className = id.getClassName();
    String entityName = id.getEntityName();
    
    EntityData.Key key = new EntityData.Key();
    EntityData.Value value = new EntityData.Value();
    key.className = className;
    key.entityName = entityName;
    value.className = className;
    value.version = version;
    value.consumerID = consumerID;
    value.canDelete = canDelete;
    value.entityName = entityName;
    value.configuration = configuration;
    this.entities.put(key, value);
    storeToDisk(ENTITIES_ALIVE_FILE_NAME, this.entities);
  }
  
<<<<<<< Updated upstream
  public synchronized void setState(State state, Set<ClientID> connectedClients) {
    Set<ClientID> clients = new HashSet<>(this.entityLifeJournal.keySet());
=======
  public synchronized void removeOrphanedClientsFromJournal(Set<ClientID> connectedClients) {
>>>>>>> Stashed changes
    this.entityLifeJournal.entrySet().removeIf(e->!connectedClients.contains(e.getKey()));
    storeToDisk(JOURNAL_CONTAINER_FILE_NAME, this.entityLifeJournal);
  }
  
  public synchronized void serialize(ObjectOutput bucket) throws IOException {
    Set<ClientID> locals = this.entityLifeJournal.keySet();
    int size = locals.size();
    bucket.writeInt(size);
    for (ClientID local : locals) {
      bucket.writeObject(local);
      bucket.writeObject(this.entityLifeJournal.get(local));
    }
    bucket.writeLong(this.counters.get(COUNTERS_CONSUMER_ID));
  }  
  
  public synchronized void layer(ObjectInput bucket) throws IOException {
    try {
      int size = bucket.readInt();
      LOGGER.debug("log size " + size);
      for (int x=0;x<size;x++) {
        ClientID key = (ClientID)bucket.readObject();
        @SuppressWarnings("unchecked")
        List<EntityData.JournalEntry> journal = (List<EntityData.JournalEntry>)bucket.readObject();
        List<EntityData.JournalEntry> check = (List<EntityData.JournalEntry>)this.entityLifeJournal.get(key);
        if (check == null) {
          this.entityLifeJournal.put(key, journal);
          LOGGER.debug(key + " putting " + journal);
        } else {
          int pos = 0;
          for (JournalEntry je : journal) {
            while (pos < check.size() && check.get(pos).transactionID < je.transactionID) {
              pos += 1;
            }
            if (pos == check.size() || check.get(pos).transactionID != je.transactionID) {
              check.add(pos, je);
            }
          }
          LOGGER.debug(key + " layering " + journal + " " + check);
          this.entityLifeJournal.put(key, check);
        }
      }
    } catch (ClassNotFoundException cnf) {
      throw new IOException(cnf);
    }
    storeToDisk(JOURNAL_CONTAINER_FILE_NAME, this.entityLifeJournal);
    long nextConsumer = bucket.readLong();
    this.counters.put(COUNTERS_CONSUMER_ID, nextConsumer);
    storeToDisk(COUNTERS_FILE_NAME, this.counters);
  }

  private void storeToDisk(String dataName, Serializable dataElement) {
    try {
      this.storageManager.storeDataElement(dataName, dataElement);
    } catch (IOException e) {
      // In general, we have no way of solving this problem so throw it.
      throw new RuntimeException("Failure storing EntityPersistor map file", e);
    }
  }
}
