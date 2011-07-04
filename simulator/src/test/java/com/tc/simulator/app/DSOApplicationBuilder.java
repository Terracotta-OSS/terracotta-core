/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.container.IsolationClassLoaderFactory;
import com.tc.simulator.listener.ListenerProvider;

import java.lang.reflect.Constructor;

public class DSOApplicationBuilder implements ApplicationBuilder {

  private final IsolationClassLoaderFactory        configHelperFactoryData;
  private final ApplicationConfig                  applicationConfig;
  
  private final ClassLoader                        fixedClassloader;

  private Class                                    applicationClass;
  private Constructor                              applicationConstructor;

  public DSOApplicationBuilder(IsolationClassLoaderFactory configHelperFactoryData,
                               ApplicationConfig applicationConfig) {
    this.configHelperFactoryData = configHelperFactoryData;
    this.applicationConfig = applicationConfig;
    this.fixedClassloader = null;

  }

  public DSOApplicationBuilder(ApplicationConfig applicationConfig, ClassLoader classloader) {
    this.configHelperFactoryData = null;
    this.applicationConfig = applicationConfig;
    this.fixedClassloader = classloader;
  }

  public void setAppConfigAttribute(String key, String value) {
    applicationConfig.setAttribute(key, value);
  }

  // XXX:: Adding more debugs to figure out the OOME in Primitive ArrayTest.
  TCLogger logger = TCLogging.getLogger(DSOApplicationBuilder.class);

  public synchronized Application newApplication(String applicationId, ListenerProvider listenerProvider)
      throws ApplicationInstantiationException {

    try {
      ClassLoader classloader = null;
      if (fixedClassloader != null) {
        classloader = fixedClassloader;
      } else {
        classloader = this.configHelperFactoryData.createIsolationClassLoader(applicationId, applicationConfig);
      }

      ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(classloader);
      try {      
        logger.info("Before initializing Class Loader...");
        if (classloader instanceof IsolationClassLoader) {
          ((IsolationClassLoader) classloader).init();
        }
        
        logger.info("After initializing Class Loader...");
        Class applicationConfigClass = classloader.loadClass(ApplicationConfig.class.getName());
        Class listenerProviderClass = classloader.loadClass(ListenerProvider.class.getName());
        this.applicationClass = classloader.loadClass(this.applicationConfig.getApplicationClassname());
        this.applicationConstructor = this.applicationClass.getConstructor(new Class[] { String.class,
            applicationConfigClass, listenerProviderClass });
        logger.info("Before new Instance is created...");
  
        return (Application) this.applicationConstructor.newInstance(new Object[] { applicationId,
            this.applicationConfig, listenerProvider });
      } finally {
        Thread.currentThread().setContextClassLoader(previousClassLoader);
      }
    } catch (Throwable t) {
      t.printStackTrace();
      throw new ApplicationInstantiationException(t);
    }

  }
}
