/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.object.Destroyable;
import org.terracotta.toolkit.object.ToolkitObject;

public interface DestroyableToolkitObject extends Destroyable, ToolkitObject {

  /**
   * Do the needful for removing this object from the cluster
   */
  public void doDestroy();
}
