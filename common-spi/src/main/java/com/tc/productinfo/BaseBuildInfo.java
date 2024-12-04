/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.productinfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 */
public class BaseBuildInfo implements BuildInfo {

  private final Properties props = new Properties();

  public BaseBuildInfo(InputStream is) throws IOException {
    if (is != null) {
      try (is) {
        props.load(is);
      }
    }
  }

  @Override
  public String getVersion() {
    return props.getProperty("terracotta.build.version","");
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
    return props.getProperty("terracotta.build.timestamp","");
  }

  @Override
  public String getBranch() {
    return props.getProperty("terracotta.build.branch","");
  }

  @Override
  public String getRevision() {
    return props.getProperty("terracotta.build.revision","");
  }

  @Override
  public String getCopyright() {
    return "";
  }

  @Override
  public String getValue(String name) {
    return props.getProperty(name);
  }


}
