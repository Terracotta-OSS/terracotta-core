/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.test.EqualityChecker;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link ConfigurationModel}.
 */
public class ConfigurationModelTest extends TCTestCase {

  public void testAll() throws Exception {
    Object[] arr1 = new Object[] { ConfigurationModel.DEVELOPMENT, ConfigurationModel.PRODUCTION };
    Object[] arr2 = new Object[] { ConfigurationModel.DEVELOPMENT, ConfigurationModel.PRODUCTION };

    EqualityChecker.checkArraysForEquality(arr1, arr2);

    assertFalse(ConfigurationModel.DEVELOPMENT.equals(null));
    assertFalse(ConfigurationModel.DEVELOPMENT.equals("development"));
    assertFalse(ConfigurationModel.DEVELOPMENT.equals("foo"));

    ConfigurationModel.DEVELOPMENT.toString();
    ConfigurationModel.PRODUCTION.toString();
  }

}
