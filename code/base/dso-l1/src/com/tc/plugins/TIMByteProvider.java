/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.plugins;

import org.apache.commons.io.IOUtils;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.TIMByteProviderMBean;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class TIMByteProvider implements TIMByteProviderMBean {
  private static final TCLogger     logger = TCLogging.getLogger(TIMByteProvider.class);
  private final String              url;
  private final Manifest            manifest;
  private final Map<String, byte[]> entryMap;

  public TIMByteProvider(String url) throws IOException {
    super();
    URL location = new URL(this.url = url);
    JarInputStream jis = new JarInputStream(location.openStream());
    manifest = jis.getManifest();
    entryMap = new HashMap<String, byte[]>();
    for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
      entryMap.put(entry.getName(), IOUtils.toByteArray(jis));
    }
    jis.close();
  }

  public byte[] getResourceAsByteArray(String name) throws IOException {
    logger.info("getResourceByteArray name='" + name + "'");
    byte[] ba = entryMap.get(name);
    if (ba != null) { return ba; }
    throw new IOException("resource '" + name + "' not found");
  }

  public String getManifestEntry(String name) {
    if (manifest != null) { return manifest.getMainAttributes().getValue(name); }
    return null;
  }

  public byte[] getModuleBytes() throws IOException {
    URL location = new URL(url);
    InputStream is = null;

    try {
      return IOUtils.toByteArray(is = location.openStream());
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
