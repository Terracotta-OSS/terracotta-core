/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.testing.master;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 */
public class ServerDeploymentBuilder {
  private Path installPath;
  private List<Plugin> plugins = new ArrayList<>();
  private boolean refresh = false;
  
  public ServerDeploymentBuilder() {
    
  }
  
  public ServerDeploymentBuilder installPath(Path install) {
    this.installPath = install;
    return this;
  }
  
  public ServerDeploymentBuilder addPlugin(Path api, Path impl) {
    plugins.add(new Plugin(api, impl));
    return this;
  }
  
  private static boolean delete(Path f) {
    try {
      if (Files.isDirectory(f)) {
        Files.list(f).forEach(ServerDeploymentBuilder::delete);
      }
      Files.delete(f);
      return true;
    } catch (IOException io) {
      return false;
    }
  }
  
  private void insureInstallPath() {
    if (installPath == null) {
      try {
        installPath = Files.createTempDirectory("tcserver");
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }
  
  public Path deploy() {
    insureInstallPath();
    try (InputStream is = ServerDeploymentBuilder.class.getResourceAsStream("/galvan-test-server.zip")) {
      ZipInputStream zip = new ZipInputStream(is);
      ZipEntry e = zip.getNextEntry();
      if (refresh && Files.exists(installPath)) {
        delete(installPath);
      }
      Files.createDirectories(installPath);
      while (e != null) {
        //System.out.println(e.getName() + " " + e.getSize() + " " + e.isDirectory());
        if (!e.isDirectory()) {
          Path p = Paths.get(e.getName()).getFileName();
          try (OutputStream out = Files.newOutputStream(installPath.resolve(p), StandardOpenOption.CREATE)) {
            long len = e.getSize();
            long count = 0;
            while (len > count) {
              out.write(zip.read());
              count++;
            }
          }
        }
        e = zip.getNextEntry();
      }
      
      Path api = Files.createDirectories(installPath.resolve("plugins").resolve("api"));
      Path lib = Files.createDirectories(installPath.resolve("plugins").resolve("lib"));
      for (Plugin p : plugins) {
        if (p.api != null) Files.list(p.api).forEach(s->copy(s, api));
        if (p.impl != null) Files.list(p.impl).forEach(s->copy(s, lib));
      }
    } catch (IOException io) {
      throw new RuntimeException(io);
    }
    return installPath;
  }
  
  public static void copy(Path s, Path d) {
    try {
      Files.copy(s, d.resolve(s.getFileName()));
    } catch (IOException io) {
      throw new RuntimeException(io);
    }
  }
  
  public static void main(String[] args) {
    new ServerDeploymentBuilder().deploy();
  }
  
  static class Plugin {
    private final Path api;
    private final Path impl;

    public Plugin(Path api, Path impl) {
      this.api = api;
      this.impl = impl;
    }
  }
}
