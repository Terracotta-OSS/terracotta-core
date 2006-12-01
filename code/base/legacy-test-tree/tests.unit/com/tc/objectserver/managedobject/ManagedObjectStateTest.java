/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.managedobject.bytecode.ClassNotCompatableException;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;

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

  public void setUp() throws Exception {
    objectID = new ObjectID(2002);
    listenerProvider = new NullManagedObjectChangeListenerProvider();
  }

  public void testPhysicalManagedObjectClassId() throws Exception {
    String loaderDesc = "System.loader";

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(listenerProvider, new InMemoryPersistor());

    HashMap types = new HashMap();
    types.put("field1", new ObjectID(1));
    types.put("field2", new Boolean(true));
    types.put("field3", new Character('c'));
    types.put("field4", new Long(5));
    types.put("field5", new Integer(15));
    types.put("field6", new String("neoistheone"));
    types.put("field7", new Short((short) 606));
    types.put("field8", new Double(98.76d));
    types.put("field9", new Float(8.8f));
    // Not supported in 1.4
    // types.put("field10", new StackTraceElement("classname", "methodname", "filename", 123));

    TestDNACursor cursor = new TestDNACursor();
    for (Iterator i = types.entrySet().iterator(); i.hasNext();) {
      Map.Entry element = (Map.Entry) i.next();
      cursor.addPhysicalAction((String) element.getKey(), element.getValue());
    }

    int numOfGeneratedClasses = 10000;
    Map classNameToClassId = new HashMap();
    Map classIdToClassName = new HashMap();

    for (int i = 0; i < numOfGeneratedClasses; i++) {
      String className = "com.xxx.SomeClassName" + i;
      PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
          .createState(new ObjectID(1), ObjectID.NULL_ID, className, loaderDesc, cursor);
      state.apply(objectID, cursor, new BackReferences());

      int classId = state.getClassId();
      assertTrue(classId != 0);
      assertEquals(state.getClassName(), className);
      assertEquals(state.getLoaderDescription(), loaderDesc);
      
      classNameToClassId.put(className, new Integer(classId));
      assertNull(classIdToClassName.get(new Integer(classId)));
      classIdToClassName.put(new Integer(classId), className);
    }
    
    for (int i = 0; i < numOfGeneratedClasses; i++) {
      String className = "com.xxx.SomeClassName" + i;
      PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
          .createState(new ObjectID(1), ObjectID.NULL_ID, className, loaderDesc, cursor);
      state.apply(objectID, cursor, new BackReferences());

      int classId = state.getClassId();

      Integer storedClassId = (Integer)classNameToClassId.get(className);
      assertEquals(classId, storedClassId.intValue());
      
      String storedClassName = (String)classIdToClassName.get(new Integer(classId));
      assertEquals(className, storedClassName);
      
    }
  }

  public void testPhysicalManagedObjectState() throws Exception {
    String loaderDesc = "System.loader";

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(listenerProvider, new InMemoryPersistor());

    HashMap types = new HashMap();
    types.put("field1", new ObjectID(1));
    types.put("field2", new Boolean(true));
    types.put("field3", new Character('c'));
    types.put("field4", new Long(5));
    types.put("field5", new Integer(15));
    types.put("field6", new String("neoistheone"));
    types.put("field7", new Short((short) 606));
    types.put("field8", new Double(98.76d));
    types.put("field9", new Float(8.8f));
    // Not supported in 1.4
    // types.put("field10", new StackTraceElement("classname", "methodname", "filename", 123));

    TestDNACursor cursor = new TestDNACursor();
    for (Iterator i = types.entrySet().iterator(); i.hasNext();) {
      Map.Entry element = (Map.Entry) i.next();
      cursor.addPhysicalAction((String) element.getKey(), element.getValue());
    }

    PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(new ObjectID(1), ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor);
    state.apply(objectID, cursor, new BackReferences());

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
    cursor.addPhysicalAction("field1", new ObjectID(2));
    cursor.addPhysicalAction("field2", new Boolean(false));
    cursor.addPhysicalAction("field3", new Character('d'));

    state.apply(objectID, cursor, new BackReferences());

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
    String loaderDesc = "System.loader";

    InMemoryPersistor persistor = new InMemoryPersistor();
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(listenerProvider, persistor);

    TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(1));
    cursor.addPhysicalAction("field2", new Long(11));
    cursor.addPhysicalAction("field3", new String("neoistheone"));

    PhysicalManagedObjectState state = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor);
    state.apply(objectID, cursor, new BackReferences());

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
    cursor.addPhysicalAction("field1", new ObjectID(2));
    // Newly added fields
    cursor.addPhysicalAction("field4", new Boolean(false));
    cursor.addPhysicalAction("field5", new ObjectID(22));

    try {
      state.apply(objectID, cursor, new BackReferences());
      assertTrue(false);
    } catch (ClassNotCompatableException cfe) {
      // expected
    }

    // recreate the state object
    PhysicalManagedObjectState state1 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .recreateState(objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor, state);
    state1.apply(objectID, cursor, new BackReferences());

    assertTrue(state1.getClassId() != state.getClassId());
    assertEquals(state1.getClassName(), "com.xxx.SomeClassName");
    assertEquals(state1.getLoaderDescription(), loaderDesc);

    references = state1.getObjectReferences();
    assertEquals(2, references.size());
    Set expectedRefs = new HashSet();
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
      state.apply(objectID, cursor2, new BackReferences());
      assertTrue(false);
    } catch (ClassNotCompatableException cfe) {
      // expected
    }

    // recreate the state object, even though we pass old state object, it should extend latest state object
    PhysicalManagedObjectState state2 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .recreateState(objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor2, state);
    state2.apply(objectID, cursor2, new BackReferences());
    cursor.reset();
    state2.apply(objectID, cursor, new BackReferences());

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
    cursor2.addPhysicalAction("field6", new Long(2));
    try {
      // We print the exception but dont throw it.
      state2.apply(objectID, cursor2, new BackReferences());
      System.err.println("The above exception is NORMAL.");
    } catch (ClassCastException cfe) {
      assertTrue(false);
    }

    values = state2.addValues(new HashMap());
    i6 = (Integer) values.get("field6");
    assertEquals(2, i6.intValue());

    // Try to create a new State Object
    cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(1));
    cursor.addPhysicalAction("field2", new Long(11));
    cursor.addPhysicalAction("field3", new String("neoistheone"));

    PhysicalManagedObjectState state3 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(objectID, ObjectID.NULL_ID, "com.xxx.SomeClassName", loaderDesc, cursor);
    state3.apply(objectID, cursor, new BackReferences());
    assertEquals(state2.getClass().getName(), state3.getClass().getName());

    // RESTART Scenario... still only the new object type should be used
    cursor.reset();
    ManagedObjectStateFactory.createInstance(listenerProvider, persistor);
    state3 = (PhysicalManagedObjectState) ManagedObjectStateFactory.getInstance().createState(objectID,
                                                                                              ObjectID.NULL_ID,
                                                                                              "com.xxx.SomeClassName",
                                                                                              loaderDesc, cursor);
    state3.apply(objectID, cursor, new BackReferences());
    assertEquals(state2.getClass().getName(), state3.getClass().getName());

    assertEquals(ObjectID.NULL_ID, state.getParentID());
  }

}
