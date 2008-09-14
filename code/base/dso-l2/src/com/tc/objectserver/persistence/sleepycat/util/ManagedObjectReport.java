/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat.util;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Counter;
import com.tc.util.SyncObjectIdSet;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ManagedObjectReport extends BaseUtility {

  protected Map     classMap              = new HashMap();

  protected Set     nullObjectIDSet       = new HashSet();

  protected Counter objectIDIsNullCounter = new Counter(0);

  protected Set     doesNotExistInSet     = new HashSet();

  protected Counter totalCounter          = new Counter(0);

  public ManagedObjectReport(File dir) throws Exception {
    this(dir, new OutputStreamWriter(System.out));
  }

  public ManagedObjectReport(File dir, Writer writer) throws Exception {
    super(writer, new File[] { dir });
  }

  public void report() {

    SleepycatPersistor persistor = getPersistor(1);
    SyncObjectIdSet objectIDSet = persistor.getManagedObjectPersistor().getAllObjectIDs();
    objectIDSet.snapshot();
    for (Iterator iter = objectIDSet.iterator(); iter.hasNext();) {
      ObjectID objectID = (ObjectID) iter.next();
      totalCounter.increment();
      ManagedObject managedObject = persistor.getManagedObjectPersistor().loadObjectByID(objectID);
      if (managedObject == null) {
        log("managed object is null : " + objectID);
        nullObjectIDSet.add(new NullObjectData(objectID));
      } else {
        String className = managedObject.getManagedObjectState().getClassName();
        Counter classCounter = (Counter) classMap.get(className);
        if (classCounter == null) {
          classCounter = new Counter(1);
          classMap.put(className, classCounter);
        } else {
          classCounter.increment();
        }
      }

      for (Iterator r = managedObject.getObjectReferences().iterator(); r.hasNext();) {
        ObjectID mid = (ObjectID) r.next();
        totalCounter.increment();
        if (mid == null) {
          log("reference objectID is null and parent: ");
          log(managedObject.toString());
          nullObjectIDSet.add(new NullObjectData(managedObject, null));
        } else {
          if (mid.isNull()) {
            objectIDIsNullCounter.increment();
          } else {
            boolean exitInSet = objectIDSet.contains(mid);
            if (!exitInSet) {
              doesNotExistInSet.add(mid);
            }
          }
        }

      }
    }
  }

  public void listAllObjectIDs() {
    SleepycatPersistor persistor = getPersistor(1);
    SyncObjectIdSet objectIDSet = persistor.getManagedObjectPersistor().getAllObjectIDs();
    objectIDSet.snapshot();
    log("---------------------------------- Managed Object ID List ----------------------------------------------------");
    StringBuilder msg = new StringBuilder();
    int counter = 0;
    for (Iterator iter = objectIDSet.iterator(); iter.hasNext();) {
      ObjectID objectID = (ObjectID) iter.next();
      totalCounter.increment();
      msg.append(" " + objectID);
      if (++counter >= 10) {
        log(msg.toString());
        msg = new StringBuilder();
        counter = 0;
      }
    }
    if (counter > 0) log(msg.toString());
    log("---------------------------------------------------------------------------------------------------------------");
    log("\t Total number of objects: " + totalCounter.get());
  }

  public void listSpecificObjectByID(ObjectID objectID) {
    SleepycatPersistor persistor = getPersistor(1);
    log("---------------------------------- Managed Object " + objectID
        + " ----------------------------------------------------");
    ManagedObject managedObject = persistor.getManagedObjectPersistor().loadObjectByID(objectID);
    if (managedObject != null) {
      log(managedObject.toString());
    } else {
      log("non-existent:" + objectID);
    }
    log("---------------------------------------------------------------------------------------------------------------");

  }

  public void printReport() {
    log("---------------------------------- Managed Object Report ----------------------------------------------------");
    log("\t Total number of objects read: " + totalCounter.get());
    log("\t Total number getObjectReferences that yielded isNull references: " + objectIDIsNullCounter.get());
    log("\t Total number of references that does not exist in allObjectIDs set: " + doesNotExistInSet.size());
    log("\t does not exist in allObjectIDs set: " + doesNotExistInSet + " \n");
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

    public NullObjectData(ObjectID objectID) {
      this(null, objectID);
    }

    public NullObjectData(ManagedObject parent, ObjectID objectID) {
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
      reporter.printReport();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: ManagedObjectReport <environment home directory>");
  }

}
