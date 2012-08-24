/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.factory.impl.AbstractPrimaryToolkitObjectFactory;

public abstract class AbstractDestroyableToolkitObject<T extends ToolkitObject> implements DestroyableToolkitObject {

  protected final AbstractPrimaryToolkitObjectFactory factory;
  private final DestroyApplicator                     destroyApplicator;
  private volatile boolean                            destroyed;

  public AbstractDestroyableToolkitObject(ToolkitObjectFactory<T> factory) {
    if (!(factory instanceof AbstractPrimaryToolkitObjectFactory)) { throw new IllegalStateException(); }

    this.factory = (AbstractPrimaryToolkitObjectFactory) factory;
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
    factory.destroy(this);
  }

  public void destroyFromCluster() {
    doDestroy();
    destroyed = true;
  }

  /**
   * After destroy
   */
  public abstract void applyDestroy();

  private static class DestroyApplicatorImpl implements DestroyApplicator {

    private final AbstractDestroyableToolkitObject toolkitObject;

    public DestroyApplicatorImpl(AbstractDestroyableToolkitObject toolkitObject) {
      this.toolkitObject = toolkitObject;
    }

    @Override
    public void applyDestroy() {
      toolkitObject.destroyed = true;
      toolkitObject.factory.applyDestroy(toolkitObject);
      toolkitObject.applyDestroy();
    }

    @Override
    public void setApplyDestroyCallback(DestroyApplicator applyDestroyCallback) {
      // no-op
    }

  }

}
