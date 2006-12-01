/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

import java.io.File;
import java.io.IOException;

public class FileSystemPath {

  private final File path;

  public boolean equals(Object obj) {
    if (!(obj instanceof FileSystemPath)) return false;
    FileSystemPath other = (FileSystemPath) obj; 
    return path.equals(other.path);
  }
  
  public int hashCode() {
    return path.hashCode();
  }
  
  private FileSystemPath(String path) {
    this.path = new File(path);
  }

  public FileSystemPath(File dir) {
    this.path = dir;
  }

  public static FileSystemPath existingDir(String path) {
    FileSystemPath f = new FileSystemPath(path);
    if (!f.isDirectory()) { throw new RuntimeException("Non-existent directory: " + path); }
    return f;
  }

  boolean isDirectory() {
    return path.isDirectory();
  }

  public static FileSystemPath makeExistingFile(String path) {
    FileSystemPath f = new FileSystemPath(path);
    if (!f.isFile()) { 
      throw new RuntimeException("Non-existent file: " + path); 
    }
    return f;
  }

  private boolean isFile() {
    return path.isFile();
  }

  public String toString() {
    try {
      return path.getCanonicalPath();
    } catch (IOException e) {
      return path.getAbsolutePath();
    }
  }

  public FileSystemPath existingSubdir(String subdirectoryPath) {
    return existingDir(path + "/" + subdirectoryPath);
  }

  public FileSystemPath existingFile(String fileName) {
    return makeExistingFile(this.path + "/" + fileName);
  }
  
  public Deployment warDeployment(String warName) {
    return new WARDeployment(existingFile(warName));
  }
  

  public File getFile() {
    return path;
  }

  public FileSystemPath subdir(String subdirectoryPath) {
    return new FileSystemPath(path + "/" + subdirectoryPath);
  }

  public void delete() {
    path.delete();
  }

  public FileSystemPath file(String fileName) {
    return new FileSystemPath((this.path + "/" + fileName));
  }

  public FileSystemPath mkdir(String subdir) {
    return subdir(subdir).mkdir();
  }

  private FileSystemPath mkdir() {
    path.mkdirs();
    return this;
  }

  public static FileSystemPath makeNewFile(String fileName) {
    return new FileSystemPath(fileName);
  }

}
