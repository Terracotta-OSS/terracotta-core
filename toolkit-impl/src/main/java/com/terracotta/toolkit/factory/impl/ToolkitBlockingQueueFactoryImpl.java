/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.DestroyableToolkitList;
import com.terracotta.toolkit.collections.ToolkitBlockingQueueImpl;
import com.terracotta.toolkit.collections.ToolkitListImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

public class ToolkitBlockingQueueFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitBlockingQueueImpl, ToolkitObjectStripe<ToolkitListImpl>> {

  public static final String              CAPACITY_FIELD_NAME = "capacity";
  private static final CBQIsolatedFactory FACTORY             = new CBQIsolatedFactory();

  public ToolkitBlockingQueueFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_BLOCKING_QUEUE_ROOT_NAME, FACTORY,
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.BLOCKING_QUEUE;
  }

  private static class CBQIsolatedFactory implements
      IsolatedToolkitTypeFactory<ToolkitBlockingQueueImpl, ToolkitObjectStripe<ToolkitListImpl>> {

    @Override
    public ToolkitBlockingQueueImpl createIsolatedToolkitType(ToolkitObjectFactory<ToolkitBlockingQueueImpl> factory,
                                                              String name, Configuration config,
                                                              ToolkitObjectStripe<ToolkitListImpl> tcClusteredObject) {
      int actualCapacity = assertConfig(name, config, tcClusteredObject);
      DestroyableToolkitList listWrapper = new DestroyableToolkitList(factory, tcClusteredObject.iterator().next(),
                                                                      name);

      return new ToolkitBlockingQueueImpl(factory, listWrapper, actualCapacity);
    }

    @Override
    public ToolkitObjectStripeImpl<ToolkitListImpl> createTCClusteredObject(Configuration config) {
      ToolkitListImpl[] components = new ToolkitListImpl[] { new ToolkitListImpl() };
      return new ToolkitObjectStripeImpl<ToolkitListImpl>(config, components);
    }

    private static int assertConfig(String name, Configuration config, ToolkitObjectStripe coStripe) {
      int actualCapacity = coStripe.getConfiguration().getInt(CAPACITY_FIELD_NAME);
      int expectedCapacity = config.getInt(CAPACITY_FIELD_NAME);
      if (actualCapacity != expectedCapacity) { throw new IllegalArgumentException(
                                                                                   "A "
                                                                                       + ToolkitBlockingQueue.class
                                                                                           .getSimpleName()
                                                                                       + " with name '"
                                                                                       + name
                                                                                       + "' already exists with different capacity - "
                                                                                       + actualCapacity
                                                                                       + ", requested capacity - "
                                                                                       + expectedCapacity); }
      return actualCapacity;
    }

  }
}
