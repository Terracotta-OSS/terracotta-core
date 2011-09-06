/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tools.cli;

import org.terracotta.modules.tool.CachedModules;
import org.terracotta.modules.tool.DefaultModuleReport;
import org.terracotta.modules.tool.ModuleReport;
import org.terracotta.modules.tool.Modules;
import org.terracotta.modules.tool.commands.ActionLog;
import org.terracotta.modules.tool.commands.CommandRegistry;
import org.terracotta.modules.tool.config.Config;
import org.terracotta.modules.tool.config.ConfigAnnotation;
import org.terracotta.modules.tool.util.DownloadUtil;

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
  private final Config    config;
  private final ActionLog actionLog;

  public AppContext(Config config, ActionLog actionLog) {
    this.config = config;
    this.actionLog = actionLog;
  }

  public void configure(Binder binder) {
    // Inject the tcVersion anywhere the @TerracottaVersion annotation is used
    // binder.bindConstant().annotatedWith(TerracottaVersion.class).to(config.getTcVersion());
    binder.bindConstant().annotatedWith(Names.named(TERRACOTTA_VERSION)).to(config.getTcVersion());

    // Inject the tcTimApiVersion anywhere the @TimApiVersion annotation is used
    // binder.bindConstant().annotatedWith(ApiVersion.class).to(config.getApiVersion());
    binder.bindConstant().annotatedWith(Names.named(TIM_API_VERSION)).to(config.getTimApiVersion());

    // Inject the relativeUrlBase anywhere @Named(ConfigAnnotation.RELATIVE_URL_BASE) is used
    binder.bindConstant().annotatedWith(Names.named(RELATIVE_URL_BASE)).to(config.getRelativeUrlBase().toString());

    // Inject the includeSnapshots anywhere the @IncludeSnapshots is used
    binder.bindConstant().annotatedWith(Names.named(INCLUDE_SNAPSHOTS)).to(config.getIncludeSnapshots());

    // Inject the dataCacheExpirationInSeconds anywhere the @DataCacheExpirationInSeconds is used
    binder.bindConstant().annotatedWith(Names.named(DATA_CACHE_EXPIRATION_IN_SECONDS))
        .to(config.getDataCacheExpirationInSeconds());

    // Inject the modulesDirectory anywhere the @ModulesDirectory is used
    binder.bindConstant().annotatedWith(Names.named(MODULES_DIRECTORY)).to(config.getModulesDirectory().toString());

    // Make our Config object available to anybody that needs it
    binder.bind(Config.class).in(Scopes.SINGLETON); // XXX: Do we need this?

    binder.bind(Config.class).annotatedWith(Names.named(CONFIG_INSTANCE)).toInstance(config);
    binder.bind(DownloadUtil.class).annotatedWith(Names.named(DOWNLOADUTIL_INSTANCE)).to(DownloadUtil.class);

    // Make action log object available
    binder.bind(ActionLog.class).in(Scopes.SINGLETON);
    binder.bind(ActionLog.class).annotatedWith(Names.named(ACTION_LOG_INSTANCE)).toInstance(actionLog);

    binder.bind(ModuleReport.class).to(DefaultModuleReport.class);
    binder.bind(ModuleReport.class).annotatedWith(Names.named(MODULEREPORT_INSTANCE)).to(DefaultModuleReport.class)
        .in(Scopes.SINGLETON);

    binder.bind(Modules.class).to(CachedModules.class).in(Scopes.SINGLETON);
    binder.bind(Modules.class).annotatedWith(Names.named(MODULES_INSTANCE)).to(CachedModules.class)
        .in(Scopes.SINGLETON);

    binder.bind(CommandRegistry.class).in(Scopes.SINGLETON);

    binder.bind(DownloadUtil.class).toProvider(new Provider<DownloadUtil>() {
      public DownloadUtil get() {
        DownloadUtil downloadUtil = new DownloadUtil();
        URL proxyUrl = config.getProxyUrl();
        if (proxyUrl != null) {
          SocketAddress proxyAddress = new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort());
          Proxy proxy = new Proxy(Type.HTTP, proxyAddress);
          downloadUtil.setProxy(proxy);
          downloadUtil.setProxyAuth(config.getProxyAuth());
        }
        return downloadUtil;
      }
    });
  }
}
