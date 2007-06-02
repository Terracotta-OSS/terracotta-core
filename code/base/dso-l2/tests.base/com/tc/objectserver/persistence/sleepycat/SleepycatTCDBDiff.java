/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.impl.StringIndexImpl;
import com.tc.util.Assert;
import com.tc.util.SyncObjectIdSet;

import gnu.trove.TObjectLongHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SleepycatTCDBDiff {
  
  private static final String VERSION_STRING = "SleepycatTCDBDiff [ Ver 0.2]";

  private final SleepycatPersistor        sdb1;
  private final SleepycatPersistor        sdb2;
  private final File                      d1;
  private final File                      d2;
  private final boolean                   moDiff;
  private final ManagedObjectStateFactory mosf1;
  private final ManagedObjectStateFactory mosf2;

  public SleepycatTCDBDiff(File d1, File d2, boolean moDiff) throws TCDatabaseException, IOException {
    this.d1 = d1;
    this.d2 = d2;
    this.moDiff = moDiff;
    this.sdb1 = new SleepycatPersistor(TCLogging.getLogger(SleepycatTCDBDiff.class), new DBEnvironment(true, d1),
                                       new CustomSerializationAdapterFactory());
    this.sdb2 = new SleepycatPersistor(TCLogging.getLogger(SleepycatTCDBDiff.class), new DBEnvironment(true, d2),
                                       new CustomSerializationAdapterFactory());

    // Since we dont create any new MOState Objects in this program, we use 1 of the 2 persistor.
    ManagedObjectChangeListenerProviderImpl moclp = new ManagedObjectChangeListenerProviderImpl();
    moclp.setListener(new ManagedObjectChangeListener() {

      public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
        // NOP
      }

    });
    ManagedObjectStateFactory.disableSingleton(true);
    mosf1 = ManagedObjectStateFactory.createInstance(moclp, sdb1);
    mosf2 = ManagedObjectStateFactory.createInstance(moclp, sdb2);

  }

  public void diff() {
    log("Diffing [1] " + d1 + " and [2] " + d2);
    diffStringIndexer((StringIndexImpl) sdb1.getStringIndex(), (StringIndexImpl) sdb2.getStringIndex());
    diffManagedObjects(sdb1.getManagedObjectPersistor(), sdb2.getManagedObjectPersistor());
    diffClientStates(sdb1.getClientStatePersistor(), sdb2.getClientStatePersistor());
    diffTransactions(sdb1.getTransactionPersistor(), sdb2.getTransactionPersistor());
    diffGeneratedClasses(sdb1.getClassPersistor(), sdb2.getClassPersistor());
  }

  private void diffGeneratedClasses(ClassPersistor cp1, ClassPersistor cp2) {
    log("Diffing Generated Classes...(Partial implementation)");
    Map m1 = cp1.retrieveAllClasses();
    Map m2 = cp2.retrieveAllClasses();
    if (!m1.keySet().equals(m2.keySet())) {
      log("*** [1] containing " + m1.size() + " generated classes differs from [2] containing " + m2.size());
    }
  }

  private void diffTransactions(TransactionPersistor tp1, TransactionPersistor tp2) {
    log("Diffing Transactions...");
    Collection txns1 = tp1.loadAllGlobalTransactionDescriptors();
    Collection txns2 = tp2.loadAllGlobalTransactionDescriptors();
    if (!txns1.equals(txns2)) {
      log("*** [1] containing " + txns1.size() + " Transactions differs from [2] containing " + txns2.size()
          + " Transactions");
    }
  }

  private void diffClientStates(ClientStatePersistor cp1, ClientStatePersistor cp2) {
    log("Diffing ClientStates...");
    Set cids1 = cp1.loadClientIDs();
    Set cids2 = cp2.loadClientIDs();
    if (!cids1.equals(cids2)) {
      log("*** [1] containing " + cids1.size() + " ClientIDs differs from [2] containing " + cids2.size()
          + " ClientIDs");
    }
  }

  private void diffManagedObjects(ManagedObjectPersistor mp1, ManagedObjectPersistor mp2) {
    log("Diffing ManagedObjects...(Partial implementation)");
    SyncObjectIdSet oids1 = mp1.getAllObjectIDs();
    SyncObjectIdSet oids2 = mp2.getAllObjectIDs();
    if (!oids1.equals(oids2)) {
      log("*** [1] containing " + oids1.size() + " ObjectIDs differs from [2] containing " + oids2.size()
          + " ObjectIDs");
    }

    if (!moDiff) return;

    // The diff only makes sense if the sequence in which the objects created are same.
    // First diff the common set
    Set common = new HashSet(oids1);
    common.retainAll(oids2);
    for (Iterator i = common.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      ManagedObjectStateFactory.setInstance(mosf1);
      ManagedObject mo1 = mp1.loadObjectByID(oid);
      ManagedObjectStateFactory.setInstance(mosf2);
      ManagedObject mo2 = mp2.loadObjectByID(oid);
      Assert.assertEquals(mo1.getID(), mo2.getID());
      ManagedObjectState ms1 = mo1.getManagedObjectState();
      ManagedObjectState ms2 = mo2.getManagedObjectState();
      if (!ms1.equals(ms2)) {
        log("****** [1] " + ms1 + " differs from [2] " + ms2);
      }
    }

    // Unique
    oids1.removeAll(common);
    if (!oids1.isEmpty()) {
      log("****** [1] contains " + oids1 + " not in [2]");
    }
    oids2.removeAll(common);
    if (!oids2.isEmpty()) {
      log("****** [2] contains " + oids2 + " not in [1]");
    }
  }

  private static void log(String message) {
    System.out.println(message);
  }

  private void diffStringIndexer(StringIndexImpl stringIndex1, StringIndexImpl stringIndex2) {
    log("Diffing StringIndexes...");
    TObjectLongHashMap map1 = stringIndex1.getString2LongMappings();
    TObjectLongHashMap map2 = stringIndex2.getString2LongMappings();
    if (!map1.equals(map2)) {
      log("*** [1] containing " + map1.size() + " mapping differs from [2] containing " + map2.size() + " mapping");
    }
  }

  public static void main(String[] args) {
    boolean moDiff = false;
    
    log(VERSION_STRING);
    if (args == null || args.length < 2) {
      usage();
      System.exit(-1);
    } else if (args.length > 2) {
      Assert.assertEquals("-mo", args[2]);
      moDiff = true;
    }
    try {
      File d1 = new File(args[0]);
      validateDir(d1);
      File d2 = new File(args[1]);
      validateDir(d2);

      SleepycatTCDBDiff sdiff = new SleepycatTCDBDiff(d1, d2, moDiff);
      sdiff.diff();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(-2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    log("Usage: SleepycatTCDBDiff <environment home directory1> <environment home directory2> [-mo]");
  }

}
