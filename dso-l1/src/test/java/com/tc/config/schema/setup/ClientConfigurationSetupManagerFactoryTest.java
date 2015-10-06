package com.tc.config.schema.setup;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.tc.net.core.SecurityInfo;
import com.tc.security.PwProvider;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * @author vmad
 */
public class ClientConfigurationSetupManagerFactoryTest {

  public static final String HOST = "localhost";
  public static final int PORT = 1234;
  public static final String USER = "TestUser";

  @Test
  public void testGetL1TVSConfigurationSetupManager() throws Exception {
    ClientConfigurationSetupManagerFactory clientConfigurationSetupManagerFactory = new ClientConfigurationSetupManagerFactory(new String[0], Collections.singletonList(HOST + ":" + PORT), mock(PwProvider.class));
    L1ConfigurationSetupManager l1ConfigurationSetupManager = clientConfigurationSetupManagerFactory.getL1TVSConfigurationSetupManager(new SecurityInfo(true, USER));

    Assert.assertNotNull(l1ConfigurationSetupManager);
    Assert.assertEquals(HOST, l1ConfigurationSetupManager.l2Config().l2Data()[0].host());
    Assert.assertEquals(PORT, l1ConfigurationSetupManager.l2Config().l2Data()[0].tsaPort());
    Assert.assertEquals(USER, l1ConfigurationSetupManager.getSecurityInfo().getUsername());
    Assert.assertEquals(true, l1ConfigurationSetupManager.getSecurityInfo().isSecure());
  }

  @Test
  public void testGetL1TVSConfigurationSetupManagerMultiUrl() throws Exception {
    List<String> stripeMemberUris = new Vector<String>();
    stripeMemberUris.add(HOST + ":" + PORT);
    stripeMemberUris.add(HOST + ":" + PORT);
    ClientConfigurationSetupManagerFactory clientConfigurationSetupManagerFactory = new ClientConfigurationSetupManagerFactory(new String[0], stripeMemberUris, mock(PwProvider.class));
    L1ConfigurationSetupManager l1ConfigurationSetupManager = clientConfigurationSetupManagerFactory.getL1TVSConfigurationSetupManager(new SecurityInfo(true, USER));

    Assert.assertNotNull(l1ConfigurationSetupManager);
    Assert.assertEquals(HOST, l1ConfigurationSetupManager.l2Config().l2Data()[0].host());
    Assert.assertEquals(PORT, l1ConfigurationSetupManager.l2Config().l2Data()[0].tsaPort());
    Assert.assertEquals(USER, l1ConfigurationSetupManager.getSecurityInfo().getUsername());
    Assert.assertEquals(true, l1ConfigurationSetupManager.getSecurityInfo().isSecure());

    Assert.assertEquals(HOST, l1ConfigurationSetupManager.l2Config().l2Data()[1].host());
    Assert.assertEquals(PORT, l1ConfigurationSetupManager.l2Config().l2Data()[1].tsaPort());
    Assert.assertEquals(USER, l1ConfigurationSetupManager.getSecurityInfo().getUsername());
    Assert.assertEquals(true, l1ConfigurationSetupManager.getSecurityInfo().isSecure());
  }

  @Test (expected = ConfigurationSetupException.class)
  public void testGetL1TVSConfigurationSetupManagerFailure() throws Exception {
    ClientConfigurationSetupManagerFactory clientConfigurationSetupManagerFactory = new ClientConfigurationSetupManagerFactory(new String[0], Collections.singletonList(HOST + PORT), mock(PwProvider.class));
    clientConfigurationSetupManagerFactory.getL1TVSConfigurationSetupManager(new SecurityInfo(true, USER));
  }
}