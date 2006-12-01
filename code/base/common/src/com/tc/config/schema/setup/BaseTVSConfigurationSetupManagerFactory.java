/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.defaults.FromSchemaDefaultValueProvider;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.util.Assert;

/**
 * A base class for all {@link com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory} instances.
 */
public abstract class BaseTVSConfigurationSetupManagerFactory implements TVSConfigurationSetupManagerFactory {

  protected final IllegalConfigurationChangeHandler illegalChangeHandler;
  
  protected final ConfigBeanFactory    beanFactory;
  protected final DefaultValueProvider defaultValueProvider;
  protected final XmlObjectComparator  xmlObjectComparator;

  public BaseTVSConfigurationSetupManagerFactory(IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    Assert.assertNotNull(illegalConfigurationChangeHandler);
    
    this.illegalChangeHandler = illegalConfigurationChangeHandler;
    
    this.beanFactory = new TerracottaDomainConfigurationDocumentBeanFactory();
    this.defaultValueProvider = new FromSchemaDefaultValueProvider();
    this.xmlObjectComparator = new StandardXmlObjectComparator();
  }

}
