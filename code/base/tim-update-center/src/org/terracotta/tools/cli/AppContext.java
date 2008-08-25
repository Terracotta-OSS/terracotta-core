/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tools.cli;

import org.terracotta.modules.tool.CachedModules;
import org.terracotta.modules.tool.Modules;
import org.terracotta.modules.tool.commands.CommandRegistry;
import org.terracotta.modules.tool.config.Config;
import org.terracotta.modules.tool.config.ConfigAnnotation;
import org.terracotta.modules.tool.util.DataLoader;
import org.terracotta.modules.tool.util.DataLoader.CacheRefreshPolicy;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.Proxy.Type;

/**
 * Module definition for Guice dependency injection.
 */
class AppContext implements Module, ConfigAnnotation {
  private final Config config;

  public AppContext() {
    this(new Config());
  }

  public AppContext(Config config) {
    this.config = config;
  }

  public void configure(Binder binder) {
    // Inject the tcVersion anywhere the @TerracottaVersion annotation is used
    // binder.bindConstant().annotatedWith(TerracottaVersion.class).to(config.getTcVersion());
    binder.bindConstant().annotatedWith(Names.named(TERRACOTTA_VERSION)).to(config.getTcVersion());

    // Inject the dataCacheExpirationInSeconds anywhere the @DataCacheExpirationInSeconds is used
    binder.bindConstant().annotatedWith(Names.named(DATA_CACHE_EXPIRATION_IN_SECONDS))
        .to(config.getDataCacheExpirationInSeconds());

    // Inject the modulesDirectory anywhere the @ModulesDirectory is used
    binder.bindConstant().annotatedWith(Names.named(MODULES_DIRECTORY)).to(config.getModulesDirectory().toString());

    // Make our Config object available to anybody that needs it
    binder.bind(Config.class).in(Scopes.SINGLETON);

    // The DataLoader is used by the CachedModules implementation to download
    // the remote data file and cache it locally.
    binder.bind(DataLoader.class).toProvider(new Provider<DataLoader>() {
      public DataLoader get() {
        DataLoader dataLoader = new DataLoader(config.getDataFileUrl(), config.getDataFile());
        dataLoader.setCacheRefreshPolicy(CacheRefreshPolicy.ON_EXPIRATION.setExpirationInSeconds(60 * 60 * 24));
        URL proxyUrl = config.getProxyUrl();
        if (proxyUrl != null) {
          SocketAddress proxyAddress = new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort());
          dataLoader.setProxy(new Proxy(Type.HTTP, proxyAddress));
        }
        return dataLoader;
      }
    });

    binder.bind(Modules.class).to(CachedModules.class).in(Scopes.SINGLETON);
    binder.bind(CommandRegistry.class).in(Scopes.SINGLETON);
  }
}
