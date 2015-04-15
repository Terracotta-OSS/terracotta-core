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

/**
 * Allows you to build valid config for the entire system. This class <strong>MUST NOT</strong> invoke the actual XML
 * beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class TerracottaConfigBuilder extends BaseConfigBuilder {

  public TerracottaConfigBuilder() {
    super(0, ALL_PROPERTIES);
  }

  public void setClient(String value) {
    setProperty("clients", value);
  }

  public void setClient(L1ConfigBuilder value) {
    setProperty("clients", value);
  }

  public L1ConfigBuilder getClient() {
    if (!isSet("clients")) setClient(L1ConfigBuilder.newMinimalInstance());
    return (L1ConfigBuilder) getRawProperty("clients");
  }

  // public void setServers(String value) {
  // setProperty("servers", value);
  // }

  public void setServers(L2SConfigBuilder value) {
    setProperty("servers", value);
  }

  public L2SConfigBuilder getServers() {
    if (!isSet("servers")) setServers(L2SConfigBuilder.newMinimalInstance());
    return (L2SConfigBuilder) getRawProperty("servers");
  }

  public void setTcProperties(TcPropertiesBuilder value) {
    setProperty("tc-properties", value);
  }

  public static final String[] ALL_PROPERTIES = new String[] { "system", "clients", "servers", "application",
      "tc-properties"                        };

  @Override
  public String toString() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\n"
           + "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">\n" + elements(ALL_PROPERTIES)
           + "\n</tc:tc-config>";
  }

  public static TerracottaConfigBuilder newMinimalInstance() {
    L2SConfigBuilder l2s = L2SConfigBuilder.newMinimalInstance();
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();
    out.setServers(l2s);
    return out;
  }

  public static void main(String[] args) {
    System.err.println(newMinimalInstance());
  }

}
