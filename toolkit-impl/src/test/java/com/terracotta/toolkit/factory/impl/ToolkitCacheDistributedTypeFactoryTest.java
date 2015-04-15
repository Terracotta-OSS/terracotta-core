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
package com.terracotta.toolkit.factory.impl;

import org.junit.Test;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.search.SearchFactory;

import static org.mockito.Mockito.mock;

public class ToolkitCacheDistributedTypeFactoryTest extends BaseDistributedToolkitTypeFactoryTestBase {
  @Override
  protected BaseDistributedToolkitTypeFactory createFactory() {
    return new ToolkitCacheDistributedTypeFactory(mock(SearchFactory.class), mock(ServerMapLocalStoreFactory.class));
  }

  @Test
  public void testOverrideMaxTotalCount() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TOTAL_COUNT, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TOTAL_COUNT, 100, 1000, 100, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TOTAL_COUNT, 100, 1000, 100, "ALL");
  }

  @Test
  public void testOverrideTTI() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTI_SECONDS, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTI_SECONDS, 100, 1000, 100, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTI_SECONDS, 100, 1000, 100, "ALL");
  }

  @Test
  public void testOverrideTTL() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTL_SECONDS, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTL_SECONDS, 100, 1000, 100, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTL_SECONDS, 100, 1000, 100, "ALL");
  }
}
