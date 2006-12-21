/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class BootJarHandler {
  private final static String TEMP_OUTPUT_FILE_PREFIX = "temp-";
  private final boolean       write_out_temp_file;
  private final File          outputFile;
  private final File          tempOutputFile;
  private final String        tempDir;
  private final String        tempOutputFileAbsPath;
  private final String        outputFileAbsPath;

  public BootJarHandler(boolean write_out_temp_file, File outputFile) {
    this.write_out_temp_file = write_out_temp_file;
    this.outputFile = outputFile;
    outputFileAbsPath = this.outputFile.getAbsolutePath();
    if (this.write_out_temp_file) {
        tempDir = getTempDir();
        tempOutputFile = new File(tempDir, TEMP_OUTPUT_FILE_PREFIX + this.outputFile.getName());
        tempOutputFileAbsPath = tempOutputFile.getAbsolutePath();
      } else {
        tempDir = "";
        tempOutputFile = null;
        tempOutputFileAbsPath = "";
      }
  }
  
  public void validateDirectoryExists() throws BootJarHandlerException {
    try {
      FileUtils.forceMkdir(outputFile.getAbsoluteFile().getParentFile());
    } catch (IOException ioe) {
      throw new BootJarHandlerException("Failed to create path:" + outputFile.getParentFile().getAbsolutePath(), ioe);
    }
    
    if (write_out_temp_file) {
      try {
        FileUtils.forceMkdir(tempOutputFile.getAbsoluteFile().getParentFile());
      } catch (IOException ioe) {
        throw new BootJarHandlerException("Failed to create path:" + tempOutputFile.getParentFile().getAbsolutePath(), ioe);
      }      
    }
  }
  
  public void announceCreationStart() {
    if (write_out_temp_file) {
      announce("Creating temp boot JAR at '" + tempOutputFileAbsPath + "...");
    } else {
      announce("Creating boot JAR at '" + outputFileAbsPath + "...");
    }
  }
 
  public BootJar getBootJar() throws UnsupportedVMException {
    if (write_out_temp_file) {
      return BootJar.getBootJarForWriting(this.tempOutputFile);
    } else {
      return BootJar.getBootJarForWriting(this.outputFile);
    }
  }
  
  public String getCreationErrorMessage() {
    if (write_out_temp_file) {
      return "ERROR creating temp boot jar";
    }
    return "ERROR creating boot jar";
  }
  
  public String getCloseErrorMessage() {
    if (write_out_temp_file) {
      return "Failed to create temp jar file:" + tempOutputFileAbsPath;
    }
    return "Failed to create jar file:" + outputFileAbsPath;
  }
  
  public void announceCreationEnd() throws BootJarHandlerException {
    if (write_out_temp_file) {
      announce("Successfully created temp boot JAR file at '" + tempOutputFileAbsPath + "'.");
      createFinalBootJar();
    } else {
      announce("Successfully created boot JAR file at '" + outputFileAbsPath + "'.");
    }
  }
  
  private void createFinalBootJar() throws BootJarHandlerException {
    announce("Creating boot JAR at '" + outputFileAbsPath + "...");
    try {
      JarInputStream jarIn = new JarInputStream(new FileInputStream(tempOutputFile.getAbsolutePath()));

      Manifest manifest = jarIn.getManifest();
      if (manifest == null) {
        manifest = new Manifest();
      }
      JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(outputFile.getAbsolutePath()), manifest);
      byte[] buffer = new byte[4096];
      JarEntry entry;
      while ((entry = jarIn.getNextJarEntry()) != null) {
        if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
          continue;
        }
        jarOut.putNextEntry(entry);
        int read;
        while ((read = jarIn.read(buffer)) != -1) {
          jarOut.write(buffer, 0, read);
        }
        jarOut.closeEntry();
      }
      jarOut.flush();
      jarOut.close();
      jarIn.close();
    } catch (Exception e) {
      throw new BootJarHandlerException("ERROR creating boot jar", e);
    }
    announce("Successfully created boot JAR file at '" + outputFileAbsPath + "'.");
    announce("Deleting temp boot JAR file at '" + tempOutputFileAbsPath + "'.");
    if (tempOutputFile.delete()) {
      announce("Successfully deleted temp boot JAR file at '" + tempOutputFileAbsPath + "'.");
    } else {
      announce("Warning: Unsuccessful deletion of temp boot JAR file at '" + tempOutputFileAbsPath + "'.");
    }  
  }
  
  private String getTempDir() {
    String tmpDir = System.getProperty("java.io.tmpDir");
    if (tmpDir == null) {
      tmpDir = outputFile.getParent();
    }
    return tmpDir;
  }
  
  private void announce(String msg) {
    System.out.println(msg);
  }
  
}
