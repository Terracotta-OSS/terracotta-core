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
package com.tc.config.test.schema;

public class GarbageCollectionConfigBuilder extends BaseConfigBuilder {

  private static final String[] ALL_PROPERTIES = new String[] { "enabled", "interval", "verbose" };

  public GarbageCollectionConfigBuilder() {
    super(3, ALL_PROPERTIES);
  }

  public void setGCEnabled(boolean data) {
    setProperty("enabled", data);
  }

  public void setGCEnabled(String data) {
    setProperty("enabled", data);
  }

  public void setGCVerbose(boolean data) {
    setProperty("verbose", data);
  }

  public void setGCVerbose(String data) {
    setProperty("verbose", data);
  }

  public void setGCInterval(int data) {
    setProperty("interval", data);
  }

  @Override
  public String toString() {
    String out = "";

    out += openElement("garbage-collection");

    for (String e : ALL_PROPERTIES) {
      out += element(e);
    }

    out += closeElement("garbage-collection");

    return out;
  }

}
