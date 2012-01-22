/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import java.util.Map;

import javax.management.ObjectName;

public interface MBeanSpec extends OsgiServiceSpec {
  public Map<ObjectName, Object> getMBeans();
}
