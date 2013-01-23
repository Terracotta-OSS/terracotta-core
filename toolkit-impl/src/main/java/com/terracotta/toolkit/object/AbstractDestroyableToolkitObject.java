/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.factory.impl.AbstractPrimaryToolkitObjectFactory;
import com.terracotta.toolkit.rejoin.RejoinCallback;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDestroyableToolkitObject<T extends ToolkitObject> implements DestroyableToolkitObject,
    RejoinCallback {

  protected final AbstractPrimaryToolkitObjectFactory factory;
  private final DestroyApplicator                     destroyApplicator;
  private volatile boolean                            destroyed;
  private final AtomicBoolean                         rejoinInProgress = new AtomicBoolean(false);

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
    if (rejoinInProgress.get()) { throw new RejoinException("Cannot destroy object with name: '" + getName()
                                                            + "' as rejoin is in progress. (type: "
                                                            + getClass().getName() + ")"); }
    factory.destroy(this);
  }

  @Override
  public final void rejoinStarted() {
    rejoinInProgress.set(true);
    doRejoinStarted();
  }

  @Override
  public final void rejoinCompleted() {
    rejoinInProgress.set(false);
    doRejoinCompleted();
  }

  protected abstract void doRejoinStarted();

  protected abstract void doRejoinCompleted();

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
