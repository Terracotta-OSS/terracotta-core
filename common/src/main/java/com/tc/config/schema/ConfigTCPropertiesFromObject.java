/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.terracotta.config.Property;
import org.terracotta.config.TcProperties;

import com.tc.config.TcProperty;


public class ConfigTCPropertiesFromObject implements ConfigTCProperties {
  private final TcProperty[] tcProperties;

  public ConfigTCPropertiesFromObject(TcProperties tcProps) {
    if (tcProps == null) {
      tcProperties = new TcProperty[0];
      return;
    }
    
    Property[] props = tcProps.getProperty().toArray(new Property[] {});
    int len = props.length;
    tcProperties = new TcProperty[len];
    
    for (int i = 0; i < len; i++) {
      tcProperties[i] = new TcProperty(props[i].getName(), props[i].getValue());
    }
  }

  @Override
  public TcProperty[] getTcPropertiesArray() {
    return tcProperties;
  }

}
