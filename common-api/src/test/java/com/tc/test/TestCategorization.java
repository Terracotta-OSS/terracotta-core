/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test;

import org.terracotta.license.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestCategorization {
  private final Map<String, TestCategory> testCategoryMap = new HashMap();

  /**
   * Populates the test categorization mapping with the data from the given properties object. Property names are
   * assumed to be test class names, and property values are the corresponding test category for the test class.
   */
  public TestCategorization(Properties properties) {
    loadFromProperties(properties);
  }

  /**
   * Populates the test categorization mapping with data loaded from the given URL. The resource referred to by the URL
   * should be in standard Java properties format, with property names and values as described in
   * {@link #TestCategorization(Properties)}
   * 
   * @throws IOException if the properties cannot be read from the given URL.
   */
  public TestCategorization(URL url) throws IOException {
    Properties properties = new Properties();
    InputStream inputStream = null;
    try {
      inputStream = url.openStream();
      properties.load(inputStream);
      this.loadFromProperties(properties);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * Returns the category of the given test class, or TestCategory.UNCATEGORIZED if there is no category for the test
   * class.
   * 
   * @param fullyQualifiedTestClassName The fully qualified name of the test class.
   */
  public TestCategory getTestCategory(String fullyQualifiedTestClassName) {
    TestCategory result = testCategoryMap.get(fullyQualifiedTestClassName);
    if (result == null) {
      result = TestCategory.UNCATEGORIZED;
    }
    return result;
  }

  /**
   * Returns the category of the given test class, or TestCategory.UNCATEGORIZED if there is no category for the test
   * class.
   * 
   * @param fullyQualifiedTestClassName The Class object for the test class.
   */
  public TestCategory getTestCategory(Class testClass) {
    return getTestCategory(testClass.getName());
  }

  private void loadFromProperties(Properties properties) {
    Enumeration<?> e = properties.propertyNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String categoryString = properties.getProperty(key).toUpperCase();

      testCategoryMap.put(key, Enum.valueOf(TestCategory.class, categoryString.trim().toUpperCase()));
    }
  }
}
