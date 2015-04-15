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
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

/**
 * A mock {@link ConfigurationChangeListener}, for use in tests.
 */
public class MockConfigurationChangeListener implements ConfigurationChangeListener {

  private int       numConfigurationChangeds;
  private XmlObject lastOldConfig;
  private XmlObject lastNewConfig;

  public MockConfigurationChangeListener() {
    reset();
  }

  public void reset() {
    this.numConfigurationChangeds = 0;
    this.lastOldConfig = null;
    this.lastNewConfig = null;
  }

  @Override
  public void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    ++this.numConfigurationChangeds;
    this.lastOldConfig = oldConfig;
    this.lastNewConfig = newConfig;
  }

  public XmlObject getLastNewConfig() {
    return lastNewConfig;
  }

  public XmlObject getLastOldConfig() {
    return lastOldConfig;
  }

  public int getNumConfigurationChangeds() {
    return numConfigurationChangeds;
  }

}
