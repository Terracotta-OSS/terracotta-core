/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
