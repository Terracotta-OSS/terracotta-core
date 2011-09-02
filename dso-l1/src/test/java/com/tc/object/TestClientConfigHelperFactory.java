/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.object.config.DSOClientConfigHelper;

public interface TestClientConfigHelperFactory {

  public DSOClientConfigHelper createClientConfigHelper() throws ConfigurationSetupException;

}