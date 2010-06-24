/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.test.TCTestCase;

import java.util.HashSet;
import java.util.Set;

public class BackReferencesTest extends TCTestCase {

  public void testBasic() throws Exception {
    ApplyTransactionInfo br = new ApplyTransactionInfo();
    assertTrue(br.getAllParents().isEmpty());
    
    Set actualParents = new HashSet();
    Set actualRefChildren = new HashSet();
    
    // 1 -> 2
    ObjectID p = new ObjectID(1);
    ObjectID c = new ObjectID(2);
    
    actualParents.add(p);
    actualRefChildren.add(c);
    
    br.addBackReference(c, p);
    Set parents = br.getAllParents();
    assertEquals(actualParents, parents);
    Set referencedChildren = br.addReferencedChildrenTo(new HashSet(), parents);
    assertEquals(actualRefChildren, referencedChildren);
    
    // 3 ->5  4 ->5
    p = new ObjectID(3);
    c = new ObjectID(5);
    
    actualParents.add(p);
    br.addBackReference(c, p);
    
    p = new ObjectID(4);
    actualParents.add(p);
    br.addBackReference(c, p);
    
    parents = br.getAllParents();
    assertEquals(actualParents, parents);
    
    Set interestedParents = new HashSet();
    interestedParents.add(new ObjectID(1));
    
    referencedChildren = br.addReferencedChildrenTo(new HashSet(), interestedParents);
    assertEquals(actualRefChildren, referencedChildren);
    
    interestedParents.add(new ObjectID(3));
    actualRefChildren.add(new ObjectID(5));
    
    referencedChildren = br.addReferencedChildrenTo(new HashSet(), interestedParents);
    assertEquals(actualRefChildren, referencedChildren);
    
    
    // 5 -> 1
    p = new ObjectID(5);
    c = new ObjectID(1);
    
    actualParents.add(p);
    br.addBackReference(c, p);
    
    parents = br.getAllParents();
    assertEquals(actualParents, parents);
    
    interestedParents = new HashSet();
    interestedParents.add(new ObjectID(3));
    actualRefChildren = new HashSet();
    actualRefChildren.add(new ObjectID(5));
    actualRefChildren.add(new ObjectID(1));
    actualRefChildren.add(new ObjectID(2));
    
    referencedChildren = br.addReferencedChildrenTo(new HashSet(), interestedParents);
    assertEquals(actualRefChildren, referencedChildren);
    
  }
}
