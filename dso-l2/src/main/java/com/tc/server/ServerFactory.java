package com.tc.server;

import com.tc.config.HaConfigImpl;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.license.LicenseManager;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;

public class ServerFactory {
  public static TCServer createServer(L2ConfigurationSetupManager configurationSetupManager, TCThreadGroup threadGroup) {
    verifyLicensePresent();

    int maxClientCount = LicenseManager.maxClientCount();
    // only coordinator checks license
    boolean isCoordinatorGroup = new HaConfigImpl(configurationSetupManager).isActiveCoordinatorGroup();
    ConnectionPolicy policy = isCoordinatorGroup ? new ConnectionPolicyImpl(maxClientCount)
        : new NullConnectionPolicy();
    return new TCServerImpl(configurationSetupManager, threadGroup, policy);
  }

  private static void verifyLicensePresent() {
    // This call is to make sure there is indeed a product license available. The EE server should not start w/o a
    // license no matter what server configuration is present in tc-config.xml
    LicenseManager.assertLicenseValid();
  }
}
