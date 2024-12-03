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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRAppender extends AppenderBase<ILoggingEvent> {
  
  private Recording continuous;
  private String path;
  private String configuration = "default";
  private String filePattern;
  private Path recordings;
  private boolean dumpOnExit = true;
  private int maxAgeMinutes = 5;
  private Pattern dumpRegex;
  private LocalDateTime lastsave = LocalDateTime.MIN;
  private static final Logger LOGGER = LoggerFactory.getLogger(JFRAppender.class);

  public JFRAppender() {
    super();

  }

  @Override
  public void stop() {
    if (continuous != null) {
      continuous.stop();
    }
    super.stop();
  }
  
  private Recording createRecording() {
    try {
      continuous = new Recording(Configuration.getConfiguration(configuration));
      continuous.setToDisk(true);
      if (maxAgeMinutes > 0) {
        continuous.setMaxAge(Duration.ofMinutes(maxAgeMinutes));
      } else {
        continuous.setMaxAge(null);
      }
      Path inflight = resolveFilePath();
      if (inflight != null) {
        continuous.setDestination(inflight);
        continuous.setDumpOnExit(true);
      } else {
        continuous.setDumpOnExit(false);
      }
    } catch (IOException | ParseException boot) {
      throw new RuntimeException(boot);
    }

    return continuous;
  }
  
  private Path resolveFilePath() {
    if (path == null) {
      path = System.getProperty("user.dir");
    }
    recordings = Paths.get(path);
    if (Files.notExists(recordings)) {
      try {
        Files.createDirectories(recordings);
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
    if (dumpOnExit) {
      Path inflight = recordings.resolve("inflight-" + getPID() + ".jfr");
      if (Files.exists(inflight)) {
        try {
          Files.delete(inflight);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
      return inflight;
    }
    return null;
  }

  @Override
  public void start() {
    if (EventAppender.isEnabled()) {
      createRecording().start();
    } else {
      LOGGER.info("JFRAppender disabled, Java Flight Recorder not found");
      continuous = null;
    }
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

  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  public void setMaxAgeMinutes(int maxAgeMinutes) {
    if (maxAgeMinutes > 0) {
      this.maxAgeMinutes = maxAgeMinutes;
    } else {
      this.maxAgeMinutes = -1;
    }
  }

  public String getFilePattern() {
    return filePattern;
  }

  public void setFilePattern(String filePattern) {
    this.filePattern = filePattern;
  }

  public boolean isDumpOnExit() {
    return dumpOnExit;
  }

  public void setDumpOnExit(boolean dumpOnExit) {
    this.dumpOnExit = dumpOnExit;
  }
  
  public void setRegex(String regex) {
    if (regex != null) {
      dumpRegex = Pattern.compile(regex);
    } else {
      dumpRegex = null;
    }
  }
  
  public String getRegex() {
    return dumpRegex != null ? dumpRegex.pattern() : null;
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (dumpRegex != null) {
      String checking = eventObject.getMessage();
      if (!dumpRegex.matcher(checking).find()) {
        return;
      }
    }
    LocalDateTime now = LocalDateTime.now();
    if (lastsave.plus(Duration.ofMinutes(1)).isAfter(now)) {
      //  only dump at a maximum of every minute
      return;
    } else {
      lastsave = now;
    }
    String timestamp = filePattern != null ? DateTimeFormatter.ofPattern(filePattern).format(now) : 
      DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(now).replace(':', '_');
    try {
      if (continuous != null) {
        new LogEvent(eventObject.getLoggerName(), eventObject.getLevel().toString(), eventObject.getFormattedMessage(), true).commit();
        continuous.dump(recordings.resolve(timestamp + ".jfr"));
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
  private int getPID() {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    int index = vmName.indexOf('@');

    if (index < 0) { throw new RuntimeException("unexpected format: " + vmName); }

    return Integer.parseInt(vmName.substring(0, index));
  }  
}
