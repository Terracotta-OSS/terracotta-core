/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCRootDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Conversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BerkeleyDBTCRootDatabase extends BerkeleyDBTCBytesBytesDatabase implements TCRootDatabase {
  private final CursorConfig rootDBCursorConfig = new CursorConfig();

  public BerkeleyDBTCRootDatabase(Database db) {
    super(db);
    this.rootDBCursorConfig.setReadCommitted(true);
  }

  public Status put(byte[] rootName, long id, PersistenceTransaction tx) {
    byte[] value = Conversion.long2Bytes(id);
    return put(rootName, value, tx);
  }

  public long getIdFromName(byte[] rootName, PersistenceTransaction tx) {
    // cannot use super.get as making use of OperationStatus
    OperationStatus status = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      key.setData(rootName);

      status = this.db.get(pt2nt(tx), key, value, LockMode.DEFAULT);
      if (OperationStatus.SUCCESS.equals(status)) { return Conversion.bytes2Long(value.getData()); }
      if (OperationStatus.NOTFOUND.equals(status)) { return ObjectID.NULL_ID.toLong(); }
      throw new DBException("Could not retrieve root");
    } catch (Throwable t) {
      throw new DBException(t);
    }
  }

  public List<byte[]> getRootNames(PersistenceTransaction tx) {
    List<byte[]> rv = new ArrayList<byte[]>();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = this.db.openCursor(pt2nt(tx), this.rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(key.getData());
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public Set<ObjectID> getRootIds(PersistenceTransaction tx) {
    Set<ObjectID> rv = new HashSet<ObjectID>();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = this.db.openCursor(pt2nt(tx), this.rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(new ObjectID(Conversion.bytes2Long(value.getData())));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public Map<byte[], Long> getRootNamesToId(PersistenceTransaction tx) {
    Map<byte[], Long> rv = new HashMap<byte[], Long>();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = this.db.openCursor(pt2nt(tx), this.rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.put(key.getData(), Conversion.bytes2Long(value.getData()));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }
}
