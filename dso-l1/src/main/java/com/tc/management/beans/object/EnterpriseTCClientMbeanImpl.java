/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.management.TCClient;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;

public class EnterpriseTCClientMbeanImpl extends AbstractNotifyingMBean implements EnterpriseTCClientMbean {
  private TCClient client;

  public EnterpriseTCClientMbeanImpl(TCClient client) throws NotCompliantMBeanException {
    super(EnterpriseTCClientMbean.class);
    this.client = client;
  }

  @Override
  public void reset() {
    //
  }

  @Override
  public synchronized void reloadConfiguration() throws ConfigurationSetupException {
    client.reloadConfiguration();
  }

}
