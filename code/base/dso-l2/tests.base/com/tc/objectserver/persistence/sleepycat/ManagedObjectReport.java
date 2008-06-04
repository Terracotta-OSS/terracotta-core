/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Counter;
import com.tc.util.SyncObjectIdSet;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ManagedObjectReport {

  private static final TCLogger logger = TCLogging.getLogger(ManagedObjectReport.class);

  private SleepycatPersistor    persistor;

  public ManagedObjectReport(File dir) throws Exception {

    DBEnvironment env = new DBEnvironment(true, dir);

    SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();

    final NullManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new NullManagedObjectChangeListenerProvider();

    persistor = new SleepycatPersistor(logger, env, serializationAdapterFactory);

    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);

  }

  public void report() {
    Map classMap = new HashMap();
    Set nullObjectIDSet = new HashSet();
    Integer nullObjectIDCounter = new Integer(0);
    Integer total = new Integer(0);
    Integer counter = new Integer(0);
    SyncObjectIdSet objectIDSet = persistor.getManagedObjectPersistor().getAllObjectIDs();
    for (Iterator iter = objectIDSet.iterator(); iter.hasNext();) {
      ObjectID objectID = (ObjectID) iter.next();
      total++;
      ManagedObject managedObject = persistor.getManagedObjectPersistor().loadObjectByID(objectID);
      boolean existInSet = objectIDSet.contains(objectID);
      if (managedObject == null) {
        log("managed object is null : " + objectID + " and exists in set is: " + existInSet);
        nullObjectIDSet.add(new NullObjectData(objectID, existInSet));
      } else {
        // class stats
        String className = managedObject.getManagedObjectState().getClassName();
        Counter classCounter = (Counter) classMap.get(className);
        if (classCounter == null) {
          classCounter = new Counter(0);
          classMap.put(className, classCounter);
        } else {
          classCounter.increment();
        }
      }

      for (Iterator r = managedObject.getObjectReferences().iterator(); r.hasNext();) {
        ObjectID mid = (ObjectID) r.next();
        total++;
        if (mid == null || mid.isNull()) {
          log("reference objectID is null and parent: ");
          log(managedObject.toString());
          nullObjectIDCounter++;
          continue;
        }
        ManagedObject mo = persistor.getManagedObjectPersistor().loadObjectByID(mid);
        existInSet = objectIDSet.contains(mid);
        if (mo == null) {
          log("reference Managed object is null : " + mid + " and exists in set is: " + existInSet + " and parent");
          log(managedObject.toString());
          nullObjectIDSet.add(new NullObjectData(managedObject, mid, existInSet));
        } else {
          // class stats
          String className = managedObject.getManagedObjectState().getClassName();
          Counter classCounter = (Counter) classMap.get(className);
          if (classCounter == null) {
            classCounter = new Counter(0);
            classMap.put(className, classCounter);
          } else {
            classCounter.increment();
          }
        }

      }

      if (counter < 2) {
        counter++;
        nullObjectIDSet.add(new NullObjectData(managedObject, null, existInSet));
      }

    }

    log("---------------------------------- Managed Object Report ----------------------------------------------------");
    log("\t Total number of objects read: " + total);
    log("\t Total number getObjectReferences that yielded null references: " + nullObjectIDCounter);
    log("\t Total number of references without ManagedObjects: " + nullObjectIDSet.size());
    log("\n\t Begin references with null ManagedObject summary --> \n");
    for (Iterator iter = nullObjectIDSet.iterator(); iter.hasNext();) {
      NullObjectData data = (NullObjectData) iter.next();
      log("\t\t " + data);
    }
    log("\t Begin Class Map summary --> \n");

    for (Iterator iter = classMap.keySet().iterator(); iter.hasNext();) {
      String key = (String) iter.next();
      log("\t\t Class: --> " + key + " had --> " + ((Counter) classMap.get(key)).get() + " references");
    }
    log("------------------------------------------End-----------------------------------------------------------------");

  }

  private static class NullObjectData {

    private ManagedObject parent;

    private ObjectID      objectID;

    private boolean       exitInSet = false;

    public NullObjectData(ObjectID objectID, boolean existInSet) {
      this.objectID = objectID;
    }

    public NullObjectData(ManagedObject parent, ObjectID objectID, boolean existInSet) {
      this.parent = parent;
      this.objectID = objectID;
    }

    public ManagedObject getParent() {
      return parent;
    }

    public ObjectID getObjectID() {
      return objectID;
    }

    public String toString() {
      StringWriter writer = new StringWriter();
      PrintWriter pWriter = new PrintWriter(writer);
      PrettyPrinter out = new PrettyPrinterImpl(pWriter);
      out.println();
      out.print("Summary of reference with null ManagedObject").duplicateAndIndent().println();
      out.indent().print("identityHashCode: " + System.identityHashCode(this)).println();
      out.indent().print("objectID: " + objectID).println();
      out.indent().print("exist in getAllObjectsIDs set: " + exitInSet).println();
      out.indent().print("parent:" + parent).println();

      return writer.getBuffer().toString();
    }

  }

  public static void main(String[] args) {
    if (args == null || args.length < 1) {
      usage();
      System.exit(1);
    }

    try {
      File dir = new File(args[0]);
      validateDir(dir);
      ManagedObjectReport reporter = new ManagedObjectReport(dir);
      reporter.report();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    log("Usage: ManagedObjectReport <environment home directory>");
  }

  private static void log(String message) {
    System.out.println(message);
  }

}
