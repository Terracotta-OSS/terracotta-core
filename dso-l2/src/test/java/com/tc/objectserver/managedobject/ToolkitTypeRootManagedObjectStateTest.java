package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.PersistentObjectFactory;

/**
 * @author nishant
 */
public class ToolkitTypeRootManagedObjectStateTest extends MapManagedObjectStateTest {
  @Override
  protected boolean isInlineDGCSupported() {
    return false;
  }

  @Override
  protected MapManagedObjectState createManagedObjectState(long classID, ObjectID id, PersistentObjectFactory factory) {
    return new ToolkitTypeRootManagedObjectState(classID, id, factory);
  }
}
