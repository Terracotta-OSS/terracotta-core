/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.type;

import org.terracotta.toolkit.config.Configuration;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;

public interface IsolatedToolkitTypeFactory<T extends RejoinAwareToolkitObject, S extends TCToolkitObject> {

  /**
   * Used to create the unclustered type after faulting in the TCClusteredObject
   */
  T createIsolatedToolkitType(ToolkitObjectFactory<T> factory, IsolatedClusteredObjectLookup<S> lookup, String name,
                              Configuration config, S tcClusteredObject);

  /**
   * Used to create the TCClusteredObject to back the type
   */
  S createTCClusteredObject(Configuration config);

}
