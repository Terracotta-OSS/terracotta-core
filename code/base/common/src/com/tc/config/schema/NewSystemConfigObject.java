/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Assert;
import com.terracottatech.configV2.ConfigurationModel;
import com.terracottatech.configV2.System;

/**
 * The standard implementation of {@link NewSystemConfig}.
 */
public class NewSystemConfigObject extends BaseNewConfigObject implements NewSystemConfig {

  private final ConfigItem        configurationModel;

  public NewSystemConfigObject(ConfigContext context) throws ConfigurationSetupException {
    super(context);

    this.context.ensureRepositoryProvides(System.class);

    this.configurationModel = new XPathBasedConfigItem(this.context, "configuration-model") {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        if (xmlObject == null) return null;
        if (((ConfigurationModel) xmlObject).enumValue().equals(ConfigurationModel.DEVELOPMENT)) return com.tc.config.schema.ConfigurationModel.DEVELOPMENT;
        if (((ConfigurationModel) xmlObject).enumValue().equals(ConfigurationModel.PRODUCTION)) return com.tc.config.schema.ConfigurationModel.PRODUCTION;
        throw Assert.failure("Unexpected configuration model: " + xmlObject);
      }
    };
  }

  public ConfigItem configurationModel() {
    return this.configurationModel;
  }

}
