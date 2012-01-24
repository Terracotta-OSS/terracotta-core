/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlString;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.util.Assert;
import com.terracottatech.config.ConfigurationModel;
import com.terracottatech.config.System;
import com.terracottatech.config.TcConfigDocument.TcConfig;

/**
 * The standard implementation of {@link SystemConfig}.
 */
public class SystemConfigObject extends BaseConfigObject implements SystemConfig {

  private final com.tc.config.schema.ConfigurationModel configModel;

  public SystemConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(System.class);
    System system = (System) this.context.bean();
    com.terracottatech.config.ConfigurationModel.Enum model = system.getConfigurationModel();
    Assert.assertTrue("Unexpected configuration model: " + model, model.equals(ConfigurationModel.DEVELOPMENT)
                                                                  || model.equals(ConfigurationModel.PRODUCTION));
    if (model.equals(ConfigurationModel.PRODUCTION)) {
      this.configModel = com.tc.config.schema.ConfigurationModel.PRODUCTION;
    } else {
      this.configModel = com.tc.config.schema.ConfigurationModel.DEVELOPMENT;
    }
  }

  public com.tc.config.schema.ConfigurationModel configurationModel() {
    return this.configModel;
  }

  public static void initializeSystem(TcConfig config, DefaultValueProvider defaultValueProvider) throws XmlException {
    System system;
    if (!config.isSetSystem()) {
      system = config.addNewSystem();
    } else {
      system = config.getSystem();
    }
    initializeConfigurationModel(system, defaultValueProvider);
  }

  private static void initializeConfigurationModel(System system, DefaultValueProvider defaultValueProvider) throws XmlException {
    Assert.assertNotNull(system);
    if (!system.isSetConfigurationModel()) {
      system.setConfigurationModel(getDefaultSystemConfigurationModel(system, defaultValueProvider));
    }
  }
  
  private static ConfigurationModel.Enum getDefaultSystemConfigurationModel(System system, DefaultValueProvider defaultValueProvider) throws XmlException {
    XmlString defaultValue = (XmlString) defaultValueProvider.defaultFor(system.schemaType(),
                                                                              "configuration-model");
    Assert.assertNotNull(defaultValue);
    Assert.assertTrue(defaultValue.getStringValue().equals(ConfigurationModel.DEVELOPMENT.toString())
                      || defaultValue.getStringValue().equals(ConfigurationModel.PRODUCTION.toString()));

    if (defaultValue.getStringValue().equals(ConfigurationModel.PRODUCTION.toString())) return ConfigurationModel.PRODUCTION;
    return ConfigurationModel.DEVELOPMENT;
  }

}
