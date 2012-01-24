/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;


/**
 * Contains methods that expose whole-system config.
 */
public interface SystemConfig extends Config {

  ConfigurationModel configurationModel();

}
