/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.net.protocol.transport;

import org.junit.Test;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;

import static com.tc.net.protocol.transport.ConnectionHealthCheckerImpl.HealthCheckerMonitorThreadEngine;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Shelestovich
 */
public class HealthCheckerMonitorThreadEngineTest {

  private static final TCLogger logger = TCLogging.getLogger(HealthCheckerMonitorThreadEngineTest.class);

  @Test
  public void testAllowCheckTimeIfEnabledInConfig() {
    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor("l2.healthcheck.l2");
    // enable time checking
    props.setProperty("checkTime.enabled", "true");
    // ignore interval
    props.setProperty("checkTime.interval", "-1");
    HealthCheckerConfigImpl config = new HealthCheckerConfigImpl(props, "test-config");
    final HealthCheckerMonitorThreadEngine engine = new HealthCheckerMonitorThreadEngine(config, null, logger);

    assertTrue(engine.canCheckTime());
  }

  @Test
  public void testDisallowCheckTimeIfDisabledInConfig() {
    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor("l2.healthcheck.l2");
    // disable time checking
    props.setProperty("checkTime.enabled", "false");
    HealthCheckerConfigImpl config = new HealthCheckerConfigImpl(props, "test-config");
    final HealthCheckerMonitorThreadEngine engine = new HealthCheckerMonitorThreadEngine(config, null, logger);

    assertFalse(engine.canCheckTime());
  }

  @Test
  public void testDisallowCheckTimeIfIntervalNotExceeded() {
    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor("l2.healthcheck.l2");
    // set short interval
    props.setProperty("checkTime.interval", "900000");
    HealthCheckerConfigImpl config = new HealthCheckerConfigImpl(props, "test-config");
    final HealthCheckerMonitorThreadEngine engine = new HealthCheckerMonitorThreadEngine(config, null, logger);

    assertFalse(engine.canCheckTime());
  }

}
