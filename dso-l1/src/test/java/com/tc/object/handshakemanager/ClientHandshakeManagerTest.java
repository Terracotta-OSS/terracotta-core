/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.handshakemanager;

import com.tc.cluster.ClusterInternalEventsGun;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


/**
 * Testing method {@link ClientHandshakeManagerImpl#checkClientServerVersionCompatibility(java.lang.String)}
 * with different arguments and configurations.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(TCPropertiesImpl.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class ClientHandshakeManagerTest {

  @Mock
  private Logger logger;

  @Mock
  private ClientHandshakeMessageFactory chmf;

  @Mock
  private SessionManager sessionManager;

  @Mock
  private ClusterInternalEventsGun clusterEventsGun;

  @Mock
  private ClientHandshakeCallback entities;

  @Mock
  private TCProperties properties;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    when(properties.getBoolean(TCPropertiesConsts.VERSION_COMPATIBILITY_CHECK))
            .thenReturn(checkVersionCompatibility);

    PowerMockito.mockStatic(TCPropertiesImpl.class);
    PowerMockito.when(TCPropertiesImpl.getProperties())
            .thenReturn(properties);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Parameterized.Parameters
  public static Collection<Object[]> getCheckVersionCompatibilityPropertyValues() {
    return Arrays.asList(new Object[][]{
            {true},
            {false}
    });
  }

  private final boolean checkVersionCompatibility;

  public ClientHandshakeManagerTest(boolean checkVersionCompatibility) {
    this.checkVersionCompatibility = checkVersionCompatibility;
  }

  @Test
  public void testVersionsIdentical() {
    checkClientServerVersionCompatibility("1.1.1.1", "1.1.1.1");
    checkLoggedDifference(0);
  }

  @Test
  public void testClientVersionIsNull() {
    checkCompatibilityIfConfiguredAndDifferenceEitherWay(null, "1.1.1.1",
            NullPointerException.class);
  }

  private void checkCompatibilityIfConfiguredAndDifferenceEitherWay(String clientVersion,
                                                                    String serverVersion,
                                                                    Class<? extends Throwable> exceptionClass) {
    expectErrorIfCheckingCompatibility(exceptionClass);
    checkClientServerVersionCompatibility(clientVersion, serverVersion);
    checkLoggedDifferenceIfNotCheckingCompatibility();
  }

  private void checkClientServerVersionCompatibility(String clientVersion, String serverVersion) {
    ClientHandshakeManagerImpl manager = new ClientHandshakeManagerImpl(logger, chmf, sessionManager,
            clusterEventsGun, UUID.getUUID().toString(), "name", clientVersion, entities);
    manager.checkClientServerVersionCompatibility(serverVersion);
  }

  private void expectErrorIfCheckingCompatibility(Class<? extends Throwable> exceptionClass) {
    if (checkVersionCompatibility) {
      thrown.expect(exceptionClass);
    }
  }

  private void checkLoggedDifferenceIfNotCheckingCompatibility() {
    if (!checkVersionCompatibility) {
      checkLoggedDifference(1);
    }
  }

  private void checkLoggedDifference(int times) {
    verify(logger, times(times))
            .info(anyString());
  }

  @Test
  public void testClientVersionIsInvalid() {
    checkCompatibilityIfConfiguredAndDifferenceEitherWay("${version}", "1.1.1.1",
            IllegalArgumentException.class);
  }

  @Test
  public void testMajorVersionsDifferent() {
    checkCompatibilityIfConfiguredAndDifferenceEitherWay("2.1.1.1", "1.1.1.1",
            IllegalStateException.class);
  }

  @Test
  public void testMinorVersionsDifferent() {
    checkCompatibilityIfConfiguredAndDifferenceEitherWay("1.2.1.11", "1.1.1.1",
            IllegalStateException.class);
  }

  @Test
  public void testMicroVersionsDifferent() {
    checkDifferenceOnly("1.1.2.1", "1.1.1.1");
  }

  private void checkDifferenceOnly(String clientVersion, String serverVersion) {
    checkClientServerVersionCompatibility(clientVersion, serverVersion);
    checkLoggedDifference(1);
  }

  @Test
  public void testPatchDifferent() {
    checkDifferenceOnly("1.1.1.2", "1.1.1.1");
  }

  @Test
  public void testBuildDifferent() {
    checkDifferenceOnly("1.1.1.1.10", "1.1.1.1.11");
  }

  @Test
  public void testSpecifierDifferent() {
    checkDifferenceOnly("1.1.1.1.10_fix1", "1.1.1.1.11");
  }

  @Test
  public void testSnapshotDifferent() {
    checkDifferenceOnly("1.1.1-SNAPSHOT", "1.1.1");
  }
}
