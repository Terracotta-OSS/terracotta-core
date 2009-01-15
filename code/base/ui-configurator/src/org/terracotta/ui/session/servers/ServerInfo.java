/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session.servers;

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
  private String name;
  private String label;
  private String displayName;
  private String startupTrigger;
  private String applicationPath;
  private List   env;

  public ServerInfo(Properties props, Properties env) {
    this(props.getProperty("name"), props.getProperty("label"), props.getProperty("display.name"), props
        .getProperty("startup.trigger"), props.getProperty("application.path"));
    setProperties(env);
  }

  public ServerInfo(String name, String label, String displayName, String startupTrigger, String applicationPath) {
    this.name = name;
    this.label = label;
    this.displayName = displayName;
    this.startupTrigger = startupTrigger;
    this.applicationPath = applicationPath;
    env = new ArrayList();
  }

  public ServerInfo(ServerInfo info) {
    name = info.getName();
    label = info.getLabel();
    displayName = info.getDisplayName();
    startupTrigger = info.getStartupTrigger();
    applicationPath = info.getApplicationPath();
    env = info.cloneProperties();
  }

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getStartupTrigger() {
    return startupTrigger;
  }

  public String getApplicationPath() {
    return applicationPath;
  }

  public List getProperties() {
    return env;
  }

  public void setProperties(Properties env) {
    Enumeration props = env.propertyNames();
    String key;
    String value;

    while (props.hasMoreElements()) {
      key = (String) props.nextElement();
      value = env.getProperty(key);

      addProperty(key, value);
    }
  }

  public Properties toProperties() {
    Properties props = new Properties();
    int propCount = propertyCount();
    ServerProperty property;

    for (int i = 0; i < propCount; i++) {
      property = (ServerProperty) env.get(i);
      props.put(property.getName(), quote(property.getValue()));
    }

    return props;
  }

  public String[] toEnvironment() {
    ArrayList list = new ArrayList();
    Iterator iter = env.iterator();
    ServerProperty prop;

    while (iter.hasNext()) {
      prop = (ServerProperty) iter.next();
      list.add(prop.getName() + "=" + quote(prop.getValue()));
    }

    return (String[]) list.toArray(new String[list.size()]);
  }

  private String quote(final String value) {
    return value.matches(".*[\\s].*") ? "\"" + value + "\"" : value;
  }

  public void storeEnvironment(Preferences prefs) {
    int propCount = propertyCount();
    ServerProperty property;
    String value;

    for (int i = 0; i < propCount; i++) {
      property = (ServerProperty) env.get(i);
      if ((value = property.getValue()) != null) {
        prefs.put(property.getName(), value);
      }
    }
  }

  public void loadEnvironment(Preferences prefs) {
    String[] keys;

    try {
      keys = prefs.keys();
    } catch (BackingStoreException bse) {
      return;
    }

    String key;
    ServerProperty prop;

    for (int i = 0; i < keys.length; i++) {
      key = keys[i];
      prop = getProperty(key);

      if (prop != null) {
        addProperty(key, prefs.get(key, prop.getValue()));
      }
    }
  }

  private String getenv(String key) {
    try {
      Method m = System.class.getMethod("getenv", new Class[] { String.class });

      if (m != null) { return (String) m.invoke(null, new Object[] { key }); }
    } catch (Throwable t) {/**/
    }

    return null;
  }

  public void addProperty(String propName, String propValue) {
    ServerProperty prop = getProperty(propName);

    if (propValue == null || propValue.length() == 0) {
      propValue = getenv(propName);
    } else {
      Pattern pattern = Pattern.compile("\\$\\{(.*)\\}(.*)");
      String[] comps = propValue.split(",");

      for (int i = 0; i < comps.length; i++) {
        propValue = comps[i];

        if (propValue.indexOf('$') != -1) {
          Matcher matcher = pattern.matcher(propValue);

          if (matcher.matches()) {
            String var = matcher.group(1);
            if ((propValue = getenv(var)) == null) {
              propValue = System.getProperty(var);
            }

            if (propValue != null) {
              propValue = propValue + matcher.group(2);
              String fileSep = System.getProperty("file.separator");
              File file = new File(propValue = StringUtils.replace(propValue, "/", fileSep));

              if (file.exists()) {
                try {
                  propValue = file.getCanonicalPath();
                } catch (IOException ioe) {
                  propValue = file.getAbsolutePath();
                }
                break;
              }

            }
          }
        } else {
          break;
        }
      }
    }

    if (propValue == null) {
      propValue = "";
    }

    if (prop != null) {
      prop.setValue(propValue);
    } else {
      env.add(prop = new ServerProperty(propName, propValue));
    }
  }

  protected List cloneProperties() {
    ArrayList list = new ArrayList();
    int count = env.size();
    ServerProperty prop;

    for (int i = 0; i < count; i++) {
      prop = (ServerProperty) env.get(i);
      list.add(new ServerProperty(prop.getName(), prop.getValue()));
    }

    return list;
  }

  public ServerProperty getProperty(String propName) {
    int count = env.size();
    ServerProperty prop;

    for (int i = 0; i < count; i++) {
      prop = (ServerProperty) env.get(i);
      if (prop.getName().equals(propName)) { return prop; }
    }

    return null;
  }

  public void setProperty(String name, String value) {
    ServerProperty prop = getProperty(name);
    if (prop != null) {
      prop.setValue(value);
    }
  }

  public int propertyCount() {
    return env.size();
  }

  public String toString() {
    return getDisplayName();
  }

  public String[] validateProperties() {
    ArrayList list = new ArrayList();
    int count = env.size();
    ServerProperty prop;
    String value;

    for (int i = 0; i < count; i++) {
      prop = (ServerProperty) env.get(i);
      value = prop.getValue();

      if (value == null || value.trim().length() == 0) {
        list.add("Value for '" + prop.getName() + "' cannot be empty.");
      } else {
        File file = new File(value);

        if (!file.exists()) {
          list.add(prop.getValue() + " does not exist.");
        }
      }
    }

    return list.size() > 0 ? (String[]) list.toArray(new String[0]) : null;
  }
}
