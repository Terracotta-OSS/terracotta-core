/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.simulator.container;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.object.TestClientConfigHelperFactory;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.IsolationClassLoader;

import java.util.Iterator;
import java.util.Map;

import com.tc.simulator.app.ApplicationConfig;

public class IsolationClassLoaderFactory {

  private final TestClientConfigHelperFactory factory;
  private final Class                   applicationClass;
  private final Map                     optionalAttributes;
  private final ConfigVisitor           configVisitor;
  private final PreparedComponentsFromL2Connection components;
  private final Map                                adapterMap;


  public IsolationClassLoaderFactory(TestClientConfigHelperFactory factory, Class applicationClass, Map optionalAttributes, PreparedComponentsFromL2Connection components, Map adapterMap) {
    this.factory = factory;
    this.applicationClass = applicationClass;
    this.optionalAttributes = optionalAttributes;
    this.configVisitor = new ConfigVisitor();
    this.components = components;
    this.adapterMap = adapterMap;
  }
  
  public IsolationClassLoader createIsolationClassLoader(String applicationId, ApplicationConfig applicationConfig) throws ConfigurationSetupException {
    boolean usesAdapters = false;

    IsolationClassLoader classloader = new IsolationClassLoader(createClientConfigHelper(), this.components);
    if (adapterMap != null) {
      if (adapterMap.size() > 0) {
        usesAdapters = true;
      }
      for (Iterator iter = adapterMap.keySet().iterator(); iter.hasNext();) {
        String adapteeName = (String) iter.next();
        Class adapterClass = (Class) adapterMap.get(adapteeName);
        classloader.addAdapter(adapteeName, adapterClass);
      }
    }

    // so the app instance can tell whether it has been adapted
    applicationConfig.setAttribute(applicationId + ApplicationConfig.ADAPTED_KEY, usesAdapters + "");

    return classloader;
  }

  protected DSOClientConfigHelper createClientConfigHelper() throws ConfigurationSetupException {
    DSOClientConfigHelper configHelper = factory.createClientConfigHelper();
    if (optionalAttributes != null && optionalAttributes.size() > 0) {
      this.configVisitor.visit(configHelper, this.applicationClass, this.optionalAttributes);
    } else {
      this.configVisitor.visit(configHelper, this.applicationClass);
    }

    return configHelper;
  }
}