/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.exception.ImplementMe;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class ManagedObjectStateSerialization15Test extends TestCase {
  private static final String                 loaderDesc = "System.loader";

  private ObjectID                            objectID;
  private ManagedObjectChangeListenerProvider listenerProvider;

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

  public void testEnum() throws Exception {
    String className = "java.lang.Enum";
    State state = State.RUN;
    TestDNACursor cursor = new TestDNACursor();

    cursor.addLiteralAction(state);
    ManagedObjectState managedObjectState = applyValidation(className, cursor);
    serializationValidation(managedObjectState, cursor, ManagedObjectState.LITERAL_TYPE);
  }

  private ManagedObjectState applyValidation(String className, DNACursor dnaCursor) throws Exception {
    ManagedObjectState state = apply(className, dnaCursor);
    TestDNAWriter dnaWriter = dehydrate(state);
    validate(dnaCursor, dnaWriter);

    return state;
  }

  private void serializationValidation(ManagedObjectState state, DNACursor dnaCursor, byte type) throws Exception {
    byte[] buffer = writeTo(state);
    TestDNAWriter dnaWriter = readFrom(type, buffer);
    validate(dnaCursor, dnaWriter);
  }

  private byte[] writeTo(ManagedObjectState state) throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);

    return bout.toByteArray();
  }

  private TestDNAWriter readFrom(byte type, byte[] buffer) throws Exception {
    ByteArrayInputStream bin = new ByteArrayInputStream(buffer);
    TCObjectInputStream in = new TCObjectInputStream(bin);

    ManagedObjectState state = ManagedObjectStateFactory.getInstance().readManagedObjectStateFrom(in, type);
    return dehydrate(state);
  }

  private ManagedObjectState apply(String className, DNACursor dnaCursor) throws Exception {
    ManagedObjectState state = ManagedObjectStateFactory.getInstance().createState(new ObjectID(1), ObjectID.NULL_ID,
                                                                                   className, loaderDesc, dnaCursor);
    state.apply(objectID, dnaCursor, new BackReferences());
    return state;
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

    public void finalizeDNA() {
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

    public void addPhysicalAction(String fieldName, Object value, boolean canBeReferenced) {
      physicalActions.add(new PhysicalAction(fieldName, value, canBeReferenced));
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

    public void addClassLoaderAction(String classLoaderFieldName, Object value) {
      //
    }

    public void addSubArrayAction(int start, Object array, int length) {
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

  public interface EnumIntf {
    public int getStateNum();

    public void setStateNum(int stateNum);
  }

  public enum State implements EnumIntf {
    START(0), RUN(1), STOP(2);

    private int stateNum;

    State(int stateNum) {
      this.stateNum = stateNum;
    }

    public int getStateNum() {
      return this.stateNum;
    }

    public void setStateNum(int stateNum) {
      this.stateNum = stateNum;
    }
  }

}
