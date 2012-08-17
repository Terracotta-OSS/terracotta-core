/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;

public abstract class AbstractDestroyableToolkitObject<T extends ToolkitObject> implements DestroyableToolkitObject {

  protected final ToolkitObjectFactory<T> factory;
  private final DestroyApplicator         destroyApplicator;
  private volatile boolean                destroyed;

  public AbstractDestroyableToolkitObject(ToolkitObjectFactory<T> factory) {
    this.factory = factory;
    this.destroyApplicator = new DestroyApplicatorImpl(this);
  }

  public final DestroyApplicator getDestroyApplicator() {
    return destroyApplicator;
  }

  @Override
  public final boolean isDestroyed() {
    return destroyed;
  }

  @Override
  public final void destroy() {
    factory.lock(this);
    try {
      if (!destroyed) {
        factory.destroy((T) this);
        destroyed = true;
      }
    } finally {
      factory.unlock(this);
    }
  }

  /**
   * After destroy
   */
  public abstract void afterDestroy();

  private static class DestroyApplicatorImpl implements DestroyApplicator {

    private final AbstractDestroyableToolkitObject toolkitObject;

    public DestroyApplicatorImpl(AbstractDestroyableToolkitObject toolkitObject) {
      this.toolkitObject = toolkitObject;
    }

    @Override
    public void applyDestroy() {
      toolkitObject.destroyed = true;
      toolkitObject.factory.applyDestroy(toolkitObject);
      toolkitObject.afterDestroy();
    }

    @Override
    public void setApplyDestroyCallback(DestroyApplicator applyDestroyCallback) {
      // no-op
    }

  }

}
