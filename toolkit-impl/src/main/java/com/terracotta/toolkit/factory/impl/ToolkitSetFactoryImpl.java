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
import com.terracotta.toolkit.collections.DestroyableToolkitMap;
import com.terracotta.toolkit.collections.ToolkitSetImpl;
import com.terracotta.toolkit.collections.map.ToolkitMapImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

public class ToolkitSetFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitSetImpl, ToolkitMapImpl> {

  public ToolkitSetFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SET_ROOT_NAME, new SetIsolatedTypeFactory(context.getPlatformService()),
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SET;
  }

  private static class SetIsolatedTypeFactory implements IsolatedToolkitTypeFactory<ToolkitSetImpl, ToolkitMapImpl> {

    private final PlatformService platformService;

    SetIsolatedTypeFactory(PlatformService platformService) {
      this.platformService = platformService;
    }

    @Override
    public ToolkitSetImpl createIsolatedToolkitType(ToolkitObjectFactory<ToolkitSetImpl> factory,
                                                    IsolatedClusteredObjectLookup<ToolkitMapImpl> lookup, String name,
                                                    Configuration config, ToolkitMapImpl tcClusteredObject) {
      DestroyableToolkitMap map = new DestroyableToolkitMap(factory, lookup, tcClusteredObject, name, platformService);
      return new ToolkitSetImpl(map, platformService);
    }

    @Override
    public ToolkitMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitMapImpl(platformService);
    }

  }

}
