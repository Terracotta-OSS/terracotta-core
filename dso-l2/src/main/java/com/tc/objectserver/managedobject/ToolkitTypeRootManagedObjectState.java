/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
