/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.context;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.listen.ConfigurationChangeListener;
import com.tc.config.schema.repository.BeanRepository;
import com.tc.util.Assert;

/**
 * Binds together a {@link BeanRepository} and a {@link DefaultValueProvider}.
 */
public class StandardConfigContext implements ConfigContext {

  private final BeanRepository                    beanRepository;
  private final DefaultValueProvider              defaultValueProvider;
  private final IllegalConfigurationChangeHandler illegalConfigurationChangeHandler;

  public StandardConfigContext(BeanRepository beanRepository, DefaultValueProvider defaultValueProvider,
                               IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    Assert.assertNotNull(beanRepository);
    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(illegalConfigurationChangeHandler);

    this.beanRepository = beanRepository;
    this.defaultValueProvider = defaultValueProvider;
    this.illegalConfigurationChangeHandler = illegalConfigurationChangeHandler;
  }

  public IllegalConfigurationChangeHandler illegalConfigurationChangeHandler() {
    return this.illegalConfigurationChangeHandler;
  }

  public void ensureRepositoryProvides(Class theClass) {
    beanRepository.ensureBeanIsOfClass(theClass);
  }

  public boolean hasDefaultFor(String xpath) throws XmlException {
    return this.defaultValueProvider.possibleForXPathToHaveDefault(xpath)
           && this.defaultValueProvider.hasDefault(this.beanRepository.rootBeanSchemaType(), xpath);
  }

  public XmlObject defaultFor(String xpath) throws XmlException {
    return this.defaultValueProvider.defaultFor(this.beanRepository.rootBeanSchemaType(), xpath);
  }

  public boolean isOptional(String xpath) throws XmlException {
    return this.defaultValueProvider.isOptional(this.beanRepository.rootBeanSchemaType(), xpath);
  }

  public XmlObject bean() {
    return this.beanRepository.bean();
  }

  public Object syncLockForBean() {
    return this.beanRepository;
  }

  public void itemCreated(ConfigItem item) {
    if (item instanceof ConfigurationChangeListener) this.beanRepository
        .addListener((ConfigurationChangeListener) item);
  }

  public String toString() {
    return "<ConfigContext around repository: " + this.beanRepository + ">";
  }

}
