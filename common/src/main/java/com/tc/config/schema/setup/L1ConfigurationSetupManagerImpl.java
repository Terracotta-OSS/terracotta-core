/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.CommonL1ConfigObject;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.config.schema.L2ConfigForL1Object;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.object.config.schema.L1Config;
import com.tc.util.Assert;

import java.io.File;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.L1ConfigurationSetupManager}.
 */
public class L1ConfigurationSetupManagerImpl extends BaseConfigurationSetupManager implements
    L1ConfigurationSetupManager {
  private final CommonL1Config     commonL1Config;
  private final SecurityInfo       securityInfo;
  private final boolean            loadedFromTrustedSource;

  public L1ConfigurationSetupManagerImpl(ConfigurationCreator configurationCreator, SecurityInfo securityInfo)
      throws ConfigurationSetupException {
    super(configurationCreator);

    Assert.assertNotNull(configurationCreator);
    this.securityInfo = securityInfo;

    runConfigurationCreator();
    loadedFromTrustedSource = configurationCreator().loadedFromTrustedSource();

    commonL1Config = new CommonL1ConfigObject();
  }

  @Override
  public void setupLogging() {
    File logsPath = new File("client-logs");
    TCLogging.setLogDirectory(logsPath, TCLogging.PROCESS_TYPE_L1);
  }

  @Override
  public String rawConfigText() {
    return configurationCreator().rawConfigText();
  }
  
  @Override
  public String source() {
    return configurationCreator().source();
  }
  
  @Override
  public boolean loadedFromTrustedSource() {
    return this.loadedFromTrustedSource;
  }

  @Override
  public L2ConfigForL1 l2Config() {
    return new L2ConfigForL1Object(serversBeanRepository(), null);
  }

  @Override
  public CommonL1Config commonL1Config() {
    return this.commonL1Config;
  }

  @Override
  public L1Config dsoL1Config() {
    return new L1Config() {
      @Override
      public Object getBean() {
        return null;
      }
    };
  }

  @Override
  public void reloadServersConfiguration() throws ConfigurationSetupException {
    configurationCreator().reloadServersConfiguration(true, false);
  }

  @Override
  public SecurityInfo getSecurityInfo() {
    return this.securityInfo;
  }
}
