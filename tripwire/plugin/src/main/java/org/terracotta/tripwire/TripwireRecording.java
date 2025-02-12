/*
 * Copyright IBM Corp. 2024, 2025
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;



public class TripwireRecording {
  private final Recording flightRecording;
  
  TripwireRecording(String configuration, Path sendTo, int maxAge, long maxSize) {
    try {
      Configuration c = Configuration.create(new InputStreamReader(getClass().getResourceAsStream("/" + configuration + ".jfc")));
      flightRecording = new Recording(c);
      if (maxAge > 0) {
        flightRecording.setMaxAge(Duration.ofMinutes(maxAge));
      }
      if (maxSize > 0) {
        flightRecording.setMaxSize(maxSize);
      }
      flightRecording.setToDisk(true);
      if (sendTo != null) {
        flightRecording.setDestination(sendTo);
        flightRecording.setDumpOnExit(true);
      }
      flightRecording.start();
    } catch (IOException | ParseException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
  public String dump(Path save) {
    try {
      flightRecording.dump(save);
    } catch (IOException ioe) {
      return ioe.toString();
    }
    return save.toAbsolutePath().toString();
  }
  
  public String dumpSegment(Path destination, Duration segment) {
    Instant now = Instant.now();
    if (segment == null) segment = flightRecording.getMaxAge();
    Instant i = now.minus(segment);
    try (Recording r = flightRecording.copy(true)) {
      try (InputStream is = r.getStream(i, now)) {
        writeToPath(is, destination);
      }
    } catch (IOException ioe) {
      return ioe.toString();
    }
    return destination.toAbsolutePath().toString();
  }
  
  private void writeToPath(InputStream is, Path dest) throws IOException {
    byte[] buffer = new byte[1024];
    try (OutputStream fos = Files.newOutputStream(dest)) {
      int amt = is.read(buffer);
      while (amt >= 0) {
        fos.write(buffer, 0, amt);
        amt = is.read(buffer);
      }
    }
  }  
}
