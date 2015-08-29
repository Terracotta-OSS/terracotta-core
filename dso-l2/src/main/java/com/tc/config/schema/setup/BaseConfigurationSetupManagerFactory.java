/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.net.core.SecurityInfo;

/**
 * A base class for all {@link com.tc.config.schema.setup.ConfigurationSetupManagerFactory} instances.
 */
public abstract class BaseConfigurationSetupManagerFactory implements ConfigurationSetupManagerFactory {

  protected final ConfigBeanFactory beanFactory;

  public BaseConfigurationSetupManagerFactory() {
    this.beanFactory = new TerracottaDomainConfigurationDocumentBeanFactory();
  }
}
