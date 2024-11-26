/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TCServerMain {
  
  public static void main(String[] args) {
    while (startServer(args)) {
        // signaling to shell that restart is requested with special exit code 11
        System.exit(11);
    };
    System.exit(0);
  }

  private static boolean startServer(String[] args) {
    Future<Boolean> s = createServer(Arrays.asList(args), null);
    if (s == null) {
      return false;
    } else {
      try {
        return s.get();
      } catch (ExecutionException | InterruptedException e) {
        return false;
      }
    }
  }

  public static Future<Boolean> createServer(List<String> args) {
    return createServer(args, System.out);
  }

  public static Future<Boolean> createServer(List<String> args, OutputStream console) {
    try {
      Path serverJar = Directories.getServerJar();

      if (serverJar != null) {
        ClassLoader serverClassLoader = new URLClassLoader(new URL[] {serverJar.toUri().toURL()}, TCServerMain.class.getClassLoader());

        return ServerFactory.createServer(args, console, serverClassLoader);
      }
    } catch (RuntimeException t) {
      throw t;
    } catch (IOException io) {
      throw new UncheckedIOException(io);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException("server libraries not found");
  }
}
