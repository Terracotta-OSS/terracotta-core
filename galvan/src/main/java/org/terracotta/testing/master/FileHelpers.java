/*
 * Copyright Terracotta, Inc.
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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;


public class FileHelpers {
  public static void cleanDirectory(final ContextualLogger logger, String directoryToClean) throws IOException {
    final Path start = FileSystems.getDefault().getPath(directoryToClean);
    // We will assume that we are intended to create the directory, if it doesn't already exist.
    if (!start.toFile().exists()) {
      boolean didMake = start.toFile().mkdir();
      Assert.assertTrue(didMake);
    }
    // We need the directory to be a directory (as we don't want to follow symlinks anywhere in this delete operation).
    Assert.assertTrue(start.toFile().isDirectory());
    // We don't want to delete the starting directory, but we do want to delete everything in it.
    Files.walkFileTree(start, new FileVisitor<Path>(){
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

  public static String createTempCopyOfDirectory(ContextualLogger logger, String targetParentDirectoryString, String newDirectoryName, String sourceDirectoryString) throws IOException {
    FileSystem fileSystem = FileSystems.getDefault();
    Path targetParentDirectory = fileSystem.getPath(targetParentDirectoryString);
    logger.output(" Target directory: " + targetParentDirectoryString);
    Assert.assertTrue(targetParentDirectory.toFile().isDirectory());
    logger.output(" Source directory: " + sourceDirectoryString);
    Path sourceDirectory = fileSystem.getPath(sourceDirectoryString);
    Assert.assertTrue(sourceDirectory.toFile().exists());
    Assert.assertTrue(sourceDirectory.toFile().isDirectory());
    // Create the target directory path.
    Path targetDirectory = targetParentDirectory.resolve(newDirectoryName);
    boolean didMake = targetDirectory.toFile().mkdir();
    Assert.assertTrue(didMake);
    DirectoryCopier copier = new DirectoryCopier(logger, targetDirectory, sourceDirectory);
    Files.walkFileTree(sourceDirectory, copier);
    return targetDirectory.toAbsolutePath().toString();
  }

  public static String createTempEmptyDirectory(String targetParentDirectoryString, String newDirectoryName) {
    FileSystem fileSystem = FileSystems.getDefault();
    Path targetParentDirectory = fileSystem.getPath(targetParentDirectoryString);
    Assert.assertTrue(targetParentDirectory.toFile().isDirectory());
    Path targetDirectory = targetParentDirectory.resolve(newDirectoryName);
    boolean didMake = targetDirectory.toFile().mkdir();
    Assert.assertTrue(didMake);
    return targetDirectory.toAbsolutePath().toString();
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


  public static void copyJarsToServer(ContextualLogger logger, String instanceServerInstallPath, List<String> extraJarPaths) throws IOException {
    // We know we want to copy these into plugins/lib.
    FileSystem fileSystem = FileSystems.getDefault();
    Path pluginsLibDirectory = fileSystem.getPath(instanceServerInstallPath, "plugins", "lib");
    // This needs to be a directory.
    Assert.assertTrue(pluginsLibDirectory.toFile().isDirectory());
    for (String oneJarPath : extraJarPaths) {
      Path sourcePath = fileSystem.getPath(oneJarPath);
      // This file must exist.
      Assert.assertTrue(sourcePath.toFile().isFile());
      Path targetPath = pluginsLibDirectory.resolve(sourcePath.getFileName());
      // This must not exist.
      logger.output("Installing JAR: " + targetPath + "...");
      Assert.assertFalse(targetPath.toFile().exists());
      Files.copy(sourcePath, targetPath);
      logger.output("Done");
    }
  }
}
