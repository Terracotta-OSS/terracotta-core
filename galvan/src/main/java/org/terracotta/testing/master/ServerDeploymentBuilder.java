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
  
  public Path deploy() {
    try (InputStream is = ServerDeploymentBuilder.class.getResourceAsStream("/galvan-test-server.zip")) {
      System.out.println("pwd " + Paths.get(".").toAbsolutePath());
      System.out.println("pwd " + System.getProperty("user.dir"));
      ZipInputStream zip = new ZipInputStream(is);
      ZipEntry e = zip.getNextEntry();
      while (e != null) {
        //System.out.println(e.getName() + " " + e.getSize() + " " + e.isDirectory());
        if (!e.isDirectory()) {
          Path p = Paths.get(e.getName()).getFileName();
          System.out.println(p);
          try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.CREATE)) {
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
    } catch (IOException io) {
      
    }
    return null;
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
