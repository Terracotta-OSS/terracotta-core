/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.terracotta.modules.tool.config.Config;

import java.io.IOException;
import java.util.Properties;

public class TestConfig {

  static Config createTestConfig() {
    Properties props = new Properties();
    try {
      props.load(TestConfig.class.getResourceAsStream("/tim-get-testing.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new Config(props);
  }

}
