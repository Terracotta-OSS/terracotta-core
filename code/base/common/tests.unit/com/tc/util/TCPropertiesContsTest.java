/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class TCPropertiesContsTest extends TCTestCase {

  // This file resides in src.resource/com/tc/properties directory
  private static final String DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  private final Properties    props                      = new Properties();

  @Override
  protected void setUp() {
    loadDefaults(DEFAULT_TC_PROPERTIES_FILE);
  }

  private void loadDefaults(String propFile) {
    InputStream in = TCPropertiesImpl.class.getResourceAsStream(propFile);
    if (in == null) { throw new AssertionError("TC Property file " + propFile + " not Found"); }
    try {
      System.out.println("Loading default properties from " + propFile);
      props.load(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void testAllConstsDeclared() {
    Set<String> tcPropertiesConsts = new HashSet<String>();
    Field[] fields = TCPropertiesConsts.class.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      try {
        tcPropertiesConsts.add(fields[i].get(null).toString());
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
    Set tcProperties = props.keySet();
    for (Iterator iter = tcProperties.iterator(); iter.hasNext();) {
      String tcProperty = (String) iter.next();
      Assert
          .assertTrue("There is no constant declared for " + tcProperty + " in " + TCPropertiesConsts.class.getName(),
                      tcPropertiesConsts.contains(tcProperty));
    }
  }
}
