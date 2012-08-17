/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.DestroyableToolkitObject;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.AggregateToolkitTypeRoot;

public abstract class AbstractPrimaryToolkitObjectFactory<T extends ToolkitObject, S extends TCToolkitObject>
    implements ToolkitObjectFactory<T> {

  protected final AggregateToolkitTypeRoot<T, S> aggregateRootMap;
  protected final ToolkitInternal                toolkit;

  public AbstractPrimaryToolkitObjectFactory(ToolkitInternal toolkit, AggregateToolkitTypeRoot<T, S> aggregateRootMap) {
    this.aggregateRootMap = aggregateRootMap;
    this.toolkit = toolkit;
  }

  @Override
  public final T getOrCreate(String name, Configuration config) {
    return aggregateRootMap.getOrCreateToolkitType(toolkit, this, name, config);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void destroy(ToolkitObject obj) {
    DestroyableToolkitObject destroyableToolkitObject = (DestroyableToolkitObject) obj;
    final ToolkitObjectType type = getManufacturedToolkitObjectType();
    aggregateRootMap.lock(type, obj.getName());
    try {
      aggregateRootMap.removeToolkitType(type, obj.getName());
      destroyableToolkitObject.doDestroy();
    } finally {
      aggregateRootMap.unlock(type, obj.getName());
    }
  }

  @Override
  public void lock(ToolkitObject obj) {
    aggregateRootMap.lock(getManufacturedToolkitObjectType(), obj.getName());
  }

  @Override
  public void unlock(ToolkitObject obj) {
    aggregateRootMap.unlock(getManufacturedToolkitObjectType(), obj.getName());
  }

  public final void applyDestroy(ToolkitObject obj) {
    aggregateRootMap.applyDestroy(obj.getName());
  }
}