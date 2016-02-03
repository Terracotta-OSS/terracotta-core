/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.classloader;

import com.tc.util.Assert;
import java.util.List;

/**
 *
 * @author mscott
 */
public class TestInterfaceHandle implements Runnable {
  public void run() {
    List<Class<? extends TestInterface>> list = ServiceLocator.getImplementations(TestInterface.class);
     Assert.assertEquals(list.size(), 1);
     Assert.assertEquals(list.get(0).getName(), "com.tc.classloader.TestInterfaceImpl");
     Assert.assertTrue(list.get(0).getClassLoader() instanceof ComponentURLClassLoader);
     System.out.println(list.get(0).getInterfaces()[0].getClassLoader());
     Assert.assertTrue(list.get(0).getInterfaces()[0].getClassLoader() instanceof ApiClassLoader);
     Assert.assertEquals(list.get(0).getInterfaces()[0].getName(), "com.tc.classloader.TestInterface");
  }
}
