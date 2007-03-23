/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class BootJarHandler {
  private final boolean write_out_temp_file;
  private final File    outputFile;
  private final File    tempOutputFile;
  private final String  tempOutputFileAbsPath;
  private final String  outputFileAbsPath;

  public BootJarHandler(boolean write_out_temp_file, File outputFile) {
    this.write_out_temp_file = write_out_temp_file;
    this.outputFile = outputFile;
    outputFileAbsPath = this.outputFile.getAbsolutePath();
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
    announce("Creating boot JAR at '" + outputFileAbsPath + "...");
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
    return "Failed to create jar file:" + outputFileAbsPath;
  }

  public void announceCreationEnd() throws BootJarHandlerException {
    if (write_out_temp_file) {
      createFinalBootJar();
    }
    announce("Successfully created boot JAR file at '" + outputFileAbsPath + "'.");
  }

  private void createFinalBootJar() throws BootJarHandlerException {
    announce("Creating boot JAR at '" + outputFileAbsPath + "...");
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

//  private File getTempDir() {
//    String tmpDir = System.getProperty("java.io.tmpdir");
//    if (tmpDir == null) {
//      tmpDir = outputFile.getParent();
//    }
//    return new File(tmpDir);
//  }

  private void announce(String msg) {
    System.out.println(msg);
  }

  private void copyFile(File src, File dest) throws IOException {
    if (dest.isDirectory()) {
      dest = new File(dest, src.getName());
    }

    InputStream in = null;
    OutputStream out = null;
    FileLock lock = null;
    try {
      File tmpdest = new File(dest.getAbsolutePath() + ".tmp");
      out = new FileOutputStream(tmpdest);
      FileChannel channel = ((FileOutputStream) out).getChannel();
      lock = channel.lock();
      in = new FileInputStream(src);

      byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = in.read(buffer)) >= 0) {
        out.write(buffer, 0, bytesRead);
      }

      in.close();
      in = null;

      lock.release();
      lock = null;

      out.close();
      out = null;

      if(dest.exists() && !dest.delete()) {
        throw new IOException("Unable to delete '"+dest+"'");
      }
      if(!tmpdest.renameTo(dest)) {
        throw new IOException("Unable to rename '"+tmpdest+"' to '"+dest+"'");
      }
    } finally {
      if (in != null) in.close();
      if (lock != null) lock.release();
      if (out != null) out.close();
    }
  }
}
