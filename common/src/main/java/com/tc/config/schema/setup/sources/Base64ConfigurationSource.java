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
package com.tc.config.schema.setup.sources;

import org.terracotta.license.util.Base64;

import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class Base64ConfigurationSource implements ConfigurationSource {
  private final String configString;

  public Base64ConfigurationSource(String base64ConfigString) {
    Assert.assertNotBlank(base64ConfigString);
    try {
      this.configString = new String(Base64.decode(base64ConfigString), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getInputStream(long maxTimeoutMillis) {
    try {
      return new ByteArrayInputStream(configString.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public File directoryLoadedFrom() {
    return null;
  }

  @Override
  public boolean isTrusted() {
    return false;
  }

  @Override
  public String toString() {
    return "string";
  }
}
