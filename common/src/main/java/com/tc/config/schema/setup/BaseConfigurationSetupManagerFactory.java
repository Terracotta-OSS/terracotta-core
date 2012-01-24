/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.util.Assert;

/**
 * A base class for all {@link com.tc.config.schema.setup.ConfigurationSetupManagerFactory} instances.
 */
public abstract class BaseConfigurationSetupManagerFactory implements ConfigurationSetupManagerFactory {

  protected final IllegalConfigurationChangeHandler illegalChangeHandler;
  
  protected final ConfigBeanFactory    beanFactory;
  protected final DefaultValueProvider defaultValueProvider;
  protected final XmlObjectComparator  xmlObjectComparator;

  public BaseConfigurationSetupManagerFactory(IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    Assert.assertNotNull(illegalConfigurationChangeHandler);
    
    this.illegalChangeHandler = illegalConfigurationChangeHandler;
    
    this.beanFactory = new TerracottaDomainConfigurationDocumentBeanFactory();
    this.defaultValueProvider = new SchemaDefaultValueProvider();
    this.xmlObjectComparator = new StandardXmlObjectComparator();
  }

}
