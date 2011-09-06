/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class JarBuilder extends ZipBuilder {

  private boolean isInit;
  
  public JarBuilder(File archiveFile) throws IOException {
    super(archiveFile, false);
  }

  protected final ZipEntry createEntry(String name) {
    return new JarEntry(name);
  }
  
  protected final ZipOutputStream getArchiveOutputStream(File archiveFile) throws IOException {
    if (isInit) super.getArchiveOutputStream(archiveFile); // throws Exception
    isInit = true;
    return new JarOutputStream(new BufferedOutputStream(new FileOutputStream(archiveFile)), new Manifest());
  }
}
