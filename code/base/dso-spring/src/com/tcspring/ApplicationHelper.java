/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.loaders.NamedClassLoader;

import java.util.Iterator;

/**
 * ApplicationHelper
 *
 * @author Eugene Kuleshov
 */
public class ApplicationHelper {
  private final transient Log logger          = LogFactory.getLog(getClass());

  private static final String STANDALONE_APP  = "";

  // Those prefixes defined in com.tc.object.loaders.Namespace which is not visible from this class
  private static final String TOMCAT_PREFIX   = "Tomcat.context:/";
  private static final String WEBLOGIC_PREFIX = "Weblogic.";

  private String              appName;
  private DSOContext          dsoContext;

  public ApplicationHelper(Class c) {
    ClassLoader cl = c.getClassLoader();

    try {
      this.dsoContext = ClassProcessorHelper.getContext(cl);

      if (cl instanceof NamedClassLoader) {
        String name = ((NamedClassLoader) cl).__tc_getClassLoaderName();
        logger.info("Application name " + name);
        if (name != null) {
          if (name.startsWith(TOMCAT_PREFIX)) {
            name = name.substring(TOMCAT_PREFIX.length());
          } else if (name.startsWith(WEBLOGIC_PREFIX)) {
            int n = name.lastIndexOf('@');
            if (n > -1) {
              name = name.substring(n + 1);
            }

            // for weblogic 9.2
            if (name.endsWith(".war")) {
              name = name.substring(0, name.length() - 4);
            }
          }
        }
        this.appName = name;
      } else {
        this.appName = STANDALONE_APP;
      }

    } catch (Exception e) {
      // TODO find a better way
    }
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
