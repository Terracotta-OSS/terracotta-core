/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.FileUtils;
import org.h2.util.StringUtils;
import org.osgi.framework.Bundle;

import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.Locks;
import com.terracottatech.config.Module;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplication;
import com.terracottatech.config.WebApplications;
import com.terracottatech.config.DistributedMethods.MethodExpression;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModulesConfiguration {
  private final List<ModuleInfo> fModuleInfoList;
  private final DsoApplication   fApplication;

  public ModulesConfiguration() {
    fModuleInfoList = new ArrayList();
    fApplication = DsoApplication.Factory.newInstance();
  }

  public ModuleInfo add(Module module) {
    ModuleInfo moduleInfo = new ModuleInfo(module);
    fModuleInfoList.add(moduleInfo);
    return moduleInfo;
  }

  public ModuleInfo getOrAdd(Module module) {
    ModuleInfo moduleInfo = getModuleInfo(module);
    if (moduleInfo == null) {
      moduleInfo = add(module);
    }
    return moduleInfo;
  }

  public static boolean sameModule(Module m1, Module m2) {
    return m1 != null && m2 != null && m1.getName().equals(m2.getName()) && m1.getGroupId().equals(m2.getGroupId())
           && StringUtils.equals(m1.getVersion(), m2.getVersion());
  }

  public ModuleInfo getModuleInfo(Module module) {
    for (ModuleInfo moduleInfo : fModuleInfoList) {
      if (sameModule(module, moduleInfo.getModule())) { return moduleInfo; }
    }
    return null;
  }

  public ModuleInfo associateBundle(Bundle bundle) {
    for (ModuleInfo moduleInfo : fModuleInfoList) {
      File location = FileUtils.toFile(moduleInfo.getLocation());
      String bundleLocation = bundle.getLocation();

      if (location != null) {
        try {
          File bundleFile = new File(new URL(bundleLocation).getPath());
          String bundleFileLocation = bundleFile.getAbsolutePath();
          if (bundleFileLocation.equals(location.getAbsolutePath())) {
            moduleInfo.setBundle(bundle);
            return moduleInfo;
          }
        } catch (Exception e) {
          // e.printStackTrace();
        }
      }
    }
    return null;
  }

  public void setModuleApplication(ModuleInfo moduleInfo, DsoApplication application) {
    moduleInfo.setApplication(application);
    merge(application);
  }

  public DsoApplication getApplication() {
    return fApplication;
  }

  public void merge(DsoApplication application) {
    merge(application, fApplication);
  }

  public static void merge(DsoApplication src, DsoApplication dest) {
    if (src.isSetInstrumentedClasses()) {
      InstrumentedClasses srcInstrumentedClasses = src.getInstrumentedClasses();
      InstrumentedClasses destInstrumentedClasses = dest.isSetInstrumentedClasses() ? dest.getInstrumentedClasses()
          : dest.addNewInstrumentedClasses();
      merge(srcInstrumentedClasses, destInstrumentedClasses);
    }

    if (src.isSetTransientFields()) {
      TransientFields srcTransientFields = src.getTransientFields();
      TransientFields destTransientFields = dest.isSetTransientFields() ? dest.getTransientFields() : dest
          .addNewTransientFields();
      merge(srcTransientFields, destTransientFields);
    }

    if (src.isSetLocks()) {
      Locks srcLocks = src.getLocks();
      Locks destLocks = dest.isSetLocks() ? dest.getLocks() : dest.addNewLocks();
      merge(srcLocks, destLocks);
    }

    if (src.isSetRoots()) {
      Roots srcRoots = src.getRoots();
      Roots destRoots = dest.isSetRoots() ? dest.getRoots() : dest.addNewRoots();
      merge(srcRoots, destRoots);
    }

    if (src.isSetDistributedMethods()) {
      DistributedMethods srcDistributedMethods = src.getDistributedMethods();
      DistributedMethods destDistributedMethods = dest.isSetDistributedMethods() ? dest.getDistributedMethods() : dest
          .addNewDistributedMethods();
      merge(srcDistributedMethods, destDistributedMethods);
    }

    if (src.isSetAdditionalBootJarClasses()) {
      AdditionalBootJarClasses srcBootClasses = src.getAdditionalBootJarClasses();
      AdditionalBootJarClasses destBootClasses = dest.isSetAdditionalBootJarClasses() ? dest
          .getAdditionalBootJarClasses() : dest.addNewAdditionalBootJarClasses();
      merge(srcBootClasses, destBootClasses);
    }

    if (src.isSetWebApplications()) {
      WebApplications srcWebApps = src.getWebApplications();
      WebApplications destWebApps = dest.isSetWebApplications() ? dest.getWebApplications() : dest
          .addNewWebApplications();
      merge(srcWebApps, destWebApps);
    }

    if (src.isSetDsoReflectionEnabled()) {
      dest.setDsoReflectionEnabled(src.getDsoReflectionEnabled());
    }
  }

  public static void merge(InstrumentedClasses src, InstrumentedClasses dest) {
    Include[] includes = src.getIncludeArray();
    List<Include> includeList = new ArrayList(Arrays.asList(includes));

    for (Include include : dest.getIncludeArray()) {
      includeList.add((Include) include.copy());
    }
    dest.setIncludeArray(includeList.toArray(new Include[0]));
  }

  public static void merge(TransientFields src, TransientFields dest) {
    String[] fields = src.getFieldNameArray();
    List<String> fieldList = new ArrayList(Arrays.asList(fields));

    fieldList.addAll(Arrays.asList(dest.getFieldNameArray()));
    dest.setFieldNameArray(fieldList.toArray(new String[0]));
  }

  public static void merge(Locks src, Locks dest) {
    Autolock[] autolocks = src.getAutolockArray();
    List<Autolock> autolockList = new ArrayList(Arrays.asList(autolocks));

    for (Autolock autolock : dest.getAutolockArray()) {
      autolockList.add((Autolock) autolock.copy());
    }
    dest.setAutolockArray(autolockList.toArray(new Autolock[0]));

    NamedLock[] namedLocks = src.getNamedLockArray();
    List<NamedLock> namedLockList = new ArrayList(Arrays.asList(namedLocks));

    for (NamedLock namedLock : dest.getNamedLockArray()) {
      namedLockList.add((NamedLock) namedLock.copy());
    }
    dest.setNamedLockArray(namedLockList.toArray(new NamedLock[0]));
  }

  public static void merge(Roots src, Roots dest) {
    Root[] roots = src.getRootArray();
    List<Root> rootList = new ArrayList(Arrays.asList(roots));

    for (Root root : dest.getRootArray()) {
      rootList.add((Root) root.copy());
    }
    dest.setRootArray(rootList.toArray(new Root[0]));
  }

  public static void merge(DistributedMethods src, DistributedMethods dest) {
    MethodExpression[] methodExprs = src.getMethodExpressionArray();
    List<MethodExpression> exprList = new ArrayList(Arrays.asList(methodExprs));

    for (MethodExpression expr : dest.getMethodExpressionArray()) {
      exprList.add((MethodExpression) expr.copy());
    }
    dest.setMethodExpressionArray(exprList.toArray(new MethodExpression[0]));
  }

  public static void merge(AdditionalBootJarClasses src, AdditionalBootJarClasses dest) {
    String[] includes = src.getIncludeArray();
    List<String> includeList = new ArrayList(Arrays.asList(includes));

    includeList.addAll(Arrays.asList(dest.getIncludeArray()));
    dest.setIncludeArray(includeList.toArray(new String[0]));
  }

  public static void merge(WebApplications src, WebApplications dest) {
    WebApplication[] webApps = src.getWebApplicationArray();
    List<WebApplication> webAppList = new ArrayList(Arrays.asList(webApps));

    for (WebApplication webApp : dest.getWebApplicationArray()) {
      webAppList.add((WebApplication) webApp.copy());
    }
    dest.setWebApplicationArray(webAppList.toArray(new WebApplication[0]));
  }

}
