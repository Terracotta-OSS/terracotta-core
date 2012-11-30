/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

public interface ToolkitFactoryInitializationContext {

  WeakValueMapManager getWeakValueMapManager();

  PlatformService getPlatformService();

  ToolkitTypeRootsFactory getToolkitTypeRootsFactory();

  ServerMapLocalStoreFactory getServerMapLocalStoreFactory();

  SearchFactory getSearchFactory();

}
