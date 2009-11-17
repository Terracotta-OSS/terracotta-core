/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;

public class PhysicalManagedObjectStateTest extends AbstractTestManagedObjectState {
  
  public void testObjectPhysical() throws Exception {
    String className = "com.tc.objectserver.managedobject.PhysicalManagedObjectStateTest";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction("field1", new ObjectID(2002), true);
    cursor.addPhysicalAction("field2", new ObjectID(2003), true);
    cursor.addPhysicalAction("field3", new Integer(33), false);

    basicTestUnit(className, ManagedObjectState.PHYSICAL_TYPE, cursor, 2);
  }
  
  public void testObjectPhysicalHasReferences() throws Exception {
    String className = "com.tc.objectserver.managedobject.PhysicalManagedObjectStateTest";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction("field1", new ObjectID(2002), true);
    cursor.addPhysicalAction("field2", new ObjectID(2003), true);
    cursor.addPhysicalAction("field3", new Integer(33), false);


    
    ManagedObjectState state = createManagedObjectState(className, cursor);
    System.out.println("state: " + state);
    assertFalse(state.hasNoReferences());
  }
  
  public void testObjectPhysicalHasNoReferences() throws Exception {
    String className = "com.tc.objectserver.managedobject.PhysicalManagedObjectStateTest";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction("field1", new Integer(2002), false);
    cursor.addPhysicalAction("field2", new Integer(2003), false);
    cursor.addPhysicalAction("field3", new Integer(33), false);


    
    ManagedObjectState state = createManagedObjectState(className, cursor);
    System.out.println("state: " + state);
    assertTrue(state.hasNoReferences());
  }


  
}
