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
package org.terracotta.tripwire;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

public class DumpAppender extends AppenderBase<ILoggingEvent> {
  
  private final Recording continuous;
  private String path;
  private int maxAgeMinutes = 5;

  public DumpAppender() {
    try {
      continuous = new Recording(Configuration.getConfiguration("default"));
      continuous.setToDisk(true);
    } catch (IOException | ParseException boot) {
      throw new RuntimeException(boot);
    }
  }

  @Override
  public void stop() {
    continuous.stop();
    super.stop();
  }

  @Override
  public void start() {
    if (maxAgeMinutes > 0) {
      continuous.setMaxAge(Duration.ofMinutes(maxAgeMinutes));
    } else {
      continuous.setMaxAge(null);
    }
    continuous.start();
    super.start();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public int getMaxAgeMinutes() {
    return maxAgeMinutes;
  }

  public void setMaxAgeMinutes(int maxAgeMinutes) {
    if (maxAgeMinutes > 0) {
      this.maxAgeMinutes = maxAgeMinutes;
    } else {
      this.maxAgeMinutes = -1;
    }
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    String timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    if (path == null) {
      path = System.getProperty("user.dir");
    }
    Path dest = Paths.get(path);
    if (!dest.toFile().exists()) {
      dest.toFile().mkdirs();
    }
    try {
      continuous.dump(dest.resolve(timestamp + ".jfr"));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}
