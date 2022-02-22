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
package com.tc.productinfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 */
public class BasePatchInfo implements PatchInfo {

  private final Properties props = new Properties();
  private static final String UNKNOWN = "[unknown]";
  private final boolean patched;

  public BasePatchInfo(InputStream is) throws IOException {
    if (is != null) {
      props.load(is);
      is.close();
      patched = true;
    } else {
      patched = false;
    }
  }

  @Override
  public String getTimestamp() {
    return props.getProperty("terracotta.patch.timestamp", UNKNOWN);
  }

  @Override
  public String getBranch() {
    return props.getProperty("terracotta.patch.branch", UNKNOWN);
  }

  @Override
  public String getRevision() {
    return props.getProperty("terracotta.patch.revision", UNKNOWN);
  }

  @Override
  public String getLevel() {
    return props.getProperty("terracotta.patch.level", UNKNOWN);
  }

  @Override
  public int count() {
    return 0;
  }

  public boolean isPatched() {
    return patched;
  }

  @Override
  public String getValue(String name) {
    return props.getProperty(name, UNKNOWN);
  }
}
