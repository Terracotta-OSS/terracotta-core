/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.net.core.SecurityInfo;
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

  @Override
  public L1ConfigurationSetupManager getL1TVSConfigurationSetupManager() throws ConfigurationSetupException {
    return getL1TVSConfigurationSetupManager(new SecurityInfo());
  }
}
