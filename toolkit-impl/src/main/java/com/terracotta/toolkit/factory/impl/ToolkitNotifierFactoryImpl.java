/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.events.DestroyableToolkitNotifier;
import com.terracotta.toolkit.events.ToolkitNotifierImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ClusteredListFactory}
 */
public class ToolkitNotifierFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitNotifier, ToolkitNotifierImpl> {

  private static final NotifierIsolatedTypeFactory FACTORY = new NotifierIsolatedTypeFactory();

  public ToolkitNotifierFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_NOTIFIER_ROOT_NAME,
                                                                FACTORY));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.NOTIFIER;
  }

  private static class NotifierIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitNotifier, ToolkitNotifierImpl> {

    @Override
    public ToolkitNotifier createIsolatedToolkitType(ToolkitObjectFactory factory, String name, Configuration config,
                                                     ToolkitNotifierImpl tcClusteredObject) {
      return new DestroyableToolkitNotifier(factory, tcClusteredObject, name);
    }

    @Override
    public ToolkitNotifierImpl createTCClusteredObject(Configuration config) {
      return new ToolkitNotifierImpl();
    }

  }

}