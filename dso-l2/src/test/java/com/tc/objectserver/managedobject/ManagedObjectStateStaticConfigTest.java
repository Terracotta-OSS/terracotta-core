/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig.Factory;

import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ManagedObjectStateStaticConfigTest extends TestCase {

  public void test() {
    Set<String> toolkitTypeNames = ManagedObjectStateStaticConfig.ToolkitTypeNames.values();
    ManagedObjectStateStaticConfig[] configs = ManagedObjectStateStaticConfig.values();
    Factory[] factories = ManagedObjectStateStaticConfig.Factory.values();

    Assert.assertTrue(configs.length > 1);
    Assert.assertTrue(factories.length > 1);

    for (ManagedObjectStateStaticConfig config : configs) {
      System.out.println(config);
      Assert.assertNotNull(config.getFactory());
      Assert.assertEquals(config.getFactory(), ManagedObjectStateStaticConfig.Factory.getFactoryForType(config
          .getFactory().getStateObjectType()));
      Assert.assertEquals(config.getStateObjectType(), config.getFactory().getStateObjectType());

      // verify every config has a name defined
      Assert.assertTrue("ToolkitTypeName constant not defined for '" + config.getClientClassName() + "'",
                        toolkitTypeNames.contains(config.getClientClassName()));
    }

    for (Factory f : factories) {
      System.out.println(f);
      Assert.assertEquals(f, Factory.getFactoryForType(f.getStateObjectType()));
    }

    for (String name : toolkitTypeNames) {
      Assert.assertTrue(name != null);
      Assert.assertTrue(name.trim().length() > 0);
      ManagedObjectStateStaticConfig config = ManagedObjectStateStaticConfig.getConfigForClientClassName(name);
      System.out.println(name);
      // verify every name has a config defined
      Assert.assertNotNull("ManagedObjectStateStaticFactory enum type not defined for toolkit type: '" + name + "'",
                           config);
      Assert.assertEquals(name, config.getClientClassName());
    }

  }
}
