/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.Plugins;

/**
 * The standard implementation of {@link NewCommonL1Config}.
 */
public class NewCommonL1ConfigObject extends BaseNewConfigObject implements NewCommonL1Config {

  private final FileConfigItem logsPath;
  private final Plugins        plugins;

  public NewCommonL1ConfigObject(ConfigContext context) {
    super(context);
    Assert.assertNotNull(context);

    this.context.ensureRepositoryProvides(Client.class);

    logsPath = this.context.substitutedFileItem("logs");
    final Client client = (Client) this.context.bean();
    plugins = client != null && client.isSetPlugins() ? client.getPlugins() : null;
  }

  public FileConfigItem logsPath() {
    return this.logsPath;
  }

  public Plugins plugins() {
    return plugins;
  }

}
