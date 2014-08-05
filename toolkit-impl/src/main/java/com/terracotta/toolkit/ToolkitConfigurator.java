/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.terracotta.toolkit.collections.ToolkitListImpl;
import com.terracotta.toolkit.collections.ToolkitListImplApplicator;
import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.ServerMapApplicator;
import com.terracotta.toolkit.collections.map.ToolkitMapImpl;
import com.terracotta.toolkit.collections.map.ToolkitMapImplApplicator;
import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.collections.map.ToolkitSortedMapImplApplicator;
import com.terracotta.toolkit.events.ToolkitNotifierImpl;
import com.terracotta.toolkit.events.ToolkitNotifierImplApplicator;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;
import com.terracotta.toolkit.object.ToolkitObjectStripeImplApplicator;
import com.terracotta.toolkit.object.serialization.CustomLifespanSerializedEntryApplicator;
import com.terracotta.toolkit.object.serialization.CustomLifespanSerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializedClusterObjectImpl;
import com.terracotta.toolkit.object.serialization.SerializedClusterObjectImplApplicator;
import com.terracotta.toolkit.object.serialization.SerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializedMapValueApplicator;
import com.terracotta.toolkit.object.serialization.SerializerMapImpl;
import com.terracotta.toolkit.object.serialization.SerializerMapImplApplicator;
import com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl;
import com.terracotta.toolkit.roots.impl.ToolkitTypeRootImplApplicator;

public class ToolkitConfigurator {

  protected DSOClientConfigHelper configHelper;

  public final void start(DSOClientConfigHelper clientConfigHelper) throws Exception {
    if (clientConfigHelper == null) { throw new AssertionError("configHelper is null"); }
    this.configHelper = clientConfigHelper;
    addInstrumentation();
  }

  protected void addInstrumentation() {
    TransparencyClassSpec spec;
    // ToolkitTypeRoot
    spec = this.configHelper.getOrCreateSpec(ToolkitTypeRootImpl.class.getName(),
                                             ToolkitTypeRootImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // SerializedClusterObject
    spec = configHelper.getOrCreateSpec(SerializedClusterObjectImpl.class.getName(),
                                        SerializedClusterObjectImplApplicator.class.getName());

    // ClusteredList
    spec = configHelper.getOrCreateSpec(ToolkitListImpl.class.getName(), ToolkitListImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // SerializerMap
    spec = configHelper.getOrCreateSpec(SerializerMapImpl.class.getName(), SerializerMapImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // ClusteredObjectStripe
    spec = configHelper.getOrCreateSpec(ToolkitObjectStripeImpl.class.getName(),
                                        ToolkitObjectStripeImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // ServerMap
    spec = configHelper.getOrCreateSpec(ServerMap.class.getName(), ServerMapApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // ClusteredNotifier
    spec = configHelper.getOrCreateSpec(ToolkitNotifierImpl.class.getName(),
                                        ToolkitNotifierImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // SerializedEntry
    spec = configHelper.getOrCreateSpec(SerializedMapValue.class.getName(),
                                        SerializedMapValueApplicator.class.getName());

    // CustomLifespanSerializedEntry
    spec = configHelper.getOrCreateSpec(CustomLifespanSerializedMapValue.class.getName(),
                                        CustomLifespanSerializedEntryApplicator.class.getName());

    // ToolkitMap
    spec = configHelper.getOrCreateSpec(ToolkitMapImpl.class.getName(), ToolkitMapImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // ToolkitSortedMap
    spec = configHelper.getOrCreateSpec(ToolkitSortedMapImpl.class.getName(),
                                        ToolkitSortedMapImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);
  }
}
