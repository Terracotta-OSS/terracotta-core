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
package com.tc.config.schema;

import com.tc.config.TcProperty;
import com.terracottatech.config.Property;
import com.terracottatech.config.TcProperties;


public class ConfigTCPropertiesFromObject implements ConfigTCProperties {
  private TcProperty[] tcProperties = new TcProperty[0];

  public ConfigTCPropertiesFromObject(TcProperties tcProps) {
    if(tcProps == null)
      return;
    
    Property[] props = tcProps.getPropertyArray();
    int len = props.length;
    tcProperties = new TcProperty[len];
    
    for(int i = 0; i < len; i++){
      tcProperties[i] = new TcProperty(props[i].getName(), props[i].getValue());
    }
  }

  @Override
  public TcProperty[] getTcPropertiesArray() {
    return tcProperties;
  }

}
