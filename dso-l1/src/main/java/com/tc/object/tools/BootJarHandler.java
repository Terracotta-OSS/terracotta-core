/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tools;

import org.apache.commons.io.FileUtils;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class BootJarHandler {
  
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  
  private final boolean write_out_temp_file;
  private final File    outputFile;
  private final File    tempOutputFile;
  private final String  tempOutputFileAbsPath;
  private final String  outputFileCanonicalPath;

  public BootJarHandler(boolean write_out_temp_file, File outputFile) throws Exception {
    this.write_out_temp_file = write_out_temp_file;
    this.outputFile = outputFile;
    outputFileCanonicalPath = this.outputFile.getCanonicalPath();
    if (this.write_out_temp_file) {
      try {
        tempOutputFile = File.createTempFile("tc-bootjar", null);
        tempOutputFileAbsPath = tempOutputFile.getAbsolutePath();
        tempOutputFile.deleteOnExit();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
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
        throw new BootJarHandlerException("Failed to create path:" + tempOutputFile.getParentFile().getAbsolutePath(),
                                          ioe);
      }
    }
  }

  public void announceCreationStart() {
    announce("Creating boot JAR file at '" + outputFileCanonicalPath + "'...");
  }

  public BootJar getBootJar() throws UnsupportedVMException {
    if (write_out_temp_file) {
      return BootJar.getBootJarForWriting(this.tempOutputFile);
    } else {
      return BootJar.getBootJarForWriting(this.outputFile);
    }
  }

  public String getCreationErrorMessage() {
    if (write_out_temp_file) { return "ERROR creating temp boot jar"; }
    return "ERROR creating boot jar";
  }

  public String getCloseErrorMessage() {
    if (write_out_temp_file) { return "Failed to create temp jar file:" + tempOutputFileAbsPath; }
    return "Failed to create jar file:" + outputFileCanonicalPath;
  }

  public void announceCreationEnd() throws BootJarHandlerException {
    if (write_out_temp_file) {
      createFinalBootJar();
    }
    announce("Successfully created boot JAR file at '" + outputFileCanonicalPath + "'.");
  }

  private void createFinalBootJar() throws BootJarHandlerException {
    announceCreationStart();
    try {
      JarInputStream jarIn = new JarInputStream(new FileInputStream(tempOutputFile.getAbsolutePath()));
      Manifest manifest = jarIn.getManifest();
      if (manifest == null) {
        manifest = new Manifest();
      }

      File tempFile = File.createTempFile("tc-bootjar", null);
      tempFile.deleteOnExit();

      JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(tempFile.getAbsolutePath()), manifest);
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

      copyFile(tempFile, outputFile);
    } catch (Exception e) {
      throw new BootJarHandlerException("ERROR creating boot jar", e);
    }
    if (!tempOutputFile.delete()) {
      announce("Warning: Unsuccessful deletion of temp boot JAR file at '" + tempOutputFileAbsPath + "'.");
    }
  }

  private void announce(String msg) {
    consoleLogger.info(msg);
  }

  private void copyFile(File src, File dest) throws IOException {
    if (dest.isDirectory()) {
      dest = new File(dest, src.getName());
    }

    File tmplck = null;
    InputStream in = null;
    OutputStream out = null;
    try {
      boolean interrupted = false;
      try {
        // wait until it's okay to copy over the bootjar
        tmplck = new File(dest.getParentFile(), "tc-bootjar.lck");
        while (tmplck.exists()) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      } finally {
        Util.selfInterruptIfNeeded(interrupted);
      }
      
      // block everyone else from copying over their bootjar
      tmplck.createNewFile();
      tmplck.deleteOnExit();
      
      // copy our new bootjar file over into a temporary file
      File tmpdest = File.createTempFile("tc-bootjar", null, dest.getParentFile());
      out = new FileOutputStream(tmpdest);
      in = new FileInputStream(src);

      byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = in.read(buffer)) >= 0) {
        out.write(buffer, 0, bytesRead);
      }

      in.close();
      in = null;

      out.close();
      out = null;

      // remove any existing bootjar... 
      if(dest.exists() && !dest.delete()) {
        throw new IOException("Unable to delete '" + dest + "'");
      }
      
      // ... and replace it with the one that we just copied over
      if(!tmpdest.renameTo(dest)) {
        throw new IOException("Unable to rename '" + tmpdest + "' to '" + dest + "'");
      }
    } finally {
      // house keeping
      if (in != null) in.close();
      if (out != null) out.close();

      // now signal that it's okay for the other clients to copy over their bootjar
      tmplck.delete();      
    }
  }
}
