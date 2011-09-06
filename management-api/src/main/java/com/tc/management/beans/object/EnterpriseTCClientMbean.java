/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.management.TerracottaMBean;

public interface EnterpriseTCClientMbean extends TerracottaMBean {
  void reloadConfiguration() throws ConfigurationSetupException;
}