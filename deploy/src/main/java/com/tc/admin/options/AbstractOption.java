package com.tc.admin.options;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;

import java.awt.Component;
import java.util.prefs.Preferences;

import javax.swing.Icon;

public abstract class AbstractOption implements IOption {
  ApplicationContext appContext;
  String             name;
  Preferences        prefs;
  XContainer         display;

  protected AbstractOption(ApplicationContext appContext, String name) {
    this.appContext = appContext;
    this.name = name;
    this.prefs = appContext.getPrefs().node(name);
  }

  public String getName() {
    return name;
  }

  public abstract String getLabel();

  public abstract Icon getIcon();

  public abstract Component getDisplay();

  public abstract void apply();

  protected int getIntPref(String key, int defaultValue) {
    return prefs.getInt(key, defaultValue);
  }

  protected void putIntPref(String key, int value) {
    int oldValue = getIntPref(key, -1);
    if (oldValue != value) {
      prefs.putInt(key, value);
      appContext.storePrefs();
      try {
        prefs.flush();
      } catch (Exception e) {/**/
      }
    }
  }
}
