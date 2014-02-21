package com.terracotta.toolkit.factory.impl;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.search.SearchFactory;

import static org.mockito.Mockito.mock;

/**
 * @author tim
 */
public class ToolkitStoreDistributedTypeFactoryTest extends BaseDistributedToolkitTypeFactoryTestBase {
  @Override
  protected BaseDistributedToolkitTypeFactory createFactory() {
    return new ToolkitStoreDistributedTypeFactory(mock(SearchFactory.class), mock(ServerMapLocalStoreFactory.class));
  }
}
