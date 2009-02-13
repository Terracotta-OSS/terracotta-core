/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.TcProperty;
import com.terracottatech.config.TcConfigDocument.TcConfig.TcProperties;
import com.terracottatech.config.TcConfigDocument.TcConfig.TcProperties.Property;


public class ConfigTCPropertiesFromObject implements ConfigTCProperties {
  private TcProperty[] tcProperties = new TcProperty[0];

  public ConfigTCPropertiesFromObject(TcProperties tcProps) {
    if(tcProps == null)
      return;
    
    Property[] props = tcProps.getPropertyArray();
    int len = props.length;
    tcProperties = new TcProperty[len];
    
    for(int i = 0; i < len; i++){
      tcProperties[i] = new TcProperty(props[i].getName().getStringValue(), props[i].getValue().getStringValue());
    }
  }

  public TcProperty[] getTcPropertiesArray() {
    return tcProperties;
  }

}
