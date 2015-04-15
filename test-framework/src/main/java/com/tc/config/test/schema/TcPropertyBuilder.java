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

public class TcPropertyBuilder extends BaseConfigBuilder{

  private static final String[] ALL_PROPERTIES = concat(new Object[]{"name", "value"});
  private String name;
  private String value;
  
  public TcPropertyBuilder(String name, String value){
    super(3, ALL_PROPERTIES);
    this.name = name;
    this.value = value;
  }
  
  public void setTcProperty(String name, String value){
    this.name = name;
    this.value = value;
  }
  
  @Override
  public String toString() {
    String out = "";
    
    out += indent() + "<property " + (this.name != null ? "name=\"" + this.name + "\"" : "") + (this.value != null ? " value=\"" + this.value + "\"" : "") + "/>";
    
    return out;
  }
}
