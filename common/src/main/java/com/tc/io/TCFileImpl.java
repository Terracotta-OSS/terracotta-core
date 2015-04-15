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
package com.tc.io;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class TCFileImpl implements TCFile {
  private File pathToFile;
  
  public TCFileImpl(File pathToFile) {
    this.pathToFile = pathToFile;
  }
  
  public TCFileImpl(TCFile location, String fileName) {
    pathToFile = new File(location.getFile(), fileName);
  }

  @Override
  public boolean exists() {
    return pathToFile.exists();
  }

  @Override
  public void forceMkdir() throws IOException {
    FileUtils.forceMkdir(pathToFile);
  }

  @Override
  public boolean createNewFile() throws IOException {
    return pathToFile.createNewFile();
  }

  @Override
  public File getFile() {
    return pathToFile;
  }

  @Override
  public TCFile createNewTCFile(TCFile location, String fileName) {
    return new TCFileImpl(location, fileName);
  }

  @Override
  public String toString() {
    return pathToFile.toString();
  }
}
