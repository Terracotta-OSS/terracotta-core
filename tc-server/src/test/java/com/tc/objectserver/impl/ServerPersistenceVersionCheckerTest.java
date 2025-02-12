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
package com.tc.objectserver.impl;

import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.productinfo.ProductInfo;
import com.tc.test.TCTestCase;
import com.tc.util.version.Version;
import org.junit.Ignore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({ ProductInfo.class })
@Ignore("powermock no longer available")
public class ServerPersistenceVersionCheckerTest extends TCTestCase {
  private ClusterStatePersistor           clusterStatePersistor;
  private ServerPersistenceVersionChecker serverPersistenceVersionChecker;
  private ProductInfo                     productInfo;

  @Override
  public void setUp() throws Exception {
    clusterStatePersistor = mock(ClusterStatePersistor.class);
    productInfo = mock(ProductInfo.class);
    serverPersistenceVersionChecker = new ServerPersistenceVersionChecker(productInfo);
  }

  public void testDotVersionBump() {
    persistedVersion("1.0.0");
    currentVersion("1.0.1");
    verifyUpdatedTo("1.0.1");
  }

  public void testDotBumpToSnapshot() {
    persistedVersion("1.0.0");
    currentVersion("1.0.1-SNAPSHOT");
    verifyUpdatedTo("1.0.1-SNAPSHOT");
  }

  public void testDotVersionDrop() {
    currentVersion("1.0.0");
    persistedVersion("1.0.1");
    verifyNoUpdate();
  }

  public void testMinorVersionBump() {
    currentVersion("1.1.0");
    persistedVersion("1.0.0");
    verifyUpdatedTo("1.1.0");
  }

  public void testMinorVersionDrop() {
    currentVersion("1.0.0");
    persistedVersion("1.1.0");
    verifyNoUpdate();
  }

  public void testMajorVersionDrop() {
    currentVersion("1.0.0");
    persistedVersion("2.0.0");
    verifyNoUpdate();
  }

  public void testMajorVersionBump() {
    persistedVersion("1.0.0");
    currentVersion("2.0.0");
    verifyUpdatedTo("2.0.0");
  }

  public void testInitializeVersion() {
    persistedVersion(null);
    currentVersion("1.0.0");
    verifyUpdatedTo("1.0.0");
  }

  public void testUpdateDotVersion() {
    currentVersion("1.0.1");
    persistedVersion("1.0.0");
    verifyUpdatedTo("1.0.1");
  }

  public void testDoNotOverwriteNewerVersion() {
    currentVersion("1.0.0");
    persistedVersion("1.0.1");
    verifyNoUpdate();
  }

  private void currentVersion(String v) {
    when(productInfo.version()).thenReturn(v);
  }

  private void persistedVersion(String v) {
    when(clusterStatePersistor.getVersion()).thenReturn(v == null ? null : new Version(v));
  }

  private void verifyUpdatedTo(String v) {
    serverPersistenceVersionChecker.checkAndBumpPersistedVersion(clusterStatePersistor);
    verify(clusterStatePersistor).setVersion(new Version(v));
  }

  private void verifyNoUpdate() {
    serverPersistenceVersionChecker.checkAndBumpPersistedVersion(clusterStatePersistor);
    verify(clusterStatePersistor, never()).setVersion(any(Version.class));
  }
}
