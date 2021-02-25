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

import com.tc.productinfo.BuildInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 */
public class BaseBuildInfo implements BuildInfo {

  private final Properties props = new Properties();

  public BaseBuildInfo(InputStream is) throws IOException {
    props.load(is);
    is.close();
  }

  @Override
  public String getVersion() {
    return props.getProperty("terracotta.build.version");
  }

  @Override
  public String getVersionMessage() {
    return props.getProperty("terracotta.build.version.message", "");
  }

  @Override
  public String getMonkier() {
    return "Terracotta";
  }

  @Override
  public String getTimestamp() {
    return props.getProperty("terracotta.build.timestamp");
  }

  @Override
  public String getBranch() {
    return props.getProperty("terracotta.build.branch");
  }

  @Override
  public String getRevision() {
    return props.getProperty("terracotta.build.revision");
  }

  @Override
  public String getCopyright() {
    return "Copyright (c) 2003-2020 Terracotta, Inc. All rights reserved.";
  }

  @Override
  public String getValue(String name) {
    return props.getProperty(name);
  }


}
