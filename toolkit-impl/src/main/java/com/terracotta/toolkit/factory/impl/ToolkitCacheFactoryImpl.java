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

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;

public class ToolkitCacheFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitCacheImpl, ServerMap> {

  private ToolkitCacheFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory().createAggregateDistributedTypeRoot(
        ToolkitTypeConstants.TOOLKIT_CACHE_ROOT_NAME, new ToolkitCacheDistributedTypeFactory(
        context.getSearchFactory(), context.getServerMapLocalStoreFactory()), context.getPlatformService()));
  }

  public static ToolkitCacheFactoryImpl newToolkitCacheFactory(ToolkitInternal toolkit,
                                                               ToolkitFactoryInitializationContext context) {
    return new ToolkitCacheFactoryImpl(toolkit, context);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.CACHE;
  }

}
