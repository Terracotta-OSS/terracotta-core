/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.NonStopConfigurationBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NonStopConfigurationRegistryTest extends TestCase {
  private NonStopConfigRegistryImpl nonStopConfigurationRegistry;

  @Override
  protected void setUp() throws Exception {
    nonStopConfigurationRegistry = new NonStopConfigRegistryImpl();
  }

  public void testDefaultConfig() {
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    // TODO: should it return default config
    Assert.assertNull(nonStopConfigurationRegistry.getConfigForThread());
  }

  public void testConfigOrder() {
    // register config for type for atomic long and cache
    NonStopConfiguration configForType = new NonStopConfigurationBuilder().build();
    nonStopConfigurationRegistry.registerForType(configForType, ToolkitObjectType.CACHE, ToolkitObjectType.ATOMIC_LONG);
    // Assert default config is applicable for Store
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.STORE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.STORE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.STORE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.STORE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));

    // Assert configs for Cache
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForType));

    // Assert configs for Atomic Long
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstanceMethod("sample", "test",
                                                                              ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));

    Assert.assertNull(nonStopConfigurationRegistry.getConfigForThread());

    // register config for type Method.
    NonStopConfiguration configForTypeMethod = new NonStopConfigurationBuilder().build();
    nonStopConfigurationRegistry.registerForTypeMethod(configForTypeMethod, "sample", ToolkitObjectType.CACHE);
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForTypeMethod));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForTypeMethod));

    // register config for instance.
    NonStopConfiguration configForInstance = new NonStopConfigurationBuilder().build();
    nonStopConfigurationRegistry.registerForInstance(configForInstance, "test", ToolkitObjectType.CACHE);
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForInstance));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForInstance));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForTypeMethod));

    // register config for instance method.
    NonStopConfiguration configForInstanceMethod = new NonStopConfigurationBuilder().build();
    nonStopConfigurationRegistry.registerForInstanceMethod(configForInstanceMethod, "sample", "test",
                                                           ToolkitObjectType.CACHE);
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForInstance));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForInstanceMethod));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForTypeMethod));

    // register thread level and make sure it overrides all.
    NonStopConfiguration configForThread = new NonStopConfigurationBuilder().build();
    nonStopConfigurationRegistry.registerForThread(configForThread);
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForThread));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForThread));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForThread));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForThread));

    // deregister configurations one by one and check that deregistration works.
    // deregister for thread.
    Assert.assertEquals(configForThread, nonStopConfigurationRegistry.deregisterForThread());
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForInstance));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForInstanceMethod));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForTypeMethod));
    Assert.assertNull(nonStopConfigurationRegistry.getConfigForThread());

    // deregister for instance method.
    Assert.assertEquals(configForInstanceMethod, nonStopConfigurationRegistry
        .deregisterForInstanceMethod("sample", "test", ToolkitObjectType.CACHE));

    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForInstance));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForInstance));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForTypeMethod));

    // deregister for instance
    Assert.assertEquals(configForInstance,
                        nonStopConfigurationRegistry.deregisterForInstance("test", ToolkitObjectType.CACHE));

    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForTypeMethod));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForTypeMethod));

    // deregister for type method
    Assert.assertEquals(configForTypeMethod,
                        nonStopConfigurationRegistry.deregisterForTypeMethod("sample", ToolkitObjectType.CACHE));

    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE).equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(configForType));

    // deregister for type
    Assert.assertEquals(configForType, nonStopConfigurationRegistry.deregisterForType(ToolkitObjectType.CACHE));

    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry
        .getConfigForInstanceMethod("sample", "test", ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.CACHE)
        .equals(NonStopConfigRegistryImpl.DEFAULT_CONFIG));

    // Assert config for atomic long is not deregistered.
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstance("test", ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForInstanceMethod("sample", "test",
                                                                              ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForType(ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));
    Assert.assertTrue(nonStopConfigurationRegistry.getConfigForTypeMethod("sample", ToolkitObjectType.ATOMIC_LONG)
        .equals(configForType));

    Assert.assertNull(nonStopConfigurationRegistry.getConfigForThread());

  }

}
