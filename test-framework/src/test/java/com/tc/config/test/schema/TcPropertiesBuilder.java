/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
