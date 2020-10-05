/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;


import com.tc.util.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * NOT THREAD SAFE
 */
public class ZipBuilder implements ArchiveBuilder {

  private final CRC32           crc32    = new CRC32();
  private final ZipOutputStream zout;
  private final Set<String>     dirSet   = new HashSet<String>();
  private final Set<String>     entrySet = new HashSet<String>();
  private final boolean         verbose;

  public ZipBuilder(File archiveFile, boolean useCompression) throws IOException {
    this(archiveFile, useCompression, false);
  }

  public ZipBuilder(File archiveFile, boolean useCompression, boolean verbose) throws IOException {
    zout = getArchiveOutputStream(archiveFile);
    if (useCompression) {
      zout.setMethod(ZipEntry.DEFLATED);
      zout.setLevel(9);
    } else {
      zout.setMethod(ZipEntry.STORED);
      zout.setLevel(0);
    }
    this.verbose = verbose;
  }

  @Override
  public final void putTraverseDirectory(File dir, String dirName) throws IOException {
    if (!dir.isDirectory()) throw new IOException("Unexpected Exception: " + dir + "\nis not a directory");
    if (dirName != null && !dirName.isEmpty()) {
      putDirEntry(dirName);
      dirName = dirName + File.separator;
    } else {
      dirName = "";
    }
    String[] files = dir.list();
    for (String file2 : files) {
      File file = new File(dir.getAbsolutePath() + File.separator + file2);
      if (file.isDirectory()) {
        putTraverseDirectory(file, dirName + file.getName());
        continue;
      }
      putEntry(dirName + file2, readFile(file));
    }
  }

  @Override
  public final void putDirEntry(String file) throws IOException {
    if (file == null) {
      return;
    }
    if (dirSet.contains(file)) return;
    dirSet.add(file);
    String dirEntry = archivePath(file) + "/";
    ZipEntry entry = createEntry(dirEntry);
    entry.setSize(0);
    entry.setCrc(0);
    zout.putNextEntry(entry);
    if (verbose) System.out.println(dirEntry);
  }

  @Override
  public final void putEntry(String file, byte[] bytes) throws IOException {
    if (entrySet.contains(file.toString())) return;
    entrySet.add(file.toString());
    String fileEntry = archivePath(file);
    ZipEntry entry = createEntry(fileEntry);
    entry.setSize(bytes.length);
    entry.setCrc(getCrc32(bytes));
    zout.putNextEntry(entry);
    zout.write(bytes, 0, bytes.length);
    if (verbose) System.out.println(fileEntry);
  }

  @Override
  public final void finish() throws IOException {
    zout.close();
  }

  @Override
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

  public static void unzip(InputStream archive, File destDir) throws IOException {
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(archive);
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File file = new File(destDir, entry.getName());
        if (entry.isDirectory()) {
          if (!file.mkdirs()) { throw new IOException("failed to create directory " + file.getAbsolutePath()); }
        } else {
          FileOutputStream out= null;
          try {
            out = new FileOutputStream(file);
            IOUtils.copy(zis, out);
          } finally {
            IOUtils.closeQuietly(out);
          }          
        }
        zis.closeEntry();
      }
    } finally {
      if (zis != null) zis.close();
    }
  }
}
