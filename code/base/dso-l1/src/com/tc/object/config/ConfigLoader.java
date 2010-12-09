/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.ArrayUtils;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.TCLogger;
import com.tc.object.bytecode.SessionConfiguration;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.AppGroup;
import com.terracottatech.config.AppGroups;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DistributedMethods.MethodExpression;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InjectedField;
import com.terracottatech.config.InjectedInstances;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.OnLoad;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplication;
import com.terracottatech.config.WebApplications;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ConfigLoader {
  private final DSOClientConfigHelper config;
  private final TCLogger              logger;

  public ConfigLoader(final DSOClientConfigHelper config, final TCLogger logger) {
    this.config = config;
    this.logger = logger;
  }

  public void loadDsoConfig(final DsoApplication dsoApplication) throws ConfigurationSetupException {
    if (dsoApplication == null) return;

    addRoots(dsoApplication.getRoots());
    addWebApplications(dsoApplication.getWebApplications());
    addAppGroups(dsoApplication.getAppGroups());

    loadLocks(dsoApplication.getLocks());
    loadTransientFields(dsoApplication.getTransientFields());
    loadInstrumentedClasses(dsoApplication.getInstrumentedClasses());
    loadDistributedMethods(dsoApplication.getDistributedMethods());
    loadInjectedInstances(dsoApplication.getInjectedInstances());

    addAdditionalBootJarClasses(dsoApplication.getAdditionalBootJarClasses());
  }

  private void addRoot(final Root root) throws ConfigurationSetupException {
    String rootName = root.getRootName();
    String fieldName = root.getFieldName();
    String fieldExpression = root.getFieldExpression();

    // XXX: Can't enforce this via XML Schema - yet, the version of xml beans that
    // we are using does not correctly support substitutionGroups yet
    if (fieldName != null && fieldExpression != null) {
      String message = "Ambiguous root definition";
      if (rootName != null) message += " for root named '" + rootName + "'";
      message += ": specify the field-name or a field-expression, but not both";
      throw new ConfigurationSetupException(message);
    }

    if (fieldName != null) {
      try {
        ClassSpec classSpec = ClassUtils.parseFullyQualifiedFieldName(fieldName);
        String className = classSpec.getFullyQualifiedClassName();
        config.addRoot(new com.tc.object.config.Root(className, classSpec.getShortFieldName(), rootName), false);
      } catch (ParseException pe) {
        throw new ConfigurationSetupException("Root '" + root.getFieldName() + "' is invalid", pe);
      }
    } else if (fieldExpression != null) {
      config.addRoot(new com.tc.object.config.Root(fieldExpression, rootName), false);
    } else {
      String message = "Incomplete root definition";
      if (rootName != null) message += " for root named '" + rootName + "'";
      message += ": the value for the field-name or the field-expression must be declared.";
      throw new ConfigurationSetupException(message);
    }
  }

  private void addRoots(final Roots rootsList) throws ConfigurationSetupException {
    if (rootsList != null && rootsList.getRootArray() != null) {
      Root[] roots = rootsList.getRootArray();
      for (int i = 0; i < roots.length; ++i) {
        addRoot(roots[i]);
      }
    }
  }

  private void addWebApplication(final WebApplication webApp) {
    int lockType = webApp.getSynchronousWrite() ? com.tc.object.locks.LockLevel.SYNCHRONOUS_WRITE.toInt()
        : com.tc.object.locks.LockLevel.WRITE.toInt();

    final boolean sessionLocking;

    if (webApp.isSetSessionLocking()) {
      // if explicitly set, then use the configured value
      sessionLocking = webApp.getSessionLocking();
    } else {
      // otherwise serialization mode determines the locking
      sessionLocking = !webApp.getSerialization();
    }

    config.addWebApplication(webApp.getStringValue(),
                             new SessionConfiguration(lockType, sessionLocking, webApp.getSerialization()));
  }

  private void addWebApplications(final WebApplications webApplicationsList) {
    if (webApplicationsList != null && webApplicationsList.getWebApplicationArray() != null) {
      WebApplication[] webApplications = webApplicationsList.getWebApplicationArray();
      for (WebApplication webApplication : webApplications) {
        addWebApplication(webApplication);
      }
    }
  }

  private void addAppGroup(final AppGroup appGroup) {
    if (appGroup != null) {
      String appGroupName = appGroup.getName();
      if (appGroupName != null && appGroupName.length() > 0) {
        String[] webAppNames = appGroup.getWebApplicationArray();
        String[] namedClassloaders = appGroup.getNamedClassloaderArray();
        config.addToAppGroup(appGroupName, namedClassloaders, webAppNames);
      }
    }
  }

  private void addAppGroups(final AppGroups appGroups) {
    if (appGroups != null) {
      AppGroup[] appGroupArray = appGroups.getAppGroupArray();
      if (appGroupArray != null) {
        for (AppGroup appGroup : appGroupArray) {
          addAppGroup(appGroup);
        }
      }
    }
  }

  private void addAdditionalBootJarClasses(final AdditionalBootJarClasses additionalBootJarClassesList) {
    // XXX
    if (additionalBootJarClassesList == null) return;

    Set userDefinedBootClassNames = new HashSet();
    userDefinedBootClassNames.addAll(Arrays.asList(additionalBootJarClassesList.getIncludeArray()));
    logger.debug("Additional boot-jar classes: " + ArrayUtils.toString(userDefinedBootClassNames));

    for (Iterator i = userDefinedBootClassNames.iterator(); i.hasNext();) {
      addAdditionalBootJarClass((String) i.next());
    }
  }

  private void addAdditionalBootJarClass(final String className) {
    TransparencyClassSpec spec = config.getSpec(className);
    if (spec == null) {
      spec = new TransparencyClassSpecImpl(className, config);
      spec.markPreInstrumented();
      config.addUserDefinedBootSpec(spec.getClassName(), spec);
    } else if (!spec.isPreInstrumented()) {
      // DEV-458: if the class being added to the boot jar defines locks/distributed methods/etc. it creates a
      // spec but does not pre-instrument it. This makes sure that the adapted code gets into the boot jar in
      // this case.
      // DEV-1110: ignore java.lang.Object
      if (!"java.lang.Object".equals(className)) {
        spec.markPreInstrumented();
      }
    }
  }

  private ConfigLockLevel getLockLevel(final LockLevel.Enum lockLevel, final boolean autoSynchronized) {
    if (lockLevel == null || LockLevel.WRITE.equals(lockLevel)) {
      return autoSynchronized ? ConfigLockLevel.AUTO_SYNCHRONIZED_WRITE : ConfigLockLevel.WRITE;
    } else if (LockLevel.CONCURRENT.equals(lockLevel)) {
      return autoSynchronized ? ConfigLockLevel.AUTO_SYNCHRONIZED_CONCURRENT : ConfigLockLevel.CONCURRENT;
    } else if (LockLevel.READ.equals(lockLevel)) {
      return autoSynchronized ? ConfigLockLevel.AUTO_SYNCHRONIZED_READ : ConfigLockLevel.READ;
    } else if (LockLevel.SYNCHRONOUS_WRITE.equals(lockLevel)) { return autoSynchronized ? ConfigLockLevel.AUTO_SYNCHRONIZED_SYNCHRONOUS_WRITE
        : ConfigLockLevel.SYNCHRONOUS_WRITE; }
    throw Assert.failure("Unknown lock level " + lockLevel);
  }

  private static void gatherNamespaces(final XmlObject x, final Map nsMap) {
    XmlCursor c = x.newCursor();
    while (!c.isContainer())
      c.toNextToken();
    c.getAllNamespaces(nsMap);
    c.dispose();
  }

  private static String xmlObject2Text(final XmlObject xmlObject, final XmlOptions options) {
    return xmlObject.xmlText(options);
  }

  private void loadLocks(final Locks lockList) {
    if (lockList == null) return;

    XmlOptions options = new XmlOptions();
    options.setSaveOuter();
    options.setSavePrettyPrint();
    options.setSavePrettyPrintIndent(2);
    Map nsMap = new HashMap();
    gatherNamespaces(lockList, nsMap);
    options.setSaveImplicitNamespaces(nsMap);

    Autolock[] autolocks = lockList.getAutolockArray();
    for (int i = 0; autolocks != null && i < autolocks.length; i++) {
      Autolock autolock = autolocks[i];
      config.addAutolock(autolock.getMethodExpression(),
                         getLockLevel(autolock.getLockLevel(), autolock.getAutoSynchronized()),
                         xmlObject2Text(autolock, options));
    }

    NamedLock[] namedLocks = lockList.getNamedLockArray();
    for (int i = 0; namedLocks != null && i < namedLocks.length; i++) {
      NamedLock namedLock = namedLocks[i];
      LockDefinition lockDefinition = new LockDefinitionImpl(namedLock.getLockName(),
                                                             getLockLevel(namedLock.getLockLevel(), false),
                                                             xmlObject2Text(namedLock, options));
      lockDefinition.commit();
      config.addLock(namedLock.getMethodExpression(), lockDefinition);
    }
  }

  private void loadTransientFields(final TransientFields transientFieldsList) throws ConfigurationSetupException {
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

  private void loadInstrumentedClasses(final InstrumentedClasses instrumentedClasses)
      throws ConfigurationSetupException {
    if (instrumentedClasses != null) {
      // Call selectPath() rather than using getIncludeArray and getExcludeArray(),
      // because we need to preserve the relative order of includes and excludes.
      XmlObject[] elements = instrumentedClasses.selectPath("*");
      for (int i = 0; elements != null && i < elements.length; ++i) {
        if (elements[i] instanceof Include) {
          Include include = (Include) elements[i];
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
        } else if (elements[i] instanceof ClassExpression) {
          String expr = ((ClassExpression) elements[i]).getStringValue();
          config.addInstrumentationDescriptor(new ExcludedInstrumentedClass(expr));
        } else {
          throw new ConfigurationSetupException(
                                                "The following element was unexpected within an <instrumented-classes> element:"
                                                    + elements[i]);
        }
      }
    }
  }

  private void loadDistributedMethods(final DistributedMethods distributedMethods) {
    if (distributedMethods != null) {
      MethodExpression[] methodExpressions = distributedMethods.getMethodExpressionArray();
      for (int i = 0; methodExpressions != null && i < methodExpressions.length; i++) {
        final MethodExpression me = methodExpressions[i];
        final DistributedMethodSpec dms = new DistributedMethodSpec(me.getStringValue(), me.getRunOnAllNodes());
        config.addDistributedMethodCall(dms);
      }
    }
  }

  private void loadInjectedInstances(final InjectedInstances injectedInstances) throws ConfigurationSetupException {
    if (injectedInstances != null) {
      InjectedField[] injectedFields = injectedInstances.getInjectedFieldArray();
      try {
        for (InjectedField injectedField : injectedFields) {
          ClassSpec spec = ClassUtils.parseFullyQualifiedFieldName(injectedField.getFieldName());
          config.addInjectedField(spec.getFullyQualifiedClassName(), spec.getShortFieldName(),
                                  injectedField.getInstanceType());
        }
      } catch (ParseException e) {
        throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
      }
    }
  }
}