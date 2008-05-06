/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public abstract class AbstractTestManagedObjectState extends TestCase {
  protected static final String                 loaderDesc = "System.loader";
  protected ObjectID                            objectID;
  protected ManagedObjectChangeListenerProvider listenerProvider;

  public void setUp() throws Exception {
    super.setUp();
    listenerProvider = new NullManagedObjectChangeListenerProvider();
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(listenerProvider, new InMemoryPersistor());
    objectID = new ObjectID(2000);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    ManagedObjectStateFactory.disableSingleton(false);
    objectID = null;
    listenerProvider = null;
  }

  protected ManagedObjectState createManagedObjectState(String className, TestDNACursor cursor) throws Exception {
    ManagedObjectState state = ManagedObjectStateFactory.getInstance().createState(new ObjectID(1), ObjectID.NULL_ID,
                                                                                   className, loaderDesc, cursor);
    return state;
  }

  public void basicTestUnit(String className, final byte type, TestDNACursor cursor, int objCount) throws Exception {
    basicTestUnit(className, type, cursor, objCount, true);
  }

  public void basicTestUnit(String className, final byte type, TestDNACursor cursor, int objCount,
                            boolean verifyReadWrite) throws Exception {
    ManagedObjectState state = createManagedObjectState(className, cursor);
    state.apply(objectID, cursor, new BackReferences());

    // API verification
    basicAPI(className, type, cursor, objCount, state);

    // dehydrate
    basicDehydrate(cursor, objCount, state);

    // writeTo, readFrom and equal
    if (verifyReadWrite) {
      basicReadWriteEqual(type, state);
    }
  }

  protected void basicAPI(String className, final byte type, TestDNACursor cursor, int objCount,
                          ManagedObjectState state) {
    Assert.assertEquals("BackReferences object size", objCount, state.getObjectReferences().size());
    Assert.assertTrue(state.getType() == type);
    Assert.assertTrue("ClassName:" + state.getClassName(), state.getClassName().equals(className));

  }

  protected void basicDehydrate(TestDNACursor cursor, int objCount, ManagedObjectState state) {
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    cursor.reset();
    cursor.next();
    while (cursor.next()) {
      Object action = cursor.getAction();
      Assert.assertTrue(dnaWriter.containsAction(action));
    }
  }

  protected void basicReadWriteEqual(final byte type, ManagedObjectState state) throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);
    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    TCObjectInputStream in = new TCObjectInputStream(bin);
    ManagedObjectState state2 = ManagedObjectStateFactory.getInstance().readManagedObjectStateFrom(in, type);
    Assert.assertTrue(state.equals(state2));
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

    public void finalizeDNA(boolean isDeltaDNA) {
      //
    }

    public void finalizeDNA(boolean isDeltaDNA, int actionCount, int totalLength) {
      //
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

    protected boolean containsAction(Object targetAction) {
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
      throw new UnsupportedOperationException();
    }

    public void finalizeHeader() {
      //
    }

    public boolean isContiguous() {
      return true;
    }

    public void markSectionEnd() {
      //
    }
  }

  public class TestDNACursor implements DNACursor {
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

  public interface MyProxyInf1 {
    public int getValue();

    public void setValue(int i);
  }

  public interface MyProxyInf2 {
    public String getStringValue();

    public void setStringValue(String str);
  }

  public static class MyInvocationHandler implements InvocationHandler {
    private Map values       = new HashMap();
    private Map stringValues = new HashMap();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("getValue")) {
        return values.get(proxy);
      } else if (method.getName().equals("setValue")) {
        values.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("setStringValue")) {
        stringValues.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("getStringValue")) {
        return stringValues.get(proxy);
      } else if (method.getName().equals("hashCode")) { return new Integer(System.identityHashCode(proxy)); }
      return null;
    }
  }
}
