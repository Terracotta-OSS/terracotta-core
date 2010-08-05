/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Counter;
import com.tc.util.ObjectIDSet;

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

  public ManagedObjectReport(final File dir) throws Exception {
    this(dir, new OutputStreamWriter(System.out));
  }

  public ManagedObjectReport(final File dir, final Writer writer) throws Exception {
    super(writer, new File[] { dir });
  }

  public void report() {

    final DBPersistorImpl persistor = getPersistor(1);
    final ObjectIDSet objectIDSet = persistor.getManagedObjectPersistor().snapshotObjectIDs();
    for (final Iterator iter = objectIDSet.iterator(); iter.hasNext();) {
      final ObjectID objectID = (ObjectID) iter.next();
      this.totalCounter.increment();
      final ManagedObject managedObject = persistor.getManagedObjectPersistor().loadObjectByID(objectID);
      if (managedObject == null) {
        log("managed object is null : " + objectID);
        this.nullObjectIDSet.add(new NullObjectData(objectID));
      } else {
        final String className = managedObject.getManagedObjectState().getClassName();
        Counter classCounter = (Counter) this.classMap.get(className);
        if (classCounter == null) {
          classCounter = new Counter(1);
          this.classMap.put(className, classCounter);
        } else {
          classCounter.increment();
        }
      }

      for (final Iterator r = managedObject.getObjectReferences().iterator(); r.hasNext();) {
        final ObjectID mid = (ObjectID) r.next();
        if (mid == null) {
          log("reference objectID is null and parent: ");
          log(managedObject.toString());
          this.nullObjectIDSet.add(new NullObjectData(managedObject, null));
        } else {
          if (mid.isNull()) {
            this.objectIDIsNullCounter.increment();
          } else {
            final boolean exitInSet = objectIDSet.contains(mid);
            if (!exitInSet) {
              this.doesNotExistInSet.add(mid);
            }
          }
        }

      }
    }
  }

  public void listAllObjectIDs() {
    final DBPersistorImpl persistor = getPersistor(1);
    final ObjectIDSet objectIDSet = persistor.getManagedObjectPersistor().snapshotObjectIDs();
    log("---------------------------------- Managed Object ID List ----------------------------------------------------");
    StringBuilder msg = new StringBuilder();
    int counter = 0;
    for (final Iterator iter = objectIDSet.iterator(); iter.hasNext();) {
      final ObjectID objectID = (ObjectID) iter.next();
      this.totalCounter.increment();
      msg.append(" " + objectID);
      if (++counter >= 10) {
        log(msg.toString());
        msg = new StringBuilder();
        counter = 0;
      }
    }
    if (counter > 0) {
      log(msg.toString());
    }
    log("---------------------------------------------------------------------------------------------------------------");
    log("\t Total number of objects: " + this.totalCounter.get());
  }

  public void listSpecificObjectByID(final ObjectID objectID) {
    final DBPersistorImpl persistor = getPersistor(1);
    log("---------------------------------- Managed Object " + objectID
        + " ----------------------------------------------------");
    final ManagedObject managedObject = persistor.getManagedObjectPersistor().loadObjectByID(objectID);
    if (managedObject != null) {
      log(managedObject.toString());
    } else {
      log("non-existent:" + objectID);
    }
    log("---------------------------------------------------------------------------------------------------------------");

  }

  public void printReport() {
    log("---------------------------------- Managed Object Report ----------------------------------------------------");
    log("\t Total number of objects read: " + this.totalCounter.get());
    log("\t Total number getObjectReferences that yielded isNull references: " + this.objectIDIsNullCounter.get());
    log("\t Total number of references that does not exist in allObjectIDs set: " + this.doesNotExistInSet.size());
    log("\t does not exist in allObjectIDs set: " + this.doesNotExistInSet + " \n");
    log("\t Total number of references without ManagedObjects: " + this.nullObjectIDSet.size());
    log("\n\t Begin references with null ManagedObject summary --> \n");
    for (final Iterator iter = this.nullObjectIDSet.iterator(); iter.hasNext();) {
      final NullObjectData data = (NullObjectData) iter.next();
      log("\t\t " + data);
    }
    log("\t Begin Class Map summary --> \n");

    for (final Iterator iter = this.classMap.keySet().iterator(); iter.hasNext();) {
      final String key = (String) iter.next();
      log("\t\t Class: --> " + key + " had --> " + ((Counter) this.classMap.get(key)).get() + " references");
    }
    log("------------------------------------------End-----------------------------------------------------------------");
  }

  private static class NullObjectData {

    private final ManagedObject parent;

    private final ObjectID      objectID;

    public NullObjectData(final ObjectID objectID) {
      this(null, objectID);
    }

    public NullObjectData(final ManagedObject parent, final ObjectID objectID) {
      this.parent = parent;
      this.objectID = objectID;
    }

    @Override
    public String toString() {
      final StringWriter writer = new StringWriter();
      final PrintWriter pWriter = new PrintWriter(writer);
      final PrettyPrinter out = new PrettyPrinterImpl(pWriter);
      out.println();
      out.print("Summary of reference with null ManagedObject").duplicateAndIndent().println();
      out.indent().print("identityHashCode: " + System.identityHashCode(this)).println();
      out.indent().print("objectID: " + this.objectID).println();
      out.indent().print("parent:" + this.parent).println();

      return writer.getBuffer().toString();
    }

  }

  public static void main(final String[] args) {
    if (args == null || args.length < 1) {
      usage();
      System.exit(1);
    }

    try {
      final File dir = new File(args[0]);
      validateDir(dir);
      final ManagedObjectReport reporter = new ManagedObjectReport(dir);
      reporter.report();
      reporter.printReport();
    } catch (final Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(final File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: ManagedObjectReport <environment home directory>");
  }

}
