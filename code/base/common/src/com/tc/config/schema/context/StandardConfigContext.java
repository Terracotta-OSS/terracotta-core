/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.context;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.BooleanXPathBasedConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.FileXPathBasedConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.IntXPathBasedConfigItem;
import com.tc.config.schema.dynamic.StringArrayConfigItem;
import com.tc.config.schema.dynamic.StringArrayXPathBasedConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.tc.config.schema.dynamic.StringXPathBasedConfigItem;
import com.tc.config.schema.dynamic.SubstitutedFileXPathBasedConfigItem;
import com.tc.config.schema.listen.ConfigurationChangeListener;
import com.tc.config.schema.repository.BeanRepository;
import com.tc.util.Assert;

import java.io.File;

/**
 * Binds together a {@link BeanRepository} and a {@link DefaultValueProvider}.
 */
public class StandardConfigContext implements ConfigContext {

  private final BeanRepository                    beanRepository;
  private final DefaultValueProvider              defaultValueProvider;
  private final IllegalConfigurationChangeHandler illegalConfigurationChangeHandler;
  private final File                              configDirectory;

  public StandardConfigContext(BeanRepository beanRepository, DefaultValueProvider defaultValueProvider,
                               IllegalConfigurationChangeHandler illegalConfigurationChangeHandler,
                               File configDirectory) {
    Assert.assertNotNull(beanRepository);
    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(illegalConfigurationChangeHandler);

    this.beanRepository = beanRepository;
    this.defaultValueProvider = defaultValueProvider;
    this.illegalConfigurationChangeHandler = illegalConfigurationChangeHandler;
    this.configDirectory = configDirectory;
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

  public IntConfigItem intItem(String xpath) {
    return new IntXPathBasedConfigItem(this, xpath);
  }

  public StringConfigItem stringItem(String xpath) {
    return new StringXPathBasedConfigItem(this, xpath);
  }

  public StringArrayConfigItem stringArrayItem(String xpath) {
    return new StringArrayXPathBasedConfigItem(this, xpath);
  }

  public FileConfigItem fileItem(String xpath) {
    return new FileXPathBasedConfigItem(this, xpath);
  }

  public FileConfigItem substitutedFileItem(String xpath) {
    return new SubstitutedFileXPathBasedConfigItem(this, xpath);
  }

  public BooleanConfigItem booleanItem(String xpath) {
    return new BooleanXPathBasedConfigItem(this, xpath);
  }

  public BooleanConfigItem booleanItem(String xpath, boolean defaultValue) {
    return new BooleanXPathBasedConfigItem(this, xpath, defaultValue);
  }
  
  public FileConfigItem configRelativeSubstitutedFileItem(String xpath) {
    return new SubstitutedFileXPathBasedConfigItem(this, xpath, configDirectory);
  }

  public String toString() {
    return "<ConfigContext around repository: " + this.beanRepository + ">";
  }

}
