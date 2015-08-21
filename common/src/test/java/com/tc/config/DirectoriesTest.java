package com.tc.config;

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