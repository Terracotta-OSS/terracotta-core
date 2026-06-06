/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ServerDeploymentBuilder {
  private Path installPath;
  private final List<Plugin> plugins = new ArrayList<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerDeploymentBuilder.class);
  private static final String CHECKSUM_FILE = ".galvan-checksum";

  public ServerDeploymentBuilder() {

  }

  public ServerDeploymentBuilder(Path install) {
    this.installPath = install;
  }

  public ServerDeploymentBuilder installPath(Path install) {
    this.installPath = install;
    return this;
  }

  public ServerDeploymentBuilder addPlugin(Path api, Path impl) {
    plugins.add(new Plugin(api, impl));
    return this;
  }

  public static ServerDeploymentBuilder begin(Path dest) {
    return new ServerDeploymentBuilder(dest);
  }

  public static ServerDeploymentBuilder begin(File dest) {
    return begin(dest.toPath());
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

  private Path ensureInstallPath() {
    try {
      if (installPath == null) {
          if (System.getProperty("galvan.server") != null) {
            installPath = Files.createDirectories(Paths.get(System.getProperty("galvan.server")));
          } else {
            installPath = Files.createTempDirectory("tcserver");
          }

      } else {
        installPath = Files.createDirectories(installPath);
      }
      return installPath;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public Path deploy() {
    return deploy(false);
  }

  public Path deploy(boolean refresh) {
    Path ip = ensureInstallPath();
    Path dl = ip.resolve("deploy.lock");
    try (FileChannel dc = FileChannel.open(dl, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
      while (true) {
        try (FileLock lock = dc.lock()) {
          String currentChecksum = calculateZipChecksum();
          String storedChecksum = readStoredChecksum(ip);

          boolean needsRefresh = refresh ||
              !currentChecksum.equals(storedChecksum);

          if (needsRefresh) {
            LOGGER.info("Deploying server kit to {} (checksum: {})", ip, currentChecksum);
            if (Files.exists(ip)) {
              Files.list(ip).forEach(p->{
                if (!p.getFileName().toString().equals("deploy.lock")) {
                  delete(p);
                }
              });
            }
            deployServer();
            writeChecksum(ip, currentChecksum);
          } else {
            LOGGER.debug("Server kit at {} is up-to-date (checksum: {})", ip, currentChecksum);
          }
          lock.release();
          return ip;
        } catch (OverlappingFileLockException overlap) {
          TimeUnit.SECONDS.sleep(1);
          LOGGER.info("another thread is building the server deployment at {}.  Waiting 1 sec.", ip);
        }
      }
    } catch (IOException io) {
      throw new UncheckedIOException(io);
    } catch (InterruptedException i) {
      throw new RuntimeException(i);
    }
  }

  private void deployServer() throws IOException {
    byte[] buffer = new byte[4096];
    try (InputStream server= org.terracotta.testing.master.ServerDeploymentBuilder.class.getResourceAsStream("/galvan-test-server.zip")) {
      ZipInputStream zip = new ZipInputStream(server);
      ZipEntry e = zip.getNextEntry();

      while (e != null) {
        if (!e.isDirectory()) {
          Path p = Paths.get(e.getName()).getFileName();
          try (OutputStream out = Files.newOutputStream(installPath.resolve(p), StandardOpenOption.CREATE)) {
            long len = e.getSize();
            long count = 0;
            while (len > count) {
              long chunk = Math.min(buffer.length, len - count);
              int run = zip.read(buffer,0, Math.toIntExact(chunk));
              out.write(buffer,0,run);
              count+=run;
            }
          }
        }
        e = zip.getNextEntry();
      }
      installDefaultPlugins();
      Path api = Files.createDirectories(installPath.resolve("plugins").resolve("api"));
      Path lib = Files.createDirectories(installPath.resolve("plugins").resolve("lib"));
      for (Plugin p : plugins) {
        if (p.api != null && Files.isDirectory(p.api)) {
          Files.list(p.api).forEach(s->copy(s, api));
        }
        if (p.impl != null && Files.isDirectory(p.impl)) {
          Files.list(p.impl).forEach(s->copy(s, lib));
        }
      }
    }
  }

  private void installDefaultPlugins() {
    String defaultPlugin = System.getProperty("galvan.plugin");
    if (defaultPlugin != null) {
      Path p = Paths.get(defaultPlugin);
      if (plugins != null) {
        this.addPlugin(p.resolve("api"), p.resolve("lib"));
      }
    }
  }

  private static void copy(Path s, Path d) {
    try {
      if (Files.isDirectory(s)) {
        Files.list(s).forEach(u->copy(u, d.resolve(s.getFileName())));
      } else {
        Files.copy(s, d.resolve(s.getFileName()));
      }
    } catch (FileAlreadyExistsException already) {
      LOGGER.info("ignoring {}", s, already);
    } catch (IOException io) {
      throw new RuntimeException(io);
    }
  }

  private String calculateZipChecksum() throws IOException {
    // First try to read the pre-calculated checksum from the build
    try (InputStream checksumStream = ServerDeploymentBuilder.class.getResourceAsStream("/galvan-test-server.zip.sha256")) {
      if (checksumStream != null) {
        String checksum = new String(checksumStream.readAllBytes()).trim();
        if (!checksum.isEmpty()) {
          LOGGER.debug("Using pre-calculated checksum: {}", checksum);
          return checksum;
        }
      }
    } catch (IOException e) {
      LOGGER.warn("Failed to read pre-calculated checksum, will calculate at runtime", e);
    }

    // Fallback: calculate checksum at runtime if pre-calculated one is not available
    LOGGER.debug("Calculating checksum at runtime");
    try (InputStream server = ServerDeploymentBuilder.class.getResourceAsStream("/galvan-test-server.zip")) {
      if (server == null) {
        throw new IOException("galvan-test-server.zip not found in classpath");
      }
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      int read;
      while ((read = server.read(buffer)) != -1) {
        md.update(buffer, 0, read);
      }
      return bytesToHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private String readStoredChecksum(Path installPath) {
    Path checksumFile = installPath.resolve(CHECKSUM_FILE);
    try {
      if (Files.exists(checksumFile)) {
        return Files.readString(checksumFile).trim();
      }
    } catch (IOException e) {
      LOGGER.warn("Failed to read checksum file at {}", checksumFile, e);
    }
    return "";
  }

  private void writeChecksum(Path installPath, String checksum) throws IOException {
    Files.writeString(installPath.resolve(CHECKSUM_FILE), checksum);
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
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
