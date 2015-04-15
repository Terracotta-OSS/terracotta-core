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
package com.tc.objectserver.managedobject;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

public class ListManagedObjectStateTest extends AbstractTestManagedObjectState {

  // override due to difference on dehydrate
  @Override
  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }

  public void testObjectList1() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2003) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 2);
  }

  public void testObjectList2() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(LogicalOperation.ADD_AT, new Object[] { Integer.valueOf(1), new ObjectID(1000) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 3);
  }

  public void testObjectList3() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    for (int i = 0; i < 1000; ++i) {
      cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(1000 + i) });
    }
    cursor.addLogicalAction(LogicalOperation.CLEAR, null);

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 0);
  }

  public void testObjectList4() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(LogicalOperation.REMOVE, new Object[] { new ObjectID(2004) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 2);
  }

}
