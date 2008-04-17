/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.Namespace;

import java.util.Iterator;

/**
 * ApplicationHelper
 * 
 * @author Eugene Kuleshov
 */
public class ApplicationHelper {
  private final transient Log logger          = LogFactory.getLog(getClass());

  private static final String STANDALONE_APP  = "";

  static final String TOMCAT_PREFIX = Namespace.TOMCAT_NAMESPACE;
  static final String WEBLOGIC_PREFIX = Namespace.WEBLOGIC_NAMESPACE;
  static final String JETTY_PREFIX    = Namespace.JETTY_NAMESPACE;
  static final String ROOT_APP_NAME = ClassProcessorHelper.ROOT_WEB_APP_NAME;

  private String              appName;
  private DSOContext          dsoContext;

  public ApplicationHelper(Class c) {
    ClassLoader cl = c.getClassLoader();

    this.dsoContext = ClassProcessorHelper.getContext(cl);

    if (cl instanceof NamedClassLoader) {
      this.appName = getAppNameFrom((NamedClassLoader)cl);
    } else {
      this.appName = STANDALONE_APP;
    }
  }

  private String getAppNameFrom(NamedClassLoader cl) {
    String name = cl.__tc_getClassLoaderName();
    logger.info("Application name " + name);
    if (name != null) {
      if (name.startsWith(TOMCAT_PREFIX) || name.startsWith(JETTY_PREFIX)) {
        name = parseAppNameFromContextPath(name, '/', ROOT_APP_NAME);
        
      } else if (name.startsWith(WEBLOGIC_PREFIX)) {
        name = parseAppNameFromContextPath(name, '@', name);
        
        // for weblogic 9.2
        if (name.endsWith(".war")) {
          name = name.substring(0, name.length() - 4);
        }
      }
    }
    return name;
  }
  
  private String parseAppNameFromContextPath(String fullPath, char delim, String defaultTo){
    int index = fullPath.lastIndexOf(delim);
    if (index > -1) {
      return fullPath.substring(index + 1);
    }
    return defaultTo;
  }

  public boolean isDSOApplication() {
    return this.dsoContext != null && this.appName != null;
  }

  public String getAppName() {
    return appName;
  }

  public DSOContext getDsoContext() {
    return dsoContext;
  }

  public boolean isFastProxyEnabled() {
    for (Iterator it = this.dsoContext.getDSOSpringConfigHelpers().iterator(); it.hasNext();) {
      DSOSpringConfigHelper springConfigHelper = (DSOSpringConfigHelper) it.next();
      if (springConfigHelper.isMatchingApplication(this.appName)) { return springConfigHelper.isFastProxyEnabled(); }
    }
    return false;
  }
}
