/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup.sources;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link ConfigurationSource} that reads from a file.
 */
public class FileConfigurationSource implements ConfigurationSource {

  private final String path;
  private final File   defaultDirectory;

  public FileConfigurationSource(String path, File defaultDirectory) {
    Assert.assertNotBlank(path);

    this.path = path;
    this.defaultDirectory = defaultDirectory;
  }

  public InputStream getInputStream(long maxTimeoutMillis) throws ConfigurationSetupException {
    File file = createFile();

    if (!file.exists()) throw new ConfigurationSetupException("The file '" + file.getAbsolutePath()
                                                              + "' does not exist");
    if (file.isDirectory()) throw new ConfigurationSetupException("The \"file\" '" + file.getAbsolutePath()
                                                                  + "' is actually a directory");

    try {
      FileInputStream out = new FileInputStream(file);
      return out;
    } catch (IOException ioe) {
      // We need this to be a ConfigurationSetupException so that we don't keep 'retrying' this file.
      throw new ConfigurationSetupException("We can't read data from the file '" + file.getAbsolutePath() + "': "
                                            + ioe.getLocalizedMessage());
    }
  }

  public File directoryLoadedFrom() {
    return createFile().getParentFile();
  }

  private File createFile() {
    File file = new File(this.path);
    if (!file.isAbsolute()) file = new File(this.defaultDirectory, this.path);
    return file;
  }

  public boolean isTrusted() {
    return false;
  }
  
  public String toString() {
    return "file at '" + createFile().getAbsolutePath() + "'";
  }

}
