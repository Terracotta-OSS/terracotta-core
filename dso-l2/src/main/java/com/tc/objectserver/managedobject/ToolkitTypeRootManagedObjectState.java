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

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.PersistentObjectFactory;

import java.io.IOException;
import java.io.ObjectInput;

public class ToolkitTypeRootManagedObjectState extends PartialMapManagedObjectState {
  public ToolkitTypeRootManagedObjectState(ObjectInput in, PersistentObjectFactory factory) throws IOException {
    super(in, factory);
  }

  protected ToolkitTypeRootManagedObjectState(final long classID, ObjectID oid, PersistentObjectFactory factory) {
    super(classID, oid, factory);
  }

  @Override
  public byte getType() {
    return ManagedObjectStateStaticConfig.TOOLKIT_TYPE_ROOT.getStateObjectType();
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    // Nothing to add since nothing is required to be faulted in the L1
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    ToolkitTypeRootManagedObjectState mo = (ToolkitTypeRootManagedObjectState) o;
    return super.basicEquals(mo);
  }

  static ToolkitTypeRootManagedObjectState readFrom(final ObjectInput in, PersistentObjectFactory factory) throws IOException {
    ToolkitTypeRootManagedObjectState ttrmo = new ToolkitTypeRootManagedObjectState(in, factory);
    return ttrmo;
  }

}
