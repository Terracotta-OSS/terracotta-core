/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.mockl2.test;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.internal.TerracottaL1Instance;

import com.tc.exception.ImplementMe;
import com.terracotta.toolkit.TerracottaToolkit;
import com.terracotta.toolkit.ToolkitCacheManagerProvider;


public class ToolkitUnitTest {

  private final MockPlatformService platformService;

  public ToolkitUnitTest() {
    platformService = new MockPlatformService();
  }

  public Toolkit getToolKit() {
    Toolkit toolkit = new TerracottaToolkit(new TerracottaL1Instance() {
      @Override
      public void shutdown() {
        throw new ImplementMe();
      }
    }, new ToolkitCacheManagerProvider(), false, getClass().getClassLoader(), platformService);
    return toolkit;
  }

  public void addPlatformListener(MockPlatformListener listener) {
    platformService.addPlatformListener(listener);
  }

}
