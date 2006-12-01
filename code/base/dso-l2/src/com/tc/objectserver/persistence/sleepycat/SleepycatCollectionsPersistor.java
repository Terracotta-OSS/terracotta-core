/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.io.serializer.DSOSerializerPolicy;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.api.BasicSerializer;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;

public class SleepycatCollectionsPersistor extends SleepycatPersistorBase {

  private final Database                   database;
  private final BasicSerializer            serializer;
  private final ByteArrayOutputStream      bao;
  private final SleepycatCollectionFactory collectionFactory;
  private final TCObjectOutputStream oo;

  public SleepycatCollectionsPersistor(TCLogger logger, Database mapsDatabase,
                                       SleepycatCollectionFactory sleepycatCollectionFactory) {
    this.database = mapsDatabase;
    this.collectionFactory = sleepycatCollectionFactory;
    DSOSerializerPolicy policy = new DSOSerializerPolicy();
    this.serializer = new BasicSerializer(policy);
    this.bao = new ByteArrayOutputStream(1024);
    this.oo = new TCObjectOutputStream(bao);
  }

  public void saveMap(PersistenceTransaction tx, SleepycatPersistableMap map) throws IOException, DatabaseException {
    map.commit(this, tx, database);
  }

  public synchronized byte[] serialize(long id, Object o) throws IOException {
    oo.writeLong(id);
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public synchronized byte[] serialize(Object o) throws IOException {
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public SleepycatPersistableMap loadMap(PersistenceTransaction tx, ObjectID id) throws IOException,
      ClassNotFoundException, DatabaseException {
    SleepycatPersistableMap map = (SleepycatPersistableMap) collectionFactory.createPersistentMap(id);
    map.load(this, tx, database);
    return map;
  }

  public Object deserialize(int start, byte[] data) throws IOException, ClassNotFoundException {
    if (start >= data.length) return null;
    ByteArrayInputStream bai = new ByteArrayInputStream(data, start, data.length - start);
    ObjectInput ois = new TCObjectInputStream(bai);
    return serializer.deserializeFrom(ois);
  }

  public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    return deserialize(0, data);
  }

  /**
   * This method is slightly dubious in that it assumes that the ObjectID is the first 8 bytes of the Key in the entire
   * collections database.(which is true, but that logic is spread elsewhere)
   * 
   * @throws DatabaseException
   */
  public boolean deleteCollection(PersistenceTransaction tx, ObjectID id) throws DatabaseException {
    // XXX:: Since we read in one direction and since we have to read the first record of the next map to break out, we
    // need this to avoid deadlocks between commit thread and GC thread. Hence READ_COMMITTED
    Cursor c = database.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
    try {
      boolean found = false;
      byte idb[] = Conversion.long2Bytes(id.toLong());
      DatabaseEntry key = new DatabaseEntry();
      key.setData(idb);
      DatabaseEntry value = new DatabaseEntry();
      value.setPartial(0, 0, true);
      if (c.getSearchKeyRange(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        do {
          if (partialMatch(idb, key.getData())) {
            found = true;
            c.delete();
          } else {
            break;
          }
        } while (c.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS);
      }
      return found;
    } finally {
      c.close();
    }
  }

  private boolean partialMatch(byte[] idbytes, byte[] key) {
    if (key.length < idbytes.length) return false;
    for (int i = 0; i < idbytes.length; i++) {
      if (idbytes[i] != key[i]) return false;
    }
    return true;
  }

}
