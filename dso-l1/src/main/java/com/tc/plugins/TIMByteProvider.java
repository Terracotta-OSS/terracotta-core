/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
  private final URL                 url;
  private final Manifest            manifest;
  private final Map<String, byte[]> entryMap;

  public TIMByteProvider(URL url) throws IOException {
    super();
    this.url = url;
    JarInputStream jis = new JarInputStream(url.openStream());
    manifest = jis.getManifest();
    entryMap = new HashMap<String, byte[]>();
    for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
      entryMap.put(entry.getName(), IOUtils.toByteArray(jis));
    }
    jis.close();
  }

  @Override
  public byte[] getResourceAsByteArray(String name) throws IOException {
    logger.info("getResourceByteArray name='" + name + "'");
    byte[] ba = entryMap.get(name);
    if (ba != null) { return ba; }
    throw new IOException("resource '" + name + "' not found");
  }

  @Override
  public String getManifestEntry(String name) {
    if (manifest != null) { return manifest.getMainAttributes().getValue(name); }
    return null;
  }

  @Override
  public byte[] getModuleBytes() throws Exception {
    InputStream is = null;

    try {
      return IOUtils.toByteArray(is = url.openStream());
    } finally {
      if (is != null) {
        IOUtils.closeQuietly(is);
      }
    }
  }
}
