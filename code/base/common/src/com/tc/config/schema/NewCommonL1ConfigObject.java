/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.Modules;

/**
 * The standard implementation of {@link NewCommonL1Config}.
 */
public class NewCommonL1ConfigObject extends BaseNewConfigObject implements NewCommonL1Config {

  private final FileConfigItem logsPath;
  private final FileConfigItem statisticsPath;
  private final Modules        modules;

  public NewCommonL1ConfigObject(ConfigContext context) {
    super(context);
    Assert.assertNotNull(context);

    this.context.ensureRepositoryProvides(Client.class);

    this.logsPath = this.context.substitutedFileItem("logs");
    this.statisticsPath = context.configRelativeSubstitutedFileItem("statistics");
    
    final Client client = (Client) this.context.bean();
    modules = client != null && client.isSetModules() ? client.getModules() : null;
    
    if(modules != null) {
      for(int i = 0; i < modules.sizeOfRepositoryArray(); i++) {
        String location = modules.getRepositoryArray(i);
        modules.setRepositoryArray(i, ParameterSubstituter.substitute(location));
      }
    }
  }

  public FileConfigItem logsPath() {
    return this.logsPath;
  }

  public FileConfigItem statisticsPath() {
    return this.statisticsPath;
  }

  public Modules modules() {
    return modules;
  }

}
