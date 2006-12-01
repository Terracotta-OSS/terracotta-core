/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StandardDSOSpringConfigHelper implements DSOSpringConfigHelper {
  private final List applicationNamePatterns = new ArrayList();
  private final List configPatterns          = new ArrayList();
  private boolean    fastProxyEnabled        = false;
  private final List distributedEvents       = new ArrayList();

  /**
   * Map of <code>String</code> bean name to <code>Set</code> of the excluded fields.
   */
  private final Map  beans                   = new HashMap();

  public boolean isMatchingApplication(String applicationName) {
    for (Iterator it = applicationNamePatterns.iterator(); it.hasNext();) {
      if (isMatching((String) it.next(), applicationName)) { return true; }
    }
    return false;
  }

  public boolean isMatchingConfig(String configPath) {
    for (Iterator it = configPatterns.iterator(); it.hasNext();) {
      if (isMatching((String) it.next(), configPath)) { return true; }
    }
    return false;
  }

  protected boolean isMatching(String pattern, String s) {
    if ("*".equals(pattern)) {
      return true;
    } else if (s == null) {
      return false;
    } else if (pattern.startsWith("*")) {
      if (pattern.endsWith("*")) {
        return s.indexOf(pattern.substring(1, pattern.length() - 1)) > -1;
      } else {
        return s.endsWith(pattern.substring(1));
      }
    } else if (pattern.endsWith("*")) { return s.startsWith(pattern.substring(0, pattern.length() - 1)); }
    return pattern.equals(s);
  }

  public boolean isDistributedEvent(String className) {
    for (Iterator it = distributedEvents.iterator(); it.hasNext();) {
      String expression = (String) it.next();
      if (isMatching(expression, className)) { return true; }
    }
    return false;
  }

  public boolean isDistributedBean(String beanName) {
    return this.beans.containsKey(beanName);
  }

  public boolean isDistributedField(String beanName, String fieldName) {
    Set excludedFields = (Set) this.beans.get(beanName);
    return excludedFields == null || !excludedFields.contains(fieldName);
  }

  public List getDistributedEvents() {
    return distributedEvents;
  }

  public Map getDistributedBeans() {
    return this.beans;
  }

  public void addApplicationNamePattern(String pattern) {
    this.applicationNamePatterns.add(pattern);
  }

  public void addConfigPattern(String pattern) {
    this.configPatterns.add(pattern);
  }

  public void addDistributedEvent(String expression) {
    distributedEvents.add(expression);
  }

  public void addBean(String beanName) {
    this.beans.put(beanName, new HashSet());
  }

  public void excludeField(String beanName, String fieldName) {
    Set excludedFields = (Set) this.beans.get(beanName);
    if (excludedFields == null) {
      excludedFields = new HashSet();
      this.beans.put(beanName, excludedFields);
    }
    excludedFields.add(fieldName);
  }

  public boolean isFastProxyEnabled() {
    return fastProxyEnabled;
  }

  public void setFastProxyEnabled(boolean fastProxyEnabled) {
    this.fastProxyEnabled = fastProxyEnabled;
  }

}
