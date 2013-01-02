/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  @Override
  public IllegalConfigurationChangeHandler illegalConfigurationChangeHandler() {
    return this.illegalConfigurationChangeHandler;
  }

  @Override
  public void ensureRepositoryProvides(Class theClass) {
    beanRepository.ensureBeanIsOfClass(theClass);
  }

  @Override
  public boolean hasDefaultFor(String xpath) throws XmlException {
    return this.defaultValueProvider.possibleForXPathToHaveDefault(xpath)
           && this.defaultValueProvider.hasDefault(this.beanRepository.rootBeanSchemaType(), xpath);
  }

  @Override
  public XmlObject defaultFor(String xpath) throws XmlException {
    return this.defaultValueProvider.defaultFor(this.beanRepository.rootBeanSchemaType(), xpath);
  }

  @Override
  public boolean isOptional(String xpath) throws XmlException {
    return this.defaultValueProvider.isOptional(this.beanRepository.rootBeanSchemaType(), xpath);
  }

  @Override
  public XmlObject bean() {
    return this.beanRepository.bean();
  }

  @Override
  public Object syncLockForBean() {
    return this.beanRepository;
  }

  @Override
  public void itemCreated(ConfigItem item) {
    if (item instanceof ConfigurationChangeListener) this.beanRepository
        .addListener((ConfigurationChangeListener) item);
  }

  @Override
  public String toString() {
    return "<ConfigContext around repository: " + this.beanRepository + ">";
  }

}
