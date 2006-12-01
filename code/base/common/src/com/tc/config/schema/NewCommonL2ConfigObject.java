/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.terracottatech.configV2.Server;

/**
 * The standard implementation of {@link NewCommonL2Config}.
 */
public class NewCommonL2ConfigObject extends BaseNewConfigObject implements NewCommonL2Config {

  private final FileConfigItem   dataPath;
  private final FileConfigItem   logsPath;
  private final IntConfigItem    jmxPort;
  private final StringConfigItem host;

  public NewCommonL2ConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(Server.class);

    this.dataPath = context.configRelativeSubstitutedFileItem("data");
    this.logsPath = context.configRelativeSubstitutedFileItem("logs");
    this.jmxPort = context.intItem("jmx-port");
    this.host = context.stringItem("@host");
  }

  public FileConfigItem dataPath() {
    return this.dataPath;
  }

  public FileConfigItem logsPath() {
    return this.logsPath;
  }

  public IntConfigItem jmxPort() {
    return this.jmxPort;
  }

  public StringConfigItem host() {
    return this.host;
  }

}
