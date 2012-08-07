/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import junit.framework.TestCase;

public class ManagedObjectStateTest extends TestCase {

  public void testStaticHasNoReference() {

    final ArrayManagedObjectState arrayManagedObjectState = new ArrayManagedObjectState(1);
    final ManagedObjectStateTest[] objArray = new ManagedObjectStateTest[] {};
    arrayManagedObjectState.initArray(objArray);
    assertFalse(arrayManagedObjectState.hasNoReferences());
    arrayManagedObjectState.initArray(new byte[] {});
    assertTrue(arrayManagedObjectState.hasNoReferences());

    final LiteralTypesManagedObjectState literalTypesManagedObjectState = new LiteralTypesManagedObjectState();
    assertTrue(literalTypesManagedObjectState.hasNoReferences());

    final TDCSerializedEntryManagedObjectState tcHibernateSerializedEntryManagedObjectState = new TDCSerializedEntryManagedObjectState(
                                                                                                                                       1);
    assertTrue(tcHibernateSerializedEntryManagedObjectState.hasNoReferences());
  }

}
