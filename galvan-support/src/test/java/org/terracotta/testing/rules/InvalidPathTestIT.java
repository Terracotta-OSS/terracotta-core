/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    String namespaceFragment = "";
    String serviceFragment = "";
    String entityFragment = "";
    BasicExternalCluster testCluster = new BasicExternalCluster(clusterDirectory, stripeSize, serverJars, namespaceFragment, serviceFragment, entityFragment);
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
    new BasicExternalCluster(clusterDirectory, stripeSize, serverJars, namespaceFragment, serviceFragment, entityFragment);
  }
}
