/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
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

  @Override
  public File directoryLoadedFrom() {
    return createFile().getParentFile();
  }

  private File createFile() {
    File file = new File(this.path);
    if (!file.isAbsolute()) file = new File(this.defaultDirectory, this.path);
    return file;
  }

  @Override
  public boolean isTrusted() {
    return false;
  }
  
  @Override
  public String toString() {
    return "file at '" + createFile().getAbsolutePath() + "'";
  }

}
