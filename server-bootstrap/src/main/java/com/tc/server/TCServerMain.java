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


import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.terracotta.server.Server;

public class TCServerMain {
  
  public static void main(String[] args) {
    boolean inlineRestart = Boolean.getBoolean(ServerFactory.RESTART_INLINE);
    while (startServer(args, inlineRestart)) {
      if (inlineRestart) {
        System.out.println("Restarting server...");
      } else {
        // signaling to shell that restart is requested with special exit code 11
        System.exit(11);
      }
    };
    System.exit(0);
  }

  private static boolean startServer(String[] args, boolean requestStop) {
    Server s = createServer(Arrays.asList(args), requestStop ? System.out : null);
    if (s == null) {
      return false;
    } else {
      return s.waitUntilShutdown();
    }
  }

  public static Server createServer(List<String> args) {
    return createServer(args, Boolean.getBoolean(ServerFactory.RESTART_INLINE) ? System.out : null);
  }

  public static Server createServer(List<String> args, OutputStream console) {
    try {
      if (Files.isDirectory(Directories.getServerLibFolder())) {
        Optional<Path> p = Files.list(Directories.getServerLibFolder()).filter(f->f.getFileName().toString().startsWith("dso-l2")).findFirst();

        ClassLoader serverClassLoader = new URLClassLoader(new URL[] {p.get().toUri().toURL()}, TCServerMain.class.getClassLoader());

        return console != null ? ServerFactory.createServer(args, console, serverClassLoader) : ServerFactory.createServer(args, serverClassLoader);
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
