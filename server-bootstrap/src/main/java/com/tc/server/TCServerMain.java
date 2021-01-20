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
package com.tc.server;


import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.terracotta.server.Server;

public class TCServerMain {
  
  public static void main(String[] args) {
    boolean inlineRestart = Boolean.getBoolean("restart.inline");

    while (startServer(args)) {
      if (inlineRestart) {
        System.out.println("Restarting server...");
      } else {
        System.exit(11);
      }
    };
  }

  public static boolean startServer(String[] args) {
    try {
      Optional<Path> p = Files.list(Directories.getServerLibFolder().toPath()).filter(f->f.getFileName().toString().startsWith("dso-l2")).findFirst();

      ClassLoader serverClassLoader = new URLClassLoader(new URL[] {p.get().toUri().toURL()}, TCServerMain.class.getClassLoader());

      Server server = ServerFactory.createServer(args, serverClassLoader);

      return server.waitUntilShutdown();
    } catch (RuntimeException t) {
      throw t;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static Server createServer(String name, List<String> args) {
    try {
      Optional<Path> p = Files.list(Directories.getServerLibFolder().toPath()).filter(f->f.getFileName().toString().startsWith("dso-l2")).findFirst();

      ClassLoader serverClassLoader = new URLClassLoader(new URL[] {p.get().toUri().toURL()}, TCServerMain.class.getClassLoader());

      return ServerFactory.createServer(name, args, serverClassLoader);
    } catch (RuntimeException t) {
      throw t;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}