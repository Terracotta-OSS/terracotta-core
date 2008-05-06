/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.aspectwerkz.definition.deployer;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.transform.Properties;
import com.tc.aspectwerkz.util.Strings;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public class StandardAspectModuleDeployer implements AspectModuleDeployer {

  public static final String[]   ASPECT_MODULES;
  static {
    ASPECT_MODULES = Strings.splitString(Properties.ASPECT_MODULES, ",");
  }

  private final static Map       modules    = new HashMap();
  private final static TCLogger  logger     = CustomerLogging.getDSOInstrumentationLogger();

  private final List             m_builders = new ArrayList();
  private final SystemDefinition m_systemDef;
  private final ClassLoader      m_loader;

  /**
   * Loads all registered aspect modules, builds and deploy them in the class loader specified.
   *
   * @param loader
   * @param moduleNames
   */
  public static void deploy(final ClassLoader loader, String[] moduleNames) {
    for (int i = 0; i < moduleNames.length; i++) {
      deploy(loader, moduleNames[i]);
    }
  }

  public static void deploy(final ClassLoader loader, String moduleName) {
    final Map loaders;

    synchronized (modules) {
      // TODO loaders collection should use weak refs to class loaders
      Map tmp = (Map) modules.get(moduleName);
      if (tmp == null) {
        tmp = new HashMap();
        modules.put(moduleName, tmp);
      }
      loaders = tmp;
    }

    final DeployedLoader deployedLoader;

    synchronized (loaders) {
      DeployedLoader d = (DeployedLoader) loaders.get(loader);
      if (d == null) {
        d = new DeployedLoader(loader, moduleName);
        loaders.put(loader, d);
      }
      deployedLoader = d;
    }

    try {
      deployedLoader.doDeployIfNeeded();
    } catch (InterruptedException e) {
      throw new WrappedRuntimeException(e);
    }
  }

  private static String getLoaderName(final ClassLoader loader) {
    // XXX: Dependency on DSO L1 code from AW. This isn't kosher at the moment
    // if(loader instanceof NamedClassLoader) {
    // return ((NamedClassLoader) loader).__tc_getClassLoaderName();
    // }
    return loader.getClass().getName() + "@" + System.identityHashCode(loader);
  }

  /**
   * Creates, registers and returns an aspect definition builder. Use-case: Get an aspect builder and then use it to add
   * advice and pointcut builders to build up a full aspect definintion programatically.
   *
   * @param aspectClass
   * @param scope
   * @param containerClassName
   * @return a newly registered aspect builder
   */
  public synchronized AspectDefinitionBuilder newAspectBuilder(final String aspectClass, final DeploymentModel scope,
                                                               final String containerClassName) {
    AspectDefinitionBuilder aspectDefinitionBuilder = new AspectDefinitionBuilder(aspectClass, scope,
                                                                                  containerClassName, m_systemDef,
                                                                                  m_loader);
    m_builders.add(aspectDefinitionBuilder);
    return aspectDefinitionBuilder;
  }

  /**
   * Creates and adds a new mixin builder to the deployment set.
   *
   * @param aspectClass
   * @param deploymentModel
   * @param pointcut
   */
  public synchronized void addMixin(final String aspectClass, final DeploymentModel deploymentModel,
                                    final String pointcut, final boolean isTransient) {
    m_builders.add(new MixinDefinitionBuilder(aspectClass, deploymentModel, pointcut, isTransient, m_systemDef,
                                              m_loader));
  }

  public ClassLoader getClassLoader() {
    return m_loader;
  }

  /**
   * Creates a new aspect module.
   *
   * @param loader
   */
  private StandardAspectModuleDeployer(final ClassLoader loader) {
    m_systemDef = SystemDefinitionContainer.getVirtualDefinitionFor(loader);
    m_loader = loader;
  }

  private synchronized void doDeploy(String moduleName) {
    loadModule(moduleName);
    buildModule();
    SystemDefinitionContainer.printDeploymentInfoFor(m_loader);
  }

  private void loadModule(String moduleName) {
    try {
      Class aspectModuleClass = Class.forName(moduleName, true, getClass().getClassLoader());
      AspectModule aspectModule = (AspectModule) aspectModuleClass.newInstance();
      aspectModule.deploy(this);
    } catch (Throwable e) {
      logger.error("Aspect module [" + moduleName + "] could not be deployed in " + getLoaderName(m_loader) + "; "
                   + e.toString(), e);
    }
  }

  private void buildModule() {
    for (Iterator it = m_builders.iterator(); it.hasNext();) {
      ((DefinitionBuilder) it.next()).build();
    }
  }

  private static class DeployedLoader {
    private static final int  NOT_DEPLOYED = 1;
    private static final int  DEPLOYING    = 2;
    private static final int  DEPLOYED     = 3;

    private int               state        = NOT_DEPLOYED;

    private final ClassLoader loader;
    private final String      moduleName;

    DeployedLoader(ClassLoader loader, String moduleName) {
      this.loader = loader;
      this.moduleName = moduleName;
    }

    void doDeployIfNeeded() throws InterruptedException {
      boolean doDeploy = false;
      boolean wait = false;

      synchronized (this) {
        switch (state) {
          case NOT_DEPLOYED: {
            state = DEPLOYING;
            doDeploy = true;
            break;
          }
          case DEPLOYING: {
            wait = true;
            break;
          }
          case DEPLOYED: {
            // no action required
            break;
          }
          default: {
            throw new AssertionError("unknown state: " + state);
          }
        }
      }

      if (doDeploy) {
        logger.info("Loading aspect module [" + moduleName + "] in loader " + getLoaderName(loader));
        new StandardAspectModuleDeployer(loader).doDeploy(moduleName);
        synchronized (this) {
          state = DEPLOYED;
          notifyAll();
        }
      } else if (wait) {
        synchronized (this) {
          while (state != DEPLOYED) {
            wait();
          }
        }
      }
    }
  }
}
