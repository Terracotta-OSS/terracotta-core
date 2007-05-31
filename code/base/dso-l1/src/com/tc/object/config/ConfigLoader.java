/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.ArrayUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.TCLogger;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.NonDistributedFields;
import com.terracottatech.config.OnLoad;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.SpringAppContext;
import com.terracottatech.config.SpringApplication;
import com.terracottatech.config.SpringApps;
import com.terracottatech.config.SpringBean;
import com.terracottatech.config.SpringDistributedEvent;
import com.terracottatech.config.SpringPath;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplication;
import com.terracottatech.config.WebApplications;
import com.terracottatech.config.DistributedMethods.MethodExpression;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ConfigLoader {
  private final DSOClientConfigHelper config;
  private final TCLogger              logger;

  public ConfigLoader(DSOClientConfigHelper config, TCLogger logger) {
    this.config = config;
    this.logger = logger;
  }

  public void loadDsoConfig(DsoApplication dsoApplication) throws ConfigurationSetupException {
    if (dsoApplication != null) {
      Roots rootsList = dsoApplication.getRoots();
      if (rootsList != null && rootsList.getRootArray() != null) {
        Root[] roots = rootsList.getRootArray();
        for (int i = 0; i < roots.length; ++i) {
          Root root = roots[i];
          try {
            ClassSpec classSpec = ClassUtils.parseFullyQualifiedFieldName(root.getFieldName());
            String className = classSpec.getFullyQualifiedClassName();
            String fieldName = classSpec.getShortFieldName();
            String rootName = root.getRootName();
            config.addRoot(className, fieldName, rootName, false);
          } catch (ParseException pe) {
            throw new ConfigurationSetupException("Root '" + root.getFieldName() + "' is invalid", pe);
          }
        }
      }

      WebApplications webApplicationsList = dsoApplication.getWebApplications();
      if (webApplicationsList != null && webApplicationsList.getWebApplicationArray() != null) {
        WebApplication[] webApplications = webApplicationsList.getWebApplicationArray();
        for (int i = 0; i < webApplications.length; i++) {
          config.addApplicationName(webApplications[i].getStringValue());
          if (webApplications[i].getSynchronousWrite()) {
            config.addSynchronousWriteApplication(webApplications[i].getStringValue());
          }
        }
      }

      loadLocks(dsoApplication.getLocks());
      loadTransientFields(dsoApplication.getTransientFields());
      loadInstrumentedClasses(dsoApplication.getInstrumentedClasses());
      loadDistributedMethods(dsoApplication.getDistributedMethods());

      AdditionalBootJarClasses additionalBootJarClassesList = dsoApplication.getAdditionalBootJarClasses();
      // XXX
      if (additionalBootJarClassesList != null) {
        Set userDefinedBootClassNames = new HashSet();
        userDefinedBootClassNames.addAll(Arrays.asList(additionalBootJarClassesList.getIncludeArray()));
        logger.debug("Additional boot-jar classes: " + ArrayUtils.toString(userDefinedBootClassNames));

        for (Iterator i = userDefinedBootClassNames.iterator(); i.hasNext();) {
          String className = (String) i.next();
          TransparencyClassSpec spec = config.getSpec(className);
          if (spec == null) {
            spec = new TransparencyClassSpec(className, config);
            spec.markPreInstrumented();
            config.addUserDefinedBootSpec(spec.getClassName(), spec);
          } else if (!spec.isPreInstrumented()) {
            // DEV-458: if the class being added to the boot jar defines locks/distributed methods/etc. it creates a
            // spec but does not pre-instrument it.  This makes sure that the adapted code gets into the boot jar in
            // this case.
            spec.markPreInstrumented();
          }
        }
      }
    }
  }

  public void loadSpringConfig(SpringApplication springApplication) throws ConfigurationSetupException {
    if (springApplication != null) {
      SpringApps[] springApps = springApplication.getJeeApplicationArray();
      for (int i = 0; springApps != null && i < springApps.length; i++) {
        SpringApps springApp = springApps[i];
        if (springApp != null) {
          loadSpringApp(springApp);
        }
      }
    }
  }

  private void loadSpringApp(SpringApps springApp) throws ConfigurationSetupException {
    // TODO scope the following by app namespace https://jira.terracotta.lan/jira/browse/LKC-2284
    loadLocks(springApp.getLocks());
    loadTransientFields(springApp.getTransientFields());
    loadInstrumentedClasses(springApp.getInstrumentedClasses());

    if (springApp.getSessionSupport()) {
      config.addApplicationName(springApp.getName()); // enable session support
    }

    if (springApp.getApplicationContexts() != null) {
      loadSpringAppContexts(springApp);
    }
  }

  private void loadSpringAppContexts(SpringApps springApp) {
    String appName = springApp.getName();
    boolean fastProxy = springApp.getFastProxy();
    SpringAppContext[] applicationContexts = springApp.getApplicationContexts().getApplicationContextArray();
    for (int i = 0; applicationContexts != null && i < applicationContexts.length; i++) {
      SpringAppContext appContext = applicationContexts[i];
      if (appContext == null) continue;

      DSOSpringConfigHelper springConfigHelper = new StandardDSOSpringConfigHelper();
      springConfigHelper.addApplicationNamePattern(appName);
      springConfigHelper.setFastProxyEnabled(fastProxy); // copy flag to all subcontexts
      springConfigHelper.setRootName(appContext.getRootName());
      springConfigHelper.setLocationInfoEnabled(appContext.getEnableLocationInfo());

      SpringDistributedEvent distributedEventList = appContext.getDistributedEvents();
      if (distributedEventList != null) {
        String[] distributedEvents = distributedEventList.getDistributedEventArray();
        for (int k = 0; distributedEvents != null && k < distributedEvents.length; k++) {
          springConfigHelper.addDistributedEvent(distributedEvents[k]);
        }
      }

      SpringPath pathList = appContext.getPaths();
      if (pathList != null) {
        String[] paths = pathList.getPathArray();
        for (int j = 0; paths != null && j < paths.length; j++) {
          springConfigHelper.addConfigPattern(paths[j]);
        }
      }

      SpringBean springBean = appContext.getBeans();
      if (springBean != null) {
        NonDistributedFields[] nonDistributedFields = springBean.getBeanArray();
        for (int j = 0; nonDistributedFields != null && j < nonDistributedFields.length; j++) {
          NonDistributedFields nonDistributedField = nonDistributedFields[j];

          String beanName = nonDistributedField.getName();
          springConfigHelper.addBean(beanName);

          String[] fields = nonDistributedField.getNonDistributedFieldArray();
          for (int k = 0; fields != null && k < fields.length; k++) {
            springConfigHelper.excludeField(beanName, fields[k]);
          }
        }
      }

      config.addDSOSpringConfig(springConfigHelper);
    }
  }
  
  private ConfigLockLevel getLockLevel(LockLevel.Enum lockLevel, boolean autoSynchronized) {
    if (lockLevel == null || LockLevel.WRITE.equals(lockLevel)) {
      return autoSynchronized? ConfigLockLevel.AUTO_SYNCHRONIZED_WRITE : ConfigLockLevel.WRITE;
    } else if (LockLevel.CONCURRENT.equals(lockLevel)) {
      return ConfigLockLevel.CONCURRENT;
    } else if (LockLevel.READ.equals(lockLevel)) {
      return autoSynchronized? ConfigLockLevel.AUTO_SYNCHRONIZED_READ : ConfigLockLevel.READ;
    } else if (LockLevel.SYNCHRONOUS_WRITE.equals(lockLevel)) { return ConfigLockLevel.SYNCHRONOUS_WRITE; }
    throw Assert.failure("Unknown lock level " + lockLevel);
  }

  private void loadLocks(Locks lockList) {
    if (lockList == null) return;

    Autolock[] autolocks = lockList.getAutolockArray();
    for (int i = 0; autolocks != null && i < autolocks.length; i++) {
      config.addAutolock(autolocks[i].getMethodExpression(), getLockLevel(autolocks[i].getLockLevel(), autolocks[i].getAutoSynchronized()));
    }

    NamedLock[] namedLocks = lockList.getNamedLockArray();
    for (int i = 0; namedLocks != null && i < namedLocks.length; i++) {
      NamedLock namedLock = namedLocks[i];
      LockDefinition lockDefinition = new LockDefinition(namedLock.getLockName(),
                                                         getLockLevel(namedLock.getLockLevel(), false));
      lockDefinition.commit();
      config.addLock(namedLock.getMethodExpression(), lockDefinition);
    }
  }

  private void loadTransientFields(TransientFields transientFieldsList) throws ConfigurationSetupException {
    if (transientFieldsList != null) {
      String[] transientFields = transientFieldsList.getFieldNameArray();
      try {
        for (int i = 0; transientFields != null && i < transientFields.length; i++) {
          ClassSpec spec = ClassUtils.parseFullyQualifiedFieldName(transientFields[i]);
          config.addTransient(spec.getFullyQualifiedClassName(), spec.getShortFieldName());
        }
      } catch (ParseException e) {
        throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
      }
    }
  }

  private void loadInstrumentedClasses(InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      Include[] includes = instrumentedClasses.getIncludeArray();
      for (int i = 0; includes != null && i < includes.length; i++) {
        Include include = includes[i];
        IncludeOnLoad includeOnLoad = new IncludeOnLoad();
        OnLoad onLoad = include.getOnLoad();
        if (onLoad != null) {
          if (onLoad.getExecute() != null) {
            includeOnLoad = new IncludeOnLoad(IncludeOnLoad.EXECUTE, onLoad.getExecute());
          } else if (onLoad.getMethod() != null) {
            includeOnLoad = new IncludeOnLoad(IncludeOnLoad.METHOD, onLoad.getMethod());
          }
        }
        config.addInstrumentationDescriptor(new IncludedInstrumentedClass(include.getClassExpression(), include
            .getHonorTransient(), false, includeOnLoad));
      }

      String[] excludeArray = instrumentedClasses.getExcludeArray();
      for (int i = 0; excludeArray != null && i < excludeArray.length; i++) {
        config.addInstrumentationDescriptor(new ExcludedInstrumentedClass(excludeArray[i]));
      }
    }
  }

  private void loadDistributedMethods(DistributedMethods distributedMethods) {
    if (distributedMethods != null) {
      MethodExpression[] methodExpressions = distributedMethods.getMethodExpressionArray();
      for (int i = 0; methodExpressions != null && i < methodExpressions.length; i++) {
        final MethodExpression me = methodExpressions[i];
        final DistributedMethodSpec dms = new DistributedMethodSpec(me.getStringValue(), me.getRunOnAllNodes());
        config.addDistributedMethodCall(dms);
      }
    }
  }

}
