/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.object.ToolkitObjectType;

public interface ToolkitObjectFactory<T extends ToolkitObject> {

  T getOrCreate(String name, Configuration config);

  void destroy(T toolkitObject);

  void applyDestroy(T toolkitObject);

  ToolkitObjectType getManufacturedToolkitObjectType();

  void lock(ToolkitObject obj);

  void unlock(ToolkitObject obj);
}
