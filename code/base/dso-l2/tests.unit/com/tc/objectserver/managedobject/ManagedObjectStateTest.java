/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.managedobject.bytecode.ClassNotCompatableException;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ManagedObjectStateTest extends TestCase {

  private ObjectID                            objectID;
  private ManagedObjectChangeListenerProvider listenerProvider;

  @Override
  public void setUp() throws Exception {
    this.objectID = new ObjectID(2002);
    this.listenerProvider = new NullManagedObjectChangeListenerProvider();
  }

  public void testPhysicalManagedObjectClassId() throws Exception {
    final String loaderDesc = "System.loader";

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(this.listenerProvider, new InMemoryPersistor());

    final HashMap types = new HashMap();
    types.put("field1", new ObjectID(1));
    types.put("field2", new Boolean(true));
    types.put("field3", new Character('c'));
    types.put("field4", new Long(5));
    types.put("field5", new Integer(15));
    types.put("field6", new String("neoistheone"));
    types.put("field7", new Short((short) 606));
    types.put("field8", new Double(98.76d));
    types.put("field9", new Float(8.8f));
    types.put("field10", new StackTraceElement("classname", "methodname", "filename", 123));

    final TestDNACursor cursor = new TestDNACursor();
    for (final Iterator i = types.entrySet().iterator(); i.hasNext();) {
      final Map.Entry element = (Map.Entry) i.next();
      cursor.addPhysicalAction((String) element.getKey(), element.getValue(), true);
    }

    final int numOfGeneratedClasses = 10000;
    final Map classNameToClassId = new HashMap();
    final Map classIdToClassName = new HashMap();

    for (int i = 0; i < numOfGeneratedClasses; i++) {
      final String className = "com.xxx.SomeClassName" + i;
      final PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
          .createState(new ObjectID(1), ObjectID.NULL_ID, className, loaderDesc, cursor);
      state.apply(this.objectID, cursor, new ApplyTransactionInfo());

      final int classId = state.getClassId();
      assertTrue(classId != 0);
      assertEquals(state.getClassName(), className);
      assertEquals(state.getLoaderDescription(), loaderDesc);

      classNameToClassId.put(className, new Integer(classId));
      assertNull(classIdToClassName.get(new Integer(classId)));
      classIdToClassName.put(new Integer(classId), className);
    }

    for (int i = 0; i < numOfGeneratedClasses; i++) {
      final String className = "com.xxx.SomeClassName" + i;
      final PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
          .createState(new ObjectID(1), ObjectID.NULL_ID, className, loaderDesc, cursor);
      state.apply(this.objectID, cursor, new ApplyTransactionInfo());

      final int classId = state.getClassId();

      final Integer storedClassId = (Integer) classNameToClassId.get(className);
      assertEquals(classId, storedClassId.intValue());

      final String storedClassName = (String) classIdToClassName.get(new Integer(classId));
      assertEquals(className, storedClassName);

    }
  }

  public void testPhysicalManagedObjectState() throws Exception {
    final String loaderDesc = "System.loader";

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(this.listenerProvider, new InMemoryPersistor());

    final HashMap types = new HashMap();
    types.put("field1", new ObjectID(1));
    types.put("field2", new Boolean(true));
    types.put("field3", new Character('c'));
    types.put("field4", new Long(5));
    types.put("field5", new Integer(15));
    types.put("field6", new String("neoistheone"));
    types.put("field7", new Short((short) 606));
    types.put("field8", new Double(98.76d));
    types.put("field9", new Float(8.8f));
    types.put("field10", new StackTraceElement("classname", "methodname", "filename", 123));

    TestDNACursor cursor = new TestDNACursor();
    for (final Iterator i = types.entrySet().iterator(); i.hasNext();) {
      final Map.Entry element = (Map.Entry) i.next();
      cursor.addPhysicalAction((String) element.getKey(), element.getValue(), true);
    }

    final PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(new ObjectID(1), ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor);
    state.apply(this.objectID, cursor, new ApplyTransactionInfo());

    assertTrue(state.getClassId() != 0);
    assertEquals(state.getClassName(), "com.xxx.SomeClassName");
    assertEquals(state.getLoaderDescription(), loaderDesc);

    Collection references = state.getObjectReferences();
    assertEquals(1, references.size());
    assertEquals(new ObjectID(1), references.iterator().next());

    Map values = state.addValues(new HashMap());
    assertEquals(types.size(), values.size());
    assertEquals(new ObjectID(1), values.get("field1"));

    Boolean l2 = (Boolean) values.get("field2");
    assertTrue(l2.booleanValue());
    Character f3 = (Character) values.get("field3");
    assertEquals(f3.charValue(), 'c');

    System.err.println("values =" + values);
    assertEquals(types, values);

    cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(2), true);
    cursor.addPhysicalAction("field2", new Boolean(false), true);
    cursor.addPhysicalAction("field3", new Character('d'), true);

    state.apply(this.objectID, cursor, new ApplyTransactionInfo());

    references = state.getObjectReferences();
    assertEquals(1, references.size());
    assertEquals(new ObjectID(2), references.iterator().next());

    values = state.addValues(new HashMap());
    assertEquals(types.size(), values.size());
    assertEquals(new ObjectID(2), values.get("field1"));

    l2 = (Boolean) values.get("field2");
    assertFalse(l2.booleanValue());
    f3 = (Character) values.get("field3");
    assertEquals(f3.charValue(), 'd');

    assertEquals(ObjectID.NULL_ID, state.getParentID());
  }

  public void testPhysicalMOStateClassInCompatibility() throws Exception {
    final String loaderDesc = "System.loader";

    final InMemoryPersistor persistor = new InMemoryPersistor();
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(this.listenerProvider, persistor);

    TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(1), true);
    cursor.addPhysicalAction("field2", new Long(11), true);
    cursor.addPhysicalAction("field3", new String("neoistheone"), true);

    final PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(this.objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor);
    state.apply(this.objectID, cursor, new ApplyTransactionInfo());

    assertTrue(state.getClassId() != 0);
    assertEquals(state.getClassName(), "com.xxx.SomeClassName");
    assertEquals(state.getLoaderDescription(), loaderDesc);

    Collection references = state.getObjectReferences();
    assertEquals(1, references.size());
    assertEquals(new ObjectID(1), references.iterator().next());

    Map values = state.addValues(new HashMap());
    assertEquals(3, values.size());
    assertEquals(new ObjectID(1), values.get("field1"));

    Long l2 = (Long) values.get("field2");
    assertEquals(11, l2.longValue());
    String f3 = (String) values.get("field3");
    assertEquals(f3, "neoistheone");

    cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(2), true);
    // Newly added fields
    cursor.addPhysicalAction("field4", new Boolean(false), true);
    cursor.addPhysicalAction("field5", new ObjectID(22), true);

    try {
      state.apply(this.objectID, cursor, new ApplyTransactionInfo());
      assertTrue(false);
    } catch (final ClassNotCompatableException cfe) {
      // expected
    }

    // recreate the state object
    final PhysicalManagedObjectState state1 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .recreateState(this.objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor, state);
    state1.apply(this.objectID, cursor, new ApplyTransactionInfo());

    assertTrue(state1.getClassId() != state.getClassId());
    assertEquals(state1.getClassName(), "com.xxx.SomeClassName");
    assertEquals(state1.getLoaderDescription(), loaderDesc);

    references = state1.getObjectReferences();
    assertEquals(2, references.size());
    final Set expectedRefs = new HashSet();
    expectedRefs.add(new ObjectID(2));
    expectedRefs.add(new ObjectID(22));

    values = state1.addValues(new HashMap());
    assertEquals(new ObjectID(2), values.get("field1"));
    l2 = (Long) values.get("field2");
    assertEquals(11, l2.longValue());
    f3 = (String) values.get("field3");
    assertEquals(f3, "neoistheone");
    Boolean f4 = (Boolean) values.get("field4");
    assertEquals(f4, new Boolean(false));
    assertEquals(new ObjectID(22), values.get("field5"));

    // Now try applying yet another new field on the old object
    TestDNACursor cursor2 = new TestDNACursor();
    cursor2.addPhysicalAction("field6", new Integer(2), false);

    try {
      state.apply(this.objectID, cursor2, new ApplyTransactionInfo());
      assertTrue(false);
    } catch (final ClassNotCompatableException cfe) {
      // expected
    }

    // recreate the state object, even though we pass old state object, it should extend latest state object
    final PhysicalManagedObjectState state2 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .recreateState(this.objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor2, state);
    state2.apply(this.objectID, cursor2, new ApplyTransactionInfo());
    cursor.reset();
    state2.apply(this.objectID, cursor, new ApplyTransactionInfo());

    assertTrue(state1.getClassId() != state2.getClassId());
    assertEquals(state2.getClassName(), "com.xxx.SomeClassName");
    assertEquals(state2.getLoaderDescription(), loaderDesc);

    values = state2.addValues(new HashMap());
    assertEquals(new ObjectID(2), values.get("field1"));
    l2 = (Long) values.get("field2");
    assertEquals(11, l2.longValue());
    f3 = (String) values.get("field3");
    assertEquals(f3, "neoistheone");
    f4 = (Boolean) values.get("field4");
    assertEquals(f4, new Boolean(false));
    assertEquals(new ObjectID(22), values.get("field5"));
    Integer i6 = (Integer) values.get("field6");
    assertEquals(2, i6.intValue());

    assertEquals(state1.getClass().getName(), state2.getClass().getSuperclass().getName());

    // The same field is changed from Integer to Long, this is not support.
    cursor2 = new TestDNACursor();
    cursor2.addPhysicalAction("field6", new Long(2), true);
    try {
      // We print the exception but dont throw it.
      state2.apply(this.objectID, cursor2, new ApplyTransactionInfo());
      System.err.println("The above exception is NORMAL.");
    } catch (final ClassCastException cfe) {
      assertTrue(false);
    }

    values = state2.addValues(new HashMap());
    i6 = (Integer) values.get("field6");
    assertEquals(2, i6.intValue());

    // Try to create a new State Object
    cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(1), true);
    cursor.addPhysicalAction("field2", new Long(11), true);
    cursor.addPhysicalAction("field3", new String("neoistheone"), true);

    PhysicalManagedObjectState state3 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(this.objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor);
    state3.apply(this.objectID, cursor, new ApplyTransactionInfo());
    assertEquals(state2.getClass().getName(), state3.getClass().getName());

    // RESTART Scenario... still only the new object type should be used
    cursor.reset();
    ManagedObjectStateFactory.createInstance(this.listenerProvider, persistor);
    state3 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance().createState(this.objectID,
                                                                                              ObjectID.NULL_ID,
                                                                                              "com.xxx.SomeClassName",
                                                                                              loaderDesc, cursor);
    state3.apply(this.objectID, cursor, new ApplyTransactionInfo());
    assertEquals(state2.getClass().getName(), state3.getClass().getName());

    assertEquals(ObjectID.NULL_ID, state.getParentID());
  }

  public void testStaticHasNoReference() {

    final ArrayManagedObjectState arrayManagedObjectState = new ArrayManagedObjectState(1);
    final ManagedObjectStateTest[] objArray = new ManagedObjectStateTest[] {};
    arrayManagedObjectState.initArray(objArray);
    assertFalse(arrayManagedObjectState.hasNoReferences());
    arrayManagedObjectState.initArray(new byte[] {});
    assertTrue(arrayManagedObjectState.hasNoReferences());

    final LiteralTypesManagedObjectState literalTypesManagedObjectState = new LiteralTypesManagedObjectState();
    assertTrue(literalTypesManagedObjectState.hasNoReferences());

    final DateManagedObjectState dateManagedObjectState = new DateManagedObjectState(1);
    assertTrue(dateManagedObjectState.hasNoReferences());

    final URLManagedObjectState urlManagedObjectState = new URLManagedObjectState(1);
    assertTrue(urlManagedObjectState.hasNoReferences());

    final TDCSerializedEntryManagedObjectState tcHibernateSerializedEntryManagedObjectState = new TDCSerializedEntryManagedObjectState(
                                                                                                                                       1);
    assertTrue(tcHibernateSerializedEntryManagedObjectState.hasNoReferences());
  }

}
