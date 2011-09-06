/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import java.util.Map;

import javax.management.ObjectName;

public interface MBeanSpec extends OsgiServiceSpec {
  public Map<ObjectName, Object> getMBeans();
}
