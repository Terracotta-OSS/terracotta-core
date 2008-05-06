/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.listen;

import com.tc.config.schema.MockXmlObject;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link ConfigurationChangeListenerSet}.
 */
public class ConfigurationChangeListenerSetTest extends TCTestCase {

  private MockConfigurationChangeListener listener1;
  private MockConfigurationChangeListener listener2;

  private ConfigurationChangeListenerSet  set;

  private MockXmlObject                   config1;
  private MockXmlObject                   config2;

  public void setUp() throws Exception {
    this.listener1 = new MockConfigurationChangeListener();
    this.listener2 = new MockConfigurationChangeListener();

    this.set = new ConfigurationChangeListenerSet();

    this.config1 = new MockXmlObject();
    this.config2 = new MockXmlObject();
  }

  public void testAll() throws Exception {
    try {
      this.set.removeListener(null);
      fail("Didn't get NPE on null listener");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      this.set.addListener(null);
      fail("Didn't get NPE on null listener");
    } catch (NullPointerException npe) {
      // ok
    }

    check(0, 0, null, null);

    this.set.configurationChanged(this.config1, this.config2);
    check(0, 0, null, null);

    this.set.addListener(this.listener1);
    check(0, 0, null, null);

    this.set.configurationChanged(this.config1, this.config2);
    check(1, 0, this.config1, this.config2);

    this.set.addListener(this.listener1);
    check(0, 0, null, null);

    this.set.configurationChanged(this.config1, this.config2);
    check(1, 0, this.config1, this.config2);

    this.set.addListener(this.listener2);
    check(0, 0, null, null);

    this.set.configurationChanged(this.config2, this.config1);
    check(1, 1, this.config2, this.config1);

    this.set.removeListener(this.listener1);
    check(0, 0, null, null);

    this.set.configurationChanged(this.config1, this.config2);
    check(0, 1, this.config1, this.config2);
  }

  private void check(int numOne, int numTwo, MockXmlObject oldConfig, MockXmlObject newConfig) {
    assertEquals(numOne, this.listener1.getNumConfigurationChangeds());
    if (numOne > 0) {
      assertSame(oldConfig, this.listener1.getLastOldConfig());
      assertSame(newConfig, this.listener1.getLastNewConfig());
    }

    assertEquals(numTwo, this.listener2.getNumConfigurationChangeds());
    if (numTwo > 0) {
      assertSame(oldConfig, this.listener2.getLastOldConfig());
      assertSame(newConfig, this.listener2.getLastNewConfig());
    }

    this.listener1.reset();
    this.listener2.reset();
  }

}
