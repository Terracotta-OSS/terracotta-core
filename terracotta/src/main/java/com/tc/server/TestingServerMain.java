/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class TestingServerMain extends TCServerMain {
  
  public static void main(String[] args) {
    deadmansSwitch();
    TCServerMain.main(args);
  }
    
  
  private static void deadmansSwitch() {
    Thread t;
    t = new Thread(()->{
      while (true) {
        try {
          int code = readNextCode(120, TimeUnit.SECONDS);
          switch (code) {
            case 's', 'z' -> {
              break;
            }
            default -> {
            }
          }
        } catch (IOException | InterruptedException io) {
  
        } catch (TimeoutException e) {
          System.exit(2);
        }
      }
    },"Sentinel");
    t.setDaemon(true);
    t.start();
  }
  
  private static int readNextCode(long set, TimeUnit units) throws TimeoutException, IOException, InterruptedException {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < units.toMillis(set)) {
      if (System.in.available() == 0) {
        TimeUnit.SECONDS.sleep(1);
      } else {
        return System.in.read();
      }
    }
    throw new TimeoutException();
  }
}
