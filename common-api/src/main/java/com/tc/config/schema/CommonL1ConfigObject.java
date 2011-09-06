/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.Modules;

import java.io.File;

/**
 * The standard implementation of {@link CommonL1Config}.
 */
public class CommonL1ConfigObject extends BaseConfigObject implements CommonL1Config {

  private final Modules modules;

  public CommonL1ConfigObject(ConfigContext context) {
    super(context);
    Assert.assertNotNull(context);

    this.context.ensureRepositoryProvides(Client.class);

    final Client client = (Client) this.context.bean();

    modules = client.isSetModules() ? client.getModules() : null;

    if (modules != null) {
      for (int i = 0; i < modules.sizeOfRepositoryArray(); i++) {
        String location = modules.getRepositoryArray(i);
        modules.setRepositoryArray(i, ParameterSubstituter.substitute(location));
      }
    }
  }

  public File logsPath() {
    final Client client = (Client) this.context.bean();
    return new File(client.getLogs());
  }

  public Modules modules() {
    return modules;
  }

}
