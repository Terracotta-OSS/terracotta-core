/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  public TcProperty[] getTcPropertiesArray() {
    return tcProperties;
  }

}
