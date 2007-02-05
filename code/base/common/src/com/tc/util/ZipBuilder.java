/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * NOT THREAD SAFE
 */
public class ZipBuilder implements ArchiveBuilder {

  private final CRC32           crc32 = new CRC32();
  private final ZipOutputStream zout;
  private final HashSet       dirSet   = new HashSet();
  private final HashSet       entrySet = new HashSet();

  public ZipBuilder(File archiveFile, boolean useCompression) throws IOException {
    zout = getArchiveOutputStream(archiveFile);
    if (useCompression) {
      zout.setMethod(ZipEntry.DEFLATED);
      zout.setLevel(9);
    } else {
      zout.setMethod(ZipEntry.STORED);
      zout.setLevel(0);
    }
  }

  public final void putTraverseDirectory(File dir, String dirName) throws IOException {
    if (!dir.isDirectory()) throw new IOException("Unexpected Exception: " + dir + "\nis not a directory");
    putDirEntry(dirName);
    String[] files = dir.list();
    for (int i = 0; i < files.length; i++) {
      File file = new File(dir + File.separator + files[i]);
      if (file.isDirectory()) {
        putTraverseDirectory(file, dirName + File.separator + file.getName());
        continue;
      }
      putEntry(dirName + File.separator + files[i], readFile(file));
    }
  }
  
  public final void putDirEntry(String file) throws IOException {
    if (dirSet.contains(file)) return;
    dirSet.add(file);
    ZipEntry entry = createEntry(archivePath(file) + "/");
    entry.setSize(0);
    entry.setCrc(0);
    zout.putNextEntry(entry);
  }

  public final void putEntry(String file, byte[] bytes) throws IOException {
    if (entrySet.contains(file.toString())) return;
    entrySet.add(file.toString());
    ZipEntry entry = createEntry(archivePath(file));
    entry.setSize(bytes.length);
    entry.setCrc(getCrc32(bytes));
    zout.putNextEntry(entry);
    zout.write(bytes, 0, bytes.length);
    System.out.print(".");
  }

  public final void finish() throws IOException {
    zout.close();
  }

  public final byte[] readFile(File file) throws IOException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
    byte[] bytes = new byte[in.available()];
    in.read(bytes);
    in.close();
    return bytes;
  }

  protected ZipEntry createEntry(String name) {
    return new ZipEntry(name);
  }
  
  protected ZipOutputStream getArchiveOutputStream(File archiveFile) throws IOException {
    if (zout != null) throw new IllegalStateException("ArchiveOutputStream has already been instantiated.");
    return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archiveFile)));
  }

  private long getCrc32(byte[] bytes) {
    crc32.update(bytes);
    long checksum = crc32.getValue();
    crc32.reset();
    return checksum;
  }

  private String archivePath(String file) {
    if (File.separator.equals("/")) return file;
    return file.replaceAll("\\\\", "/");
  }
}
