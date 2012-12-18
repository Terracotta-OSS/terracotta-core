/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.object.ToolkitObject;

public interface ToolkitObjectLookup<T extends ToolkitObject> {
  public T getInitializedObject();

  public T getInitializedObjectOrNull();
}
