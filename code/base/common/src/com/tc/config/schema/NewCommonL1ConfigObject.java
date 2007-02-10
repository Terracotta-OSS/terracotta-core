/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.util.Assert;
import com.terracottatech.config.Client;

/**
 * The standard implementation of {@link NewCommonL1Config}.
 */
public class NewCommonL1ConfigObject extends BaseNewConfigObject implements NewCommonL1Config {

  private final FileConfigItem logsPath;

  public NewCommonL1ConfigObject(ConfigContext context) {
    super(context);
    Assert.assertNotNull(context);

    this.context.ensureRepositoryProvides(Client.class);

    this.logsPath = this.context.substitutedFileItem("logs");
  }

  public FileConfigItem logsPath() {
    return this.logsPath;
  }

}
