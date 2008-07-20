/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.sleepycat.je.CursorConfig;
import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutput;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.NullObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.ManagedObjectPersistorImpl;
import com.tc.objectserver.persistence.sleepycat.SleepycatCollectionFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatCollectionsPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatSerializationAdapterFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ManagedObjectStateSerializationTestBase extends TCTestCase {
  private final TCLogger                     logger   = TCLogging.getTestingLogger(getClass());
  private final ObjectID                     objectID = new ObjectID(2000);

  private DBEnvironment                      env;
  private ManagedObjectPersistorImpl         managedObjectPersistor;
  private TestPersistenceTransactionProvider ptp;

  public void setUp() throws Exception {
    super.setUp();

    env = newDBEnvironment();
    SleepycatSerializationAdapterFactory sleepycatSerializationAdapterFactory = new SleepycatSerializationAdapterFactory();

    SleepycatPersistor persistor = new SleepycatPersistor(logger, env, sleepycatSerializationAdapterFactory);

    ptp = new TestPersistenceTransactionProvider();
    CursorConfig rootDBCursorConfig = new CursorConfig();
    SleepycatCollectionFactory sleepycatCollectionFactory = new SleepycatCollectionFactory();
    SleepycatCollectionsPersistor sleepycatCollectionsPersistor = new SleepycatCollectionsPersistor(logger, env
        .getMapsDatabase(), sleepycatCollectionFactory);

    managedObjectPersistor = new ManagedObjectPersistorImpl(logger, env.getClassCatalogWrapper().getClassCatalog(),
                                                            sleepycatSerializationAdapterFactory, env,
                                                            new TestMutableSequence(), env.getRootDatabase(),
                                                            rootDBCursorConfig, ptp, sleepycatCollectionsPersistor, env
                                                                .isParanoidMode());

    NullManagedObjectChangeListenerProvider listenerProvider = new NullManagedObjectChangeListenerProvider();
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(listenerProvider, persistor);
  }

  private DBEnvironment newDBEnvironment() throws Exception {
    File dbHome = new File(this.getTempDirectory(), getClass().getName() + "db");
    dbHome.mkdirs();
    return new DBEnvironment(true, dbHome);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    env.close();
    ManagedObjectStateFactory.disableSingleton(false);
  }

  protected ManagedObjectState applyValidation(String className, DNACursor dnaCursor) throws Exception {
    ManagedObject mo = new ManagedObjectImpl(objectID);

    TestDNA dna = new TestDNA(dnaCursor);
    dna.typeName = className;
    mo.apply(dna, new TransactionID(1), new BackReferences(), new NullObjectInstanceMonitor(), false);

    PersistenceTransaction txn = ptp.newTransaction();
    managedObjectPersistor.saveObject(txn, mo);
    txn.commit();

    ManagedObjectState state = mo.getManagedObjectState();
    TestDNAWriter dnaWriter = dehydrate(state);
    validate(dnaCursor, dnaWriter);

    return state;
  }

  protected void serializationValidation(ManagedObjectState state, DNACursor dnaCursor, byte type) throws Exception {
    ManagedObject loaded = managedObjectPersistor.loadObjectByID(objectID);
    TestDNAWriter dnaWriter = dehydrate(loaded.getManagedObjectState());
    validate(dnaCursor, dnaWriter);
  }

  private TestDNAWriter dehydrate(ManagedObjectState state) throws Exception {
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    return dnaWriter;
  }

  private void validate(DNACursor dnaCursor, TestDNAWriter writer) throws Exception {
    Assert.assertEquals(dnaCursor.getActionCount(), writer.getActionCount());
    dnaCursor.reset();
    while (dnaCursor.next()) {
      Object action = dnaCursor.getAction();
      Assert.assertTrue(writer.containsAction(action));
    }
  }

  public class TestDNAWriter implements DNAWriter {
    private List physicalActions = new ArrayList();
    private List logicalActions  = new ArrayList();
    private List literalActions  = new ArrayList();

    public TestDNAWriter() {
      //
    }

    public void addLogicalAction(int method, Object[] parameters) {
      logicalActions.add(new LogicalAction(method, parameters));
    }

    public void addPhysicalAction(String field, Object value) {
      addPhysicalAction(field, value, value instanceof ObjectID);
    }

    public void addArrayElementAction(int index, Object value) {
      //
    }

    public void addEntireArray(Object value) {
      physicalActions.add(new PhysicalAction(value));
    }

    public void addLiteralValue(Object value) {
      literalActions.add(new LiteralAction(value));
    }

    public void setParentObjectID(ObjectID id) {
      //
    }

    public void setArrayLength(int length) {
      //
    }

    public void addPhysicalAction(String fieldName, Object value, boolean canBeReference) {
      physicalActions.add(new PhysicalAction(fieldName, value, canBeReference));
    }

    public int getActionCount() {
      return logicalActions.size() + physicalActions.size() + literalActions.size();
    }

    private boolean containsAction(Object targetAction) {
      if (targetAction instanceof LogicalAction) {
        return containsLogicalAction((LogicalAction) targetAction);
      } else if (targetAction instanceof PhysicalAction) {
        return containsPhysicalAction((PhysicalAction) targetAction);
      } else if (targetAction instanceof LiteralAction) { return containsLiteralAction((LiteralAction) targetAction); }

      return false;
    }

    private boolean containsLogicalAction(LogicalAction targetAction) {
      for (Iterator i = logicalActions.iterator(); i.hasNext();) {
        LogicalAction action = (LogicalAction) i.next();
        if (identicalLogicalAction(targetAction, action)) { return true; }
      }
      return false;
    }

    private boolean containsPhysicalAction(PhysicalAction targetAction) {
      for (Iterator i = physicalActions.iterator(); i.hasNext();) {
        PhysicalAction action = (PhysicalAction) i.next();
        if (identicalPhysicalAction(targetAction, action)) { return true; }
      }
      return false;
    }

    private boolean containsLiteralAction(LiteralAction targetAction) {
      for (Iterator i = literalActions.iterator(); i.hasNext();) {
        LiteralAction action = (LiteralAction) i.next();
        if (identicalLiteralAction(targetAction, action)) { return true; }
      }
      return false;
    }

    private boolean identicalLiteralAction(LiteralAction a1, LiteralAction a2) {
      if (a1 == null || a2 == null) { return false; }
      if (a1.getObject() == null || a2.getObject() == null) { return false; }

      return a1.getObject().equals(a2.getObject());
    }

    private boolean identicalPhysicalAction(PhysicalAction a1, PhysicalAction a2) {
      if (a1 == null || a2 == null) { return false; }

      if (!a1.isEntireArray() && !a2.isEntireArray()) {
        if (a1.getFieldName() == null || a2.getFieldName() == null) { return false; }
      }

      if (a1.isEntireArray() != a2.isEntireArray()) { return false; }

      if (a1.getObject() == null && a2.getObject() == null) { return true; }
      if (a1.getObject() == null && a2.getObject() != null) { return false; }
      if (a1.getObject() != null && a2.getObject() == null) { return false; }

      if (a1.isEntireArray()) {
        return Arrays.equals((Object[]) a1.getObject(), (Object[]) a2.getObject());
      } else if (a1.getObject() instanceof Object[] && a2.getObject() instanceof Object[]) {
        return Arrays.equals((Object[]) a1.getObject(), (Object[]) a2.getObject());
      } else {
        if (a1.getFieldName().equals(a2.getFieldName())) { return (a1.getObject().equals(a2.getObject())); }
      }
      return false;
    }

    private boolean identicalLogicalAction(LogicalAction a1, LogicalAction a2) {
      if (a1 == null || a2 == null) { return false; }
      if (a1.getParameters() == null || a2.getParameters() == null) { return false; }

      if (a1.getMethod() == a2.getMethod()) {
        if (a1.getParameters().length == a2.getParameters().length) {
          for (int i = 0; i < a1.getParameters().length; i++) {
            if (!a1.getParameters()[i].equals(a2.getParameters()[i])) { return false; }
          }
          return true;
        }
      }
      return false;
    }

    public void addClassLoaderAction(String classLoaderFieldName, ClassLoader value) {
      //
    }

    public void addSubArrayAction(int start, Object array, int length) {
      //
    }

    public void copyTo(TCByteBufferOutput dest) {
      throw new ImplementMe();

    }

    public DNAWriter createAppender() {
      throw new ImplementMe();
    }

    public void finalizeHeader() {
      throw new ImplementMe();
    }

    public boolean isContiguous() {
      throw new ImplementMe();
    }

    public void markSectionEnd() {
      throw new ImplementMe();
    }
  }

  public static class TestDNACursor implements DNACursor {
    private List actions = new ArrayList();
    private int  current = -1;

    public void addPhysicalAction(String addFieldName, Object addObj, boolean isref) {
      actions.add(new PhysicalAction(addFieldName, addObj, isref));
    }

    public void addLogicalAction(int method, Object params[]) {
      actions.add(new LogicalAction(method, params));
    }

    public void addArrayAction(Object[] objects) {
      actions.add(new PhysicalAction(objects));
    }

    public void addLiteralAction(Object value) {
      actions.add(new LiteralAction(value));
    }

    public boolean next() {
      return actions.size() > ++current;
    }

    public LogicalAction getLogicalAction() {
      return (LogicalAction) actions.get(current);
    }

    public Object getAction() {
      return actions.get(current);
    }

    public PhysicalAction getPhysicalAction() {
      return (PhysicalAction) actions.get(current);
    }

    public boolean next(DNAEncoding encoding) {
      throw new ImplementMe();
    }

    public int getActionCount() {
      return actions.size();
    }

    public void reset() throws UnsupportedOperationException {
      current = -1;
    }
  }

}
