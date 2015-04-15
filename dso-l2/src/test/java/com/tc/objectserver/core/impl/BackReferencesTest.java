/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
