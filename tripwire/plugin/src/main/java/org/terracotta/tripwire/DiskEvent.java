/*
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.tripwire;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@Category("Tripwire")
@Period("5 s")
@Label("Disk")
@StackTrace(false)
class DiskEvent extends Event implements org.terracotta.tripwire.Event {

  private String path;
  @DataAmount(DataAmount.BYTES)
  private final long free;

  DiskEvent(Path path) {
    File f = path.toFile();
    this.path = path.toString();
    try {
      this.free = Files.getFileStore(path).getUsableSpace();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public void setDescription(String description) {

  }
}
