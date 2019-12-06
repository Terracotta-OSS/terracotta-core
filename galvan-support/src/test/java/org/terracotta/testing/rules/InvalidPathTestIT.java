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
package org.terracotta.testing.rules;

import org.junit.Test;
import org.terracotta.testing.rules.BasicExternalCluster;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class InvalidPathTestIT {
  // We expect IllegalArgumentException on manualStart().
  @Test(expected=IllegalArgumentException.class)
  public void testInvalidJarPath() throws Throwable {
    File clusterDirectory = new File("target/cluster");
    int stripeSize = 1;
    List<File> serverJars = Arrays.asList(new File[]{new File("INVALID_PATH"), new File("/bogus/OTHER_INVALID_PATH")});
    BasicExternalCluster testCluster = BasicExternalClusterBuilder.newCluster().withServerJars(serverJars).build();
    // Exception here.
    testCluster.manualStart("InvalidPathTestIT");
    testCluster.manualStop();
  }

  // We expect IllegalArgumentException in BasicExternalCluster().
  @Test(expected=IllegalArgumentException.class)
  public void testInvalidClusterDirectory() throws Throwable {
    File clusterDirectory = new File("/bogus\0/path");
    int stripeSize = 1;
    List<File> serverJars = Collections.emptyList();
    String namespaceFragment = "";
    String serviceFragment = "";
    String entityFragment = "";
    // Exception here.
    BasicExternalClusterBuilder.newCluster().in(clusterDirectory).build();
  }
}
