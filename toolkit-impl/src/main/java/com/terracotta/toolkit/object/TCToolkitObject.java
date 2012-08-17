/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.object.Destroyable;

import com.tc.object.bytecode.Manageable;

/**
 * An object that can be clustered. The clustered object becomes a part of the cluster when its added to a root or an
 * existing clustered object graph, typically is added to a ToolkitRoot.
 */
public interface TCToolkitObject extends Manageable, Destroyable, DestroyApplicator {

  /**
   * Do necessary cleanup on destroy
   */
  void cleanupOnDestroy();

}
