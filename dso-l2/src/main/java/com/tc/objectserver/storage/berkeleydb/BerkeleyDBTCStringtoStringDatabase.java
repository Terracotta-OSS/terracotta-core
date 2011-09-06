/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Conversion;

public class BerkeleyDBTCStringtoStringDatabase extends BerkeleyDBTCBytesBytesDatabase implements
    TCStringToStringDatabase {

  public BerkeleyDBTCStringtoStringDatabase(Database db) {
    super(db);
  }

  public Status get(TCDatabaseEntry<String, String> entry, PersistenceTransaction tx) {
    DatabaseEntry dkey = new DatabaseEntry();
    dkey.setData(Conversion.string2Bytes(entry.getKey()));
    DatabaseEntry dvalue = new DatabaseEntry();
    OperationStatus status = this.db.get(pt2nt(tx), dkey, dvalue, LockMode.DEFAULT);
    if (status.equals(OperationStatus.SUCCESS)) {
      entry.setValue(Conversion.bytes2String(dvalue.getData()));
      return Status.SUCCESS;
    } else if (status.equals(OperationStatus.NOTFOUND)) { return Status.NOT_FOUND; }
    return Status.NOT_SUCCESS;
  }

  public Status put(String key, String value, PersistenceTransaction tx) {
    byte[] keyInBytes = Conversion.string2Bytes(key);
    byte[] valueInBytes = Conversion.string2Bytes(value);
    return put(keyInBytes, valueInBytes, tx);
  }

  public Status delete(String key, PersistenceTransaction tx) {
    byte[] keyInBytes = Conversion.string2Bytes(key);
    return delete(keyInBytes, tx);
  }
}
