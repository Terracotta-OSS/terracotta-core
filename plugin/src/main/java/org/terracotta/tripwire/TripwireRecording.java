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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;



public class TripwireRecording {
  private final Recording flightRecording;

  TripwireRecording(String configuration) {
    try {
      Configuration c = Configuration.create(new InputStreamReader(getClass().getResourceAsStream("/" + configuration + ".jfc")));
      flightRecording = new Recording(c);
      flightRecording.setMaxAge(Duration.ofMinutes(5));
      flightRecording.setToDisk(true);
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
    try (FileOutputStream fos = new FileOutputStream(dest.toFile())) {
      int amt = is.read(buffer);
      while (amt >= 0) {
        fos.write(buffer, 0, amt);
        amt = is.read(buffer);
      }
    }
  }  
}
