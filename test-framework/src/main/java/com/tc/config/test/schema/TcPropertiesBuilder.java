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

public class TcPropertiesBuilder extends BaseConfigBuilder{
  
  private TcPropertyBuilder[] tcProps;

  public TcPropertiesBuilder() {
    super(1, new String[] {"property"});
  }

  public void setTcProperties(TcPropertyBuilder[] tcProps){
    this.tcProps = tcProps;
    setProperty("property", tcProps);
  }

  public TcPropertyBuilder[] getTcProertiess() {
    return tcProps;
  }
  
  @Override
  public String toString(){
    String out = "";
    
    if(isSet("property")){
      out += indent() ;
      
      int len = tcProps.length;
      for(int i = 0; i < len; i++) {
        out += tcProps[i].toString();
      }
      out += "\n\n";
    }
    return out;
  }
}
