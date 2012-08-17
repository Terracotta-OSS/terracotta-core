/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.locks.ReentrantLock;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip.Zip32InputArchive;

public class Jar {
  // this class synchronized for many reasons:
  // - to avoid concurrent inflation
  // - update to access time that will be observed by other threads
  // - the underlying archive itself is it not thread safe!

  private static final long   NOT_INITIALIZED = -1;

  private final ReentrantLock lock            = new ReentrantLock();
  private final URL           source;
  private final JarManager    jarManager;

  private long                lastAccess      = NOT_INITIALIZED;
  private Zip32InputArchive   archive;
  private byte[]              contents;

  Jar(URL source, JarManager jarManager) {
    this.source = source;
    this.jarManager = jarManager;
  }

  public URL getSource() {
    return source;
  }

  void lock() {
    lock.lock();
  }

  void unlock() {
    lock.unlock();
  }

  boolean isDeflated() {
    lock();
    try {
      return archive == null && contents == null;
    } finally {
      unlock();
    }
  }

  boolean deflateIfIdle(long idle) {
    lock();
    try {
      if (lastAccess == NOT_INITIALIZED || archive == null) return true;

      if ((System.currentTimeMillis() - lastAccess) > idle) {
        try {
          archive.close();
        } catch (IOException e) {
          // ignore
        }

        contents = null;
        archive = null;
        return true;
      }

      return false;
    } finally {
      unlock();
    }
  }

  public boolean hasResource(String res) throws IOException {
    lock();
    try {
      touch();
      return archive.getArchiveEntry(res) != null;
    } finally {
      unlock();
    }
  }

  private void touch() throws IOException {
    this.lastAccess = System.currentTimeMillis();
    inflateIfNeeded();
  }

  public byte[] lookup(String resource) throws IOException {
    lock();
    try {
      touch();
      InputStream in = archive.getInputStream(resource);
      if (in == null) { return null; }
      return Util.extract(in);
    } finally {
      unlock();
    }
  }

  public byte[] contents() throws IOException {
    lock();
    try {
      touch();
      return contents;
    } finally {
      unlock();
    }
  }

  private void inflateIfNeeded() throws IOException {
    if (archive == null) {
      // handle multilevel jar in jars
      String extForm = source.toExternalForm();
      if (extForm.startsWith("jar:jar:")) {
        int nesting = Util.getNumJarSeparators(extForm);
        if (nesting > 2) {
          // cannot handle more than 3 levels of nesting
          throw new IOException("Cannot handle more than 3 levels of nested jar lookups");
        } else if (nesting <= 0) { throw new MalformedURLException("No '!/' found in URL beginning with 'jar:'"); }
        extForm = extForm.substring("jar:".length());
        String secondJarUrl = extForm.substring(0, extForm.lastIndexOf("!/"));
        String resourceInSecondJar = extForm.substring(extForm.lastIndexOf("!/") + "!/".length());
        if (resourceInSecondJar.startsWith("/")) {
          resourceInSecondJar = resourceInSecondJar.substring(1);
        }
        Jar secondJar = jarManager.getOrCreate(secondJarUrl, new URL(secondJarUrl));
        contents = secondJar.lookup(resourceInSecondJar);
      } else {
        contents = Util.extract(source.openStream());
      }
      archive = new Zip32InputArchive(new ByteArrayReadOnlyFile(contents), "UTF-8", false, false);
      jarManager.jarOpened(this);
    }
  }

  @Override
  public String toString() {
    return source.toExternalForm();
  }

}
