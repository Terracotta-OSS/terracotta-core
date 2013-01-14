/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.type;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;

import com.terracotta.toolkit.object.TCToolkitObject;

public interface IsolatedClusteredObjectLookup<S extends TCToolkitObject> {

  S lookupClusteredObject(String name, ToolkitObjectType type, Configuration config);
}
