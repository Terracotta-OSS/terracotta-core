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

  protected TransparencyClassSpec getOrCreateSpec(final String expr) {
    return configHelper.getOrCreateSpec(expr);
  }

  protected void addInstrumentation() {
    TransparencyClassSpec spec;
    // ToolkitTypeRoot
    spec = this.configHelper.getOrCreateSpec(ToolkitTypeRootImpl.class.getName(),
                                             ToolkitTypeRootImplApplicator.class.getName());
    spec.setHonorTransient(true);

    // SerializedClusterObject
    spec = configHelper.getOrCreateSpec(SerializedClusterObjectImpl.class.getName(),
                                        SerializedClusterObjectImplApplicator.class.getName());
    spec.setHonorTransient(true);

    // ClusteredList
    spec = configHelper.getOrCreateSpec(ToolkitListImpl.class.getName(), ToolkitListImplApplicator.class.getName());
    spec.setHonorTransient(true);

    // SerializerMap
    spec = configHelper.getOrCreateSpec(SerializerMapImpl.class.getName(), SerializerMapImplApplicator.class.getName());
    spec.setHonorTransient(true);

    // ClusteredObjectStripe
    spec = configHelper.getOrCreateSpec(ToolkitObjectStripeImpl.class.getName(),
                                        ToolkitObjectStripeImplApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);

    // ServerMap
    spec = configHelper.getOrCreateSpec(ServerMap.class.getName(), ServerMapApplicator.class.getName());
    spec.setUseNonDefaultConstructor(true);
    spec.setHonorTransient(true);

    // ClusteredNotifier
    spec = configHelper.getOrCreateSpec(ToolkitNotifierImpl.class.getName(),
                                        ToolkitNotifierImplApplicator.class.getName());
    spec.setHonorTransient(true);

    // SerializedEntry
    spec = configHelper.getOrCreateSpec(SerializedMapValue.class.getName(),
                                        SerializedMapValueApplicator.class.getName());
    spec.setHonorTransient(true);

    // CustomLifespanSerializedEntry
    spec = configHelper.getOrCreateSpec(CustomLifespanSerializedMapValue.class.getName(),
                                        CustomLifespanSerializedEntryApplicator.class.getName());
    spec.setHonorTransient(true);

    // ToolkitMap
    spec = configHelper.getOrCreateSpec(ToolkitMapImpl.class.getName(), ToolkitMapImplApplicator.class.getName());
    spec.setHonorTransient(true);

    // ToolkitSortedMap
    spec = configHelper.getOrCreateSpec(ToolkitSortedMapImpl.class.getName(), ToolkitMapImplApplicator.class.getName());
    spec.setHonorTransient(true);
  }
}
