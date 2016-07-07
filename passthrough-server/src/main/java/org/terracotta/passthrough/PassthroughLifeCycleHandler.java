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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.exception.EntityException;
import org.terracotta.passthrough.PassthroughServerMessageDecoder.LifeCycleMessageHandler;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


/**
 * This implementation has 2 different modes (may be worth splitting this into a few classes if it ever becomes complex but much of the code is the same):
 * 1) Backed by KeyValueStorage.
 * 2) Backed by Map.
 * The core logic which manipulates the data stored in either of these is the same, only how it is loaded or written-back.
 */
public class PassthroughLifeCycleHandler implements LifeCycleMessageHandler {
  private final KeyValueStorage<Long, List<LifeCycleRecord>> lifeCycleRecordByClientID;
  private final Map<Long, List<LifeCycleRecord>> fallbackMap;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public PassthroughLifeCycleHandler(IPersistentStorage persistentStorage, String topLevelName) {
    if (null != persistentStorage) {
      this.lifeCycleRecordByClientID = persistentStorage.getKeyValueStorage(topLevelName, Long.class, (Class)List.class);
      this.fallbackMap = null;
    } else {
      this.lifeCycleRecordByClientID = null;
      this.fallbackMap = new HashMap<Long, List<LifeCycleRecord>>();
    }
  }

  @Override
  public boolean didAlreadyHandle(long clientOriginID, long transactionID) throws EntityException {
    boolean didHandle = false;
    List<LifeCycleRecord> records = loadClient(clientOriginID);
    if (null != records) {
      for (LifeCycleRecord record : records) {
        if (record.transactionID == transactionID) {
          if (null == record.error) {
            // Success.
            didHandle = true;
            break;
          } else {
            // We know about this but it was a failure.
            throw record.error;
          }
        }
      }
    }
    return didHandle;
  }

  @Override
  public byte[] didAlreadyHandleResult(long clientOriginID, long transactionID) throws EntityException {
    byte[] result = null;
    List<LifeCycleRecord> records = loadClient(clientOriginID);
    if (null != records) {
      for (LifeCycleRecord record : records) {
        if (record.transactionID == transactionID) {
          if (null == record.error) {
            // Success.
            Assert.assertTrue(null != record.reconfigureResult);
            result = record.reconfigureResult;
            break;
          } else {
            // We know about this but it was a failure.
            throw record.error;
          }
        }
      }
    }
    return result;
  }

  @Override
  public void failureInMessage(long clientOriginID, long transactionID, long oldestTransactionID, EntityException error) {
    addNewRecord(clientOriginID, transactionID, oldestTransactionID, null, error);
  }

  @Override
  public void successInMessage(long clientOriginID, long transactionID, long oldestTransactionID, byte[] reconfigureResponse) {
    // Null error on success.
    EntityException error = null;
    addNewRecord(clientOriginID, transactionID, oldestTransactionID, reconfigureResponse, error);
  }


  private void addNewRecord(long clientOriginID, long transactionID, long oldestTransactionID, byte[] reconfigureResult, EntityException error) {
    List<LifeCycleRecord> newRecords = filteredRecords(clientOriginID, oldestTransactionID);
    LifeCycleRecord newRecord = new LifeCycleRecord();
    newRecord.transactionID = transactionID;
    newRecord.reconfigureResult = reconfigureResult;
    newRecord.error = error;
    newRecords.add(newRecord);
    storeClient(clientOriginID, newRecords);
  }

  private List<LifeCycleRecord> filteredRecords(long clientOriginID, long oldestTransactionID) {
    List<LifeCycleRecord> records = loadClient(clientOriginID);
    if (null == records) {
      records = new Vector<LifeCycleRecord>();
    }
    List<LifeCycleRecord> newRecords = new Vector<LifeCycleRecord>();
    for (LifeCycleRecord record : records) {
      if (record.transactionID >= oldestTransactionID) {
        newRecords.add(record);
      }
    }
    return newRecords;
  }

  private List<LifeCycleRecord> loadClient(long clientOriginID) {
    return (null != this.lifeCycleRecordByClientID)
        ? this.lifeCycleRecordByClientID.get(clientOriginID)
        : this.fallbackMap.get(clientOriginID);
  }

  private void storeClient(long clientOriginID, List<LifeCycleRecord> data) {
    if (null != this.lifeCycleRecordByClientID) {
      this.lifeCycleRecordByClientID.put(clientOriginID, data);
    } else {
      this.fallbackMap.put(clientOriginID, data);
    }
  }


  private static class LifeCycleRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public long transactionID;
    public byte[] reconfigureResult;
    public EntityException error;
  }
}
