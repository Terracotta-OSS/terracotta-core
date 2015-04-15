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

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

public class ToolkitFactoryInitializationContextBuilder {

  private WeakValueMapManager        weakValueMapManager;
  private PlatformService            platformService;
  private ToolkitTypeRootsFactory    toolkitTypeRootsFactory;
  private ServerMapLocalStoreFactory serverMapLocalStoreFactory;
  private SearchFactory              searchFactory;

  public ToolkitFactoryInitializationContextBuilder weakValueMapManager(WeakValueMapManager weakValueMapManagerParam) {
    this.weakValueMapManager = weakValueMapManagerParam;
    return this;
  }

  public ToolkitFactoryInitializationContextBuilder platformService(PlatformService platformServiceParam) {
    this.platformService = platformServiceParam;
    return this;
  }

  public ToolkitFactoryInitializationContextBuilder toolkitTypeRootsFactory(ToolkitTypeRootsFactory factory) {
    this.toolkitTypeRootsFactory = factory;
    return this;
  }

  public ToolkitFactoryInitializationContextBuilder serverMapLocalStoreFactory(ServerMapLocalStoreFactory factory) {
    this.serverMapLocalStoreFactory = factory;
    return this;
  }

  public ToolkitFactoryInitializationContextBuilder searchFactory(SearchFactory factory) {
    this.searchFactory = factory;
    return this;
  }

  public ToolkitFactoryInitializationContext build() {
    return new ToolkitFactoryInitializationContextImpl(this);
  }

  private static class ToolkitFactoryInitializationContextImpl implements ToolkitFactoryInitializationContext {

    private final WeakValueMapManager        manager;
    private final PlatformService            service;
    private final ToolkitTypeRootsFactory    rootsFactory;
    private final ServerMapLocalStoreFactory localStoreFactory;
    private final SearchFactory              factory;

    private ToolkitFactoryInitializationContextImpl(ToolkitFactoryInitializationContextBuilder builder) {
      manager = builder.weakValueMapManager;
      service = builder.platformService;
      rootsFactory = builder.toolkitTypeRootsFactory;
      localStoreFactory = builder.serverMapLocalStoreFactory;
      factory = builder.searchFactory;
    }

    @Override
    public WeakValueMapManager getWeakValueMapManager() {
      return this.manager;
    }

    @Override
    public PlatformService getPlatformService() {
      return this.service;
    }

    @Override
    public ToolkitTypeRootsFactory getToolkitTypeRootsFactory() {
      return this.rootsFactory;
    }

    @Override
    public ServerMapLocalStoreFactory getServerMapLocalStoreFactory() {
      return this.localStoreFactory;
    }

    @Override
    public SearchFactory getSearchFactory() {
      return this.factory;
    }

  }

}
