/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.handshakemanager;

import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;


/**
 * Testing method {@link ClientHandshakeManagerImpl#checkClientServerVersionCompatibility(java.lang.String)}.
 */
public class ClientHandshakeManagerTest {

  @Mock
  private Logger logger;

  @Mock
  private ClientHandshakeMessageFactory chmf;

  @Mock
  private ClientHandshakeCallback entities;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testVersionsIdentical() {
    checkClientServerVersionCompatibility("1.1.1.1", "1.1.1.1");
    checkLoggedDifference(0);
  }

  @Test
  public void testClientVersionIsNull() {
    checkThatDifferenceIsLogged(null, "1.1.1.1");
  }

  private void checkThatDifferenceIsLogged(String clientVersion, String serverVersion) {
    checkClientServerVersionCompatibility(clientVersion, serverVersion);
    checkLoggedDifference(1);
  }

  private void checkClientServerVersionCompatibility(String clientVersion, String serverVersion) {
    ClientHandshakeManagerImpl manager = new ClientHandshakeManagerImpl(logger, chmf,
            UUID.getUUID().toString(), "name", clientVersion, "revision", entities);
    manager.checkClientServerVersionCompatibility(serverVersion);
  }

  private void checkLoggedDifference(int times) {
    verify(logger, times(times))
            .info(anyString());
  }

  @Test
  public void testClientVersionIsInvalid() {
    checkThatDifferenceIsLogged("${version}", "1.1.1.1"
    );
  }

  @Test
  public void testMajorVersionsDifferent() {
    checkThatDifferenceIsLogged("2.1.1.1", "1.1.1.1"
    );
  }

  @Test
  public void testMinorVersionsDifferent() {
    checkThatDifferenceIsLogged("1.2.1.11", "1.1.1.1"
    );
  }

  @Test
  public void testMicroVersionsDifferent() {
    checkThatDifferenceIsLogged("1.1.2.1", "1.1.1.1");
  }

  @Test
  public void testPatchDifferent() {
    checkThatDifferenceIsLogged("1.1.1.2", "1.1.1.1");
  }

  @Test
  public void testBuildDifferent() {
    checkThatDifferenceIsLogged("1.1.1.1.10", "1.1.1.1.11");
  }

  @Test
  public void testSpecifierDifferent() {
    checkThatDifferenceIsLogged("1.1.1.1.10_fix1", "1.1.1.1.11");
  }

  @Test
  public void testSnapshotDifferent() {
    checkThatDifferenceIsLogged("1.1.1-SNAPSHOT", "1.1.1");
  }
}
