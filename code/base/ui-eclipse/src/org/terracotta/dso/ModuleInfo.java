/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;

import java.io.File;

public class ModuleInfo {
  private Module fModule;
  private File fLocation;
  private Bundle fBundle;
  private BundleException fBundleException;
  private DsoApplication fApplication;
  
  public ModuleInfo(Module module) {
    fModule = module;
  }

  public Module getModule() {
    return fModule;
  }
  
  public void setLocation(File location) {
    fLocation = location;
  }
  
  public File getLocation() {
    return fLocation;
  }
  
  public void setBundle(Bundle bundle) {
    fBundle = bundle;
  }
  
  public Bundle getBundle() {
    return fBundle;
  }
  
  public void setError(BundleException error) {
    fBundleException = error;
  }
  
  public BundleException getError() {
    return fBundleException;
  }
  
  public void setApplication(DsoApplication application) {
    fApplication = application;
  }
  
  public DsoApplication getApplication() {
    return fApplication;
  }
}
