/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.servers;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerInfo {
  private String m_name;
  private String m_label;
  private String m_displayName;
  private String m_startupTrigger;
  private String m_applicationPath;
  private List   m_env;

  public ServerInfo(Properties props, Properties env) {
    this(props.getProperty("name"),
         props.getProperty("label"),
         props.getProperty("display.name"),
         props.getProperty("startup.trigger"),
         props.getProperty("application.path"));
    setProperties(env);
  }
  
  public ServerInfo(String name,
                    String label,
                    String displayName,
                    String startupTrigger,
                    String applicationPath)
  {
    m_name            = name;
    m_label           = label;
    m_displayName     = displayName;
    m_startupTrigger  = startupTrigger;
    m_applicationPath = applicationPath;
    m_env             = new ArrayList();
  }

  public ServerInfo(ServerInfo info) {
    m_name            = info.getName();
    m_label           = info.getLabel();
    m_displayName     = info.getDisplayName();
    m_startupTrigger  = info.getStartupTrigger();
    m_applicationPath = info.getApplicationPath();
    m_env             = info.cloneProperties(); 
  }
  
  public String getName() {
    return m_name;
  }
  
  public String getLabel() {
    return m_label;
  }
  
  public String getDisplayName() {
    return m_displayName;
  }
  
  public String getStartupTrigger() {
    return m_startupTrigger;
  }
  
  public String getApplicationPath() {
    return m_applicationPath;
  }
  
  public List getProperties() {
    return m_env;
  }
  
  public void setProperties(Properties env) {
    Enumeration props = env.propertyNames();
    String      key;
    String      value;
    
    while(props.hasMoreElements()) {
      key   = (String)props.nextElement();
      value = env.getProperty(key);
      
      addProperty(key, value);
    }
  }
  
  public Properties toProperties() {
    Properties     props     = new Properties();
    int            propCount = propertyCount();
    ServerProperty property;
    
    for(int i = 0; i < propCount; i++) {
      property = (ServerProperty)m_env.get(i);
      props.put(property.getName(), property.getValue());
    }
    
    return props;
  }
  
  public String[] toEnvironment() {
    ArrayList      list = new ArrayList();
    Iterator       iter = m_env.iterator();
    ServerProperty prop;
    
    while(iter.hasNext()) {
      prop = (ServerProperty)iter.next();
      list.add(prop.getName()+"="+prop.getValue());
    }
    
    return (String[])list.toArray(new String[list.size()]);
  }
  
  public void storeEnvironment(Preferences prefs) {
    int            propCount = propertyCount();
    ServerProperty property;
    String         value;
    
    for(int i = 0; i < propCount; i++) {
      property = (ServerProperty)m_env.get(i);
      if((value = property.getValue()) != null) {
        prefs.put(property.getName(), value);
      }
    }
  }
  
  public void loadEnvironment(Preferences prefs) {
    String[] keys;
    
    try {
      keys = prefs.keys();
    } catch(BackingStoreException bse) {
      return;
    }
    
    String         key;
    ServerProperty prop;
     
    for(int i = 0; i < keys.length; i++) {
      key  = keys[i];
      prop = getProperty(key);
      
      if(prop != null) {
        addProperty(key, prefs.get(key, prop.getValue()));
      }
    }
  }
  
  private String getenv(String key) {
    try {
      Method m = System.class.getMethod("getenv", new Class[] {String.class});
    
      if(m != null) {
        return (String)m.invoke(null, new Object[]{key});
      }
    } catch(Throwable t) {/**/}

    return null;
  }
  
  public void addProperty(String name, String value) {
    ServerProperty prop = getProperty(name);
    
    if(value == null || value.length() == 0) {
      value = getenv(name);
    }
    else {
      Pattern  pattern = Pattern.compile("\\$\\{(.*)\\}(.*)");
      String[] comps   = value.split(",");
      
      for(int i = 0; i < comps.length; i++) {
        value = comps[i];
        
        if(value.indexOf('$') != -1) {
          Matcher matcher = pattern.matcher(value);
          
          if(matcher.matches()) {
            String var = matcher.group(1);
            
            if((value = getenv(var)) == null) {
              value = System.getProperty(var);
            }
            
            if(value != null) {
              value = value+matcher.group(2);
              break;
            }
          }
        }
        else {
          break;
        }
      }
    }
    
    if(value != null) {
      value = StringUtils.replace(value, "/", System.getProperty("file.separator"));
      File file = new File(value);
      
      try {
        value = file.getCanonicalPath();
      } catch(IOException ioe) {
        value = file.getAbsolutePath();
      }
    }
    
    if(value == null) {
      value = "";
    }
    
    if(prop != null) {
      prop.setValue(value);
    }
    else {
      m_env.add(prop = new ServerProperty(name, value));
    }
  }

  protected List cloneProperties() {
    ArrayList      list  = new ArrayList();
    int            count = m_env.size();
    ServerProperty prop;
    
    for(int i = 0; i < count; i++) {
      prop = (ServerProperty)m_env.get(i);
      list.add(new ServerProperty(prop.getName(), prop.getValue()));
    }
    
    return list;
  }

  public ServerProperty getProperty(String name) {
    int            count = m_env.size();
    ServerProperty prop;
    
    for(int i = 0; i < count; i++) {
      prop = (ServerProperty)m_env.get(i);
      if(prop.getName().equals(name)) {
        return prop;
      }
    }
    
    return null;
  }
  
  public void setProperty(String name, String value) {
    ServerProperty prop = getProperty(name);
    
    if(prop != null) {
      prop.setValue(value);
    }
  }
  
  public int propertyCount() {
    return m_env.size();
  }
  
  public String toString() {
    return getDisplayName();
  }
  
  public String[] validateProperties() {
    ArrayList      list  = new ArrayList();
    int            count = m_env.size();
    ServerProperty prop;
    String         value;
    
    for(int i = 0; i < count; i++) {
      prop  = (ServerProperty)m_env.get(i);
      value = prop.getValue();
      
      if(value == null || value.trim().length() == 0) {
        list.add("Value for '"+prop.getName()+"' cannot be empty.");
      }
      else {
        File file = new File(value);

        if(!file.exists()) {
          list.add(prop.getValue()+" does not exist.");
        }
      }
    }

    return list.size() > 0 ? (String[])list.toArray(new String[0]) : null;
  }
}
