/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.aspectwerkz.definition.deployer;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.transform.Properties;
import com.tc.aspectwerkz.util.Strings;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
// import com.tc.object.loaders.NamedClassLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public class StandardAspectModuleDeployer implements AspectModuleDeployer {

  public static final String[] ASPECT_MODULES;
  static {
    ASPECT_MODULES = Strings.splitString(Properties.ASPECT_MODULES, ",");
  }

  private final static Map modules = new HashMap();
  private final static TCLogger logger = CustomerLogging.getDSOInstrumentationLogger();

  private final List m_builders = new ArrayList();
  private final SystemDefinition m_systemDef;
  private final ClassLoader m_loader;


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
    synchronized(modules) {
      // TODO loaders collection should use weak refs to class loaders
      Set loaders = (Set) modules.get(moduleName);
      if(loaders==null) {
        loaders = new HashSet();
        modules.put(moduleName, loaders);
      }

      if(!loaders.contains(loader)) {
        logger.info("Loading aspect module [" + moduleName + "] in loader " + getLoaderName(loader));
        new StandardAspectModuleDeployer(loader).doDeploy(moduleName);
        loaders.add(loader);
      }
    }
  }

  private static String getLoaderName(final ClassLoader loader) {
    // XXX: Dependency on DSO L1 code from AW. This isn't kosher at the moment
    //    if(loader instanceof NamedClassLoader) {
    //      return ((NamedClassLoader) loader).__tc_getClassLoaderName();
    //    }
    return loader.getClass().getName()+"@"+System.identityHashCode(loader);
  }


  /**
   * Creates, registers and returns an aspect definition builder.
   * Use-case: Get an aspect builder and then use it to add advice and pointcut builders to build up a full aspect
   * definintion programatically.
   *
   * @param aspectClass
   * @param scope
   * @param containerClassName
   * @return a newly registered aspect builder
   */
  public synchronized AspectDefinitionBuilder newAspectBuilder(final String aspectClass,
                                                               final DeploymentModel scope,
                                                               final String containerClassName) {
    AspectDefinitionBuilder aspectDefinitionBuilder = new AspectDefinitionBuilder(
            aspectClass, scope, containerClassName, m_systemDef, m_loader
    );
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
  public synchronized void addMixin(final String aspectClass,
                                    final DeploymentModel deploymentModel,
                                    final String pointcut,
                                    final boolean isTransient) {
    m_builders.add(new MixinDefinitionBuilder(aspectClass, deploymentModel, pointcut, isTransient, m_systemDef, m_loader));
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
      Class aspectModuleClass = getClass().getClassLoader().loadClass(moduleName);
      AspectModule aspectModule = (AspectModule) aspectModuleClass.newInstance();
      aspectModule.deploy(this);
    } catch (Throwable e) {
      logger.error("Aspect module [" + moduleName + "] could not be deployed in "+getLoaderName(m_loader)+"; "+e.toString(), e);
    }
  }

  private void buildModule() {
    for (Iterator it = m_builders.iterator(); it.hasNext();) {
      ((DefinitionBuilder) it.next()).build();
    }
  }

}

