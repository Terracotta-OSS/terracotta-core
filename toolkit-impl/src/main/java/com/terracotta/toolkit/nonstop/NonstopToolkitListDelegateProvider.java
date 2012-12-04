/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.abortable.AbortableOperationManager;

import java.util.concurrent.FutureTask;

public class NonstopToolkitListDelegateProvider<E> extends AbstractNonStopDelegateProvider<ToolkitList<E>> {

  private final String        name;
  private final Class<E>      klazz;

  public NonstopToolkitListDelegateProvider(AbortableOperationManager abortableOperationManager,
                                             NonStopConfigRegistryImpl nonStopConfigRegistry,
                                             NonstopTimeoutBehaviorResolver behaviorResolver,
                                             FutureTask<ToolkitInternal> toolkitDelegateFutureTask, String name,
                                            Class<E> klazz) {
    super(toolkitDelegateFutureTask, abortableOperationManager, nonStopConfigRegistry, behaviorResolver, name);
    this.name = name;
    this.klazz = klazz;
  }

  @Override
  public ToolkitList<E> getToolkitObject() {
    return getToolkit().getList(name, klazz);
  }

  @Override
  public ToolkitObjectType getToolkitObjectType() {
    return ToolkitObjectType.LIST;
  }

}
