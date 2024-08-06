/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@Ignore("Temporary ignore due to current way tests are run in Jenkins")
public class DirectoriesTest {

  private File testFolder;

  @Before
  public void setUp() throws Exception {
    testFolder = File.createTempFile("test", "tmp").getParentFile();
    System.setProperty(Directories.TC_INSTALL_ROOT_PROPERTY_NAME, testFolder.getAbsolutePath());
  }

  @After
  public void tearDown() {
    System.clearProperty(Directories.TC_INSTALL_ROOT_PROPERTY_NAME);
  }

  @Test
  public void testInstallDirResolution() throws FileNotFoundException {
    assertThat(Directories.getInstallationRoot(), equalTo(testFolder));
  }

  @Test
  public void testServerLibDir() throws FileNotFoundException {
    assertThat(Directories.getServerLibFolder(), equalTo(new File(testFolder, "server/lib")));
  }

}