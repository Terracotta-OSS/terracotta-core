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

package com.tc.objectserver.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.test.TCExtension;
import com.tc.util.ProductInfo;
import com.tc.util.version.Version;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
@ExtendWith(TCExtension.class)
@PrepareForTest({ ProductInfo.class })
public class ServerPersistenceVersionCheckerTest {
  private ClusterStatePersistor           clusterStatePersistor;
  private ServerPersistenceVersionChecker serverPersistenceVersionChecker;
  private ProductInfo                     productInfo;

  @BeforeEach
  public void setUp() throws Exception {
    clusterStatePersistor = mock(ClusterStatePersistor.class);
    productInfo = mock(ProductInfo.class);
    serverPersistenceVersionChecker = new ServerPersistenceVersionChecker(productInfo);
  }

  @Test
  public void testDotVersionBump() {
    persistedVersion("1.0.0");
    currentVersion("1.0.1");
    verifyUpdatedTo("1.0.1");
  }

  @Test
  public void testDotBumpToSnapshot() {
    persistedVersion("1.0.0");
    currentVersion("1.0.1-SNAPSHOT");
    verifyUpdatedTo("1.0.1-SNAPSHOT");
  }

  @Test
  public void testDotVersionDrop() {
    currentVersion("1.0.0");
    persistedVersion("1.0.1");
    verifyNoUpdate();
  }

  @Test
  public void testMinorVersionBump() {
    currentVersion("1.1.0");
    persistedVersion("1.0.0");
    verifyUpdatedTo("1.1.0");
  }

  @Test
  public void testMinorVersionDrop() {
    currentVersion("1.0.0");
    persistedVersion("1.1.0");
    verifyNoUpdate();
  }

  @Test
  public void testMajorVersionDrop() {
    currentVersion("1.0.0");
    persistedVersion("2.0.0");
    verifyNoUpdate();
  }

  @Test
  public void testMajorVersionBump() {
    persistedVersion("1.0.0");
    currentVersion("2.0.0");
    verifyUpdatedTo("2.0.0");
  }

  @Test
  public void testInitializeVersion() {
    persistedVersion(null);
    currentVersion("1.0.0");
    verifyUpdatedTo("1.0.0");
  }

  @Test
  public void testUpdateDotVersion() {
    currentVersion("1.0.1");
    persistedVersion("1.0.0");
    verifyUpdatedTo("1.0.1");
  }

  @Test
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
