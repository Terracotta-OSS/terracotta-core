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

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;


public class FileHelpers {
  public static void cleanDirectory(final ContextualLogger logger, Path start) throws IOException {
    // We will assume that we are intended to create the directory, if it doesn't already exist.
    if (!start.toFile().exists()) {
      boolean didMake = start.toFile().mkdir();
      Assert.assertTrue(didMake);
    }
    // We need the directory to be a directory (as we don't want to follow symlinks anywhere in this delete operation).
    Assert.assertTrue(start.toFile().isDirectory());
    // We don't want to delete the starting directory, but we do want to delete everything in it.
    Files.walkFileTree(start, new FileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // Do nothing.
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            // This clearly can't be the start.
            Assert.assertFalse(start.equals(file));
            // Delete the file.
            Files.delete(file);
            logger.output("Deleted file " + file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            logger.error("visitFileFailed: \"" + file + "\"");
            throw exc;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (null == exc) {
              // Make sure that this isn't the starting directory.
              if (!start.equals(dir)) {
                Files.delete(dir);
                logger.output("Deleted directory " + dir);
              }
            } else {
              throw exc;
            }
            return FileVisitResult.CONTINUE;
          }
        }
    );
  }

  public static Path createTempCopyOfDirectory(ContextualLogger logger, Path targetParentDir,
                                               String newDirectoryName, Path sourceDir) throws IOException {
    logger.output(" Target directory: " + targetParentDir);
    Assert.assertTrue(Files.isDirectory(targetParentDir));
    logger.output(" Source directory: " + sourceDir);
    Assert.assertTrue(Files.exists(sourceDir));
    Assert.assertTrue(Files.isDirectory(sourceDir));

    Path targetDir = targetParentDir.resolve(newDirectoryName);
    boolean didMake = targetDir.toFile().mkdir();
    Assert.assertTrue(didMake);
    DirectoryCopier copier = new DirectoryCopier(logger, targetDir, sourceDir);
    Files.walkFileTree(sourceDir, copier);
    return targetDir.toAbsolutePath();
  }

  public static Path createTempEmptyDirectory(Path targetParentDirectory, String newDirectoryName) {
    Assert.assertTrue(targetParentDirectory.toFile().isDirectory());
    Path targetDirectory = targetParentDirectory.resolve(newDirectoryName);
    boolean didMake = targetDirectory.toFile().mkdir();
    Assert.assertTrue(didMake);
    return targetDirectory.toAbsolutePath();
  }

  public static void copyJarsToServer(ContextualLogger logger, Path instanceServerInstallPath, Set<Path> extraJarPaths) throws IOException {
    // We know we want to copy these into plugins/lib.
    Path pluginsLibDirectory = instanceServerInstallPath.resolve("server").resolve("plugins").resolve("lib");
    // This needs to be a directory.
    Assert.assertTrue(Files.isDirectory(pluginsLibDirectory));
    for (Path sourcePath : extraJarPaths) {
      // This file must exist.
      if (!Files.isRegularFile(sourcePath)) {
        throw new IllegalArgumentException("JAR path is not a file: " + sourcePath);
      }
      Path targetPath = pluginsLibDirectory.resolve(sourcePath.getFileName());
      // This must not exist.
      logger.output("Installing JAR: " + targetPath + "...");
      Assert.assertFalse(targetPath.toFile().exists());
      Files.copy(sourcePath, targetPath);
      logger.output("Done");
    }
  }

  public static void ensureDirectoryExists(ContextualLogger logger, Path directoryPath) {
    logger.output(" Ensure directory: " + directoryPath);
    ensureExistsRecursive(directoryPath);
  }

  public static void touchEmptyFile(ContextualLogger logger, String parentDirectoryPath, String fileName) throws IOException {
    File newFile = new File(parentDirectoryPath, fileName);
    logger.output("Creating empty file: " + newFile.getAbsolutePath());
    boolean didCreate = newFile.createNewFile();
    // This helper has no notion of failure to create (without exception).
    Assert.assertTrue(didCreate);
  }


  private static class DirectoryCopier implements FileVisitor<Path> {
    private final ContextualLogger logger;
    private final Path targetDirectory;
    private final Path sourceDirectory;
    private Path currentTargetDirectory;

    public DirectoryCopier(ContextualLogger logger, Path targetDirectory, Path sourceDirectory) {
      this.logger = logger;
      this.targetDirectory = targetDirectory;
      this.sourceDirectory = sourceDirectory;
      this.currentTargetDirectory = this.targetDirectory;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      // Make sure that this isn't the starting directory.
      if (!this.sourceDirectory.equals(dir)) {
        // Create this directory in the target and make it our current target.
        Path newDirectory = this.currentTargetDirectory.resolve(dir.getFileName());
        boolean didCreate = newDirectory.toFile().mkdir();
        Assert.assertTrue(didCreate);
        logger.output("Created directory: " + newDirectory);
        this.currentTargetDirectory = newDirectory;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      // Copy this file to the current target.
      Path targetFile = this.currentTargetDirectory.resolve(file.getFileName());
      Files.copy(file, targetFile);
      logger.output("Copied file: " + targetFile);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      logger.error("FATAL ERROR IN VISIT OF: " + file);
      throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      // As long as this isn't our source, just pop the current target.
      if (!this.sourceDirectory.equals(dir)) {
        this.currentTargetDirectory = this.currentTargetDirectory.getParent();
      }
      return FileVisitResult.CONTINUE;
    }
  }

  private static void ensureExistsRecursive(Path directoryToCreate) {
    if (Files.exists(directoryToCreate)) {
      // This exists so it must be a directory.
      Assert.assertTrue(Files.isDirectory(directoryToCreate));
    } else {
      ensureExistsRecursive(directoryToCreate.getParent());
      boolean didMake = directoryToCreate.toFile().mkdir();
      Assert.assertTrue(didMake);
    }
  }
}
