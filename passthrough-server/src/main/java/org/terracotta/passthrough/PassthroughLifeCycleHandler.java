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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.terracotta.exception.EntityException;
import org.terracotta.passthrough.PassthroughServerMessageDecoder.LifeCycleMessageHandler;
import org.terracotta.persistence.IPlatformPersistence;


/**
 * This implementation has 2 different modes (may be worth splitting this into a few classes if it ever becomes complex but much of the code is the same):
 * 1) Backed by KeyValueStorage.
 * 2) Backed by Map.
 * The core logic which manipulates the data stored in either of these is the same, only how it is loaded or written-back.
 */
public class PassthroughLifeCycleHandler implements LifeCycleMessageHandler {
  private static final String LIFE_CYCLE_RECORD_FILE_NAME = "life_cycle_record.map";

  private final IPlatformPersistence platformPersistence;
  private final HashMap<Long, List<LifeCycleRecord>> lifeCycleRecordByClientIDMap;

  @SuppressWarnings({ "unchecked", })
  public PassthroughLifeCycleHandler(IPlatformPersistence platformPersistence, boolean shouldLoadStorage) {
    Assert.assertTrue(null != platformPersistence);
    this.platformPersistence = platformPersistence;
    HashMap<Long, List<LifeCycleRecord>> loadedMap = null;
    try {
      loadedMap = (HashMap<Long, List<LifeCycleRecord>>) (shouldLoadStorage ? this.platformPersistence.loadDataElement(LIFE_CYCLE_RECORD_FILE_NAME) : null);
    } catch (IOException e) {
      Assert.unexpected(e);
    }
    if (null == loadedMap) {
      loadedMap = new HashMap<Long, List<LifeCycleRecord>>();
    }
    this.lifeCycleRecordByClientIDMap = loadedMap;
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
    return this.lifeCycleRecordByClientIDMap.get(clientOriginID);
  }

  private void storeClient(long clientOriginID, List<LifeCycleRecord> data) {
    this.lifeCycleRecordByClientIDMap.put(clientOriginID, data);
    try {
      this.platformPersistence.storeDataElement(LIFE_CYCLE_RECORD_FILE_NAME, this.lifeCycleRecordByClientIDMap);
    } catch (IOException e) {
      Assert.unexpected(e);
    }
  }


  private static class LifeCycleRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public long transactionID;
    public byte[] reconfigureResult;
    public EntityException error;
  }
}
