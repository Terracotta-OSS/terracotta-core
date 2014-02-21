/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.mockl2.test;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.internal.TerracottaL1Instance;

import com.tc.exception.ImplementMe;
import com.terracotta.toolkit.TerracottaToolkit;
import com.terracotta.toolkit.ToolkitCacheManagerProvider;
import com.terracotta.toolkit.rejoin.PlatformServiceProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


public class ToolkitUnitTest {

  private final MockPlatformService platformService;

  public ToolkitUnitTest() {
    platformService = new MockPlatformService();
    setPlatformService(platformService);
  }

  public Toolkit getToolKit() {
    Toolkit toolkit = new TerracottaToolkit(new TerracottaL1Instance() {
      @Override
      public void shutdown() {
        throw new ImplementMe();
      }
    }, new ToolkitCacheManagerProvider(), false, getClass().getClassLoader());
    return toolkit;
  }

  public void addPlatformListener(MockPlatformListener listener) {
    platformService.addPlatformListener(listener);
  }
  
  
  /* Bad implementation here. Not sure of any Better feasible Option Here.
   * Dependency Injection would have been best suited here
   */
  private void setPlatformService(MockPlatformService mockPlatformService)  {
    try {
      Field platformServiceField = PlatformServiceProvider.class.getDeclaredField("platformService");
      platformServiceField.setAccessible(true);
      
      // setting the static final field
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(platformServiceField, platformServiceField.getModifiers() & ~Modifier.FINAL);
      platformServiceField.set(PlatformServiceProvider.class, mockPlatformService);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void main(String[] args) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
    new ToolkitUnitTest();
    System.out.println(PlatformServiceProvider.getPlatformService());
    
  }
  
}
