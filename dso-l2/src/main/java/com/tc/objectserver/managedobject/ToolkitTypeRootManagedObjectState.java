/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Map;

public class ToolkitTypeRootManagedObjectState extends PartialMapManagedObjectState {
  public ToolkitTypeRootManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected ToolkitTypeRootManagedObjectState(final long classID, final Map map) {
    super(classID, map);
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

  static ToolkitTypeRootManagedObjectState readFrom(final ObjectInput in) throws IOException {
    ToolkitTypeRootManagedObjectState ttrmo = new ToolkitTypeRootManagedObjectState(in);
    return ttrmo;
  }

}
