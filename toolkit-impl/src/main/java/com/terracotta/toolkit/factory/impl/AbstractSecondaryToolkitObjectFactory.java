/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;

public abstract class AbstractSecondaryToolkitObjectFactory<T extends ToolkitObject> implements ToolkitObjectFactory<T> {

  @Override
  public void destroy(T toolkitObject) {
    // destroy is implemented by Secondary type itself, and no state created by factory
    throw new UnsupportedOperationException();
  }

  @Override
  public void applyDestroy(T toolkitObject) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void lock(ToolkitObject obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlock(ToolkitObject obj) {
    throw new UnsupportedOperationException();
  }

}
