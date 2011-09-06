/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

public final class PropertiesInterpolatorTest extends TestCase {
  protected Properties             props;
  protected PropertiesInterpolator interpolator;

  @Override
  public void setUp() {
    props = new Properties();
    try {
      props.load(getClass().getResourceAsStream("/PropertiesInterpolaterTest.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    interpolator = new PropertiesInterpolator();
  }

  public final void testInterpolated() {
    /*
     * basic.property = hello use.basic.property = ${basic.property} use.system.property = ${user.home}/.tc
     * use.environment.variable = ${USER} use.invalid.syntax = ${non-existant-property} use.multiple = ${os.name} -
     * ${java.vm.name}${java.vm.version} ${JAVA_HOME}
     */
    props = interpolator.interpolated(props);
    assertPropertyEquals("basic.property", "hello");
    assertPropertyEquals("use.basic.property", "hello");
    assertPropertyEquals("use.system.property", System.getProperty("user.home") + "/.tc");
    assertPropertyEquals("use.environment.variable", System.getenv("USER"));
    assertPropertyEquals("use.invalid.syntax", "${non-existant-property}");
    assertPropertyEquals("use.multiple", System.getProperty("os.name") + " - " + System.getProperty("java.vm.name")
                                         + System.getProperty("java.vm.version") + " " + System.getenv("USER"));
  }

  private void assertPropertyEquals(String key, String expectedValue) {
    assertEquals(expectedValue, props.getProperty(key));
  }
}
