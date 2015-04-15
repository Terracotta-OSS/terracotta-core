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
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.DestroyableToolkitSortedMap;
import com.terracotta.toolkit.collections.ToolkitSortedSetImpl;
import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ToolkitSortedSetFactory}
 */
public class ToolkitSortedSetFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitSortedSetImpl, ToolkitSortedMapImpl> {

  public ToolkitSortedSetFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SORTED_SET_ROOT_NAME,
                                         new SortedSetIsolatedTypeFactory(context.getPlatformService()),
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SORTED_SET;
  }

  private static class SortedSetIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitSortedSetImpl, ToolkitSortedMapImpl> {

    private final PlatformService platformService;

    SortedSetIsolatedTypeFactory(PlatformService platformService) {
      this.platformService = platformService;
    }

    @Override
    public ToolkitSortedSetImpl createIsolatedToolkitType(ToolkitObjectFactory<ToolkitSortedSetImpl> factory,
                                                          IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup,
                                                          String name, Configuration config,
                                                          ToolkitSortedMapImpl tcClusteredObject) {
      DestroyableToolkitSortedMap map = new DestroyableToolkitSortedMap(factory, lookup, tcClusteredObject, name,
                                                                        platformService);
      return new ToolkitSortedSetImpl(map, platformService);
    }

    @Override
    public ToolkitSortedMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitSortedMapImpl(platformService);
    }
  }

}
