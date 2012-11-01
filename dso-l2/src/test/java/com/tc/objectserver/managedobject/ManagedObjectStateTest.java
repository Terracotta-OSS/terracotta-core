/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import junit.framework.TestCase;

public class ManagedObjectStateTest extends TestCase {

  public void testStaticHasNoReference() {

    final TDCSerializedEntryManagedObjectState tcHibernateSerializedEntryManagedObjectState = new TDCSerializedEntryManagedObjectState(
                                                                                                                                       1);
    assertTrue(tcHibernateSerializedEntryManagedObjectState.hasNoReferences());
  }

}
