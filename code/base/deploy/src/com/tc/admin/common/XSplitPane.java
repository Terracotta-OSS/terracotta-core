/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JSplitPane;

public class XSplitPane extends JSplitPane implements PropertyChangeListener {
  private Integer                dividerLocation;
  private double                 defaultDividerLocation   = DEFAULT_DIVIDER_LOCATION;
  private Preferences            prefs;
  private static final double    DEFAULT_DIVIDER_LOCATION = 0.7;
  private static final String    SPLIT_PREF_KEY           = "Split";
  private static final Dimension CHILD_MIN_SIZE           = new Dimension();

  public XSplitPane() {
    super();
  }

  public XSplitPane(int orientation) {
    super(orientation);
    setResizeWeight(0.5);
    super.setDividerLocation(defaultDividerLocation);
  }

  public XSplitPane(int orientation, Component leftComponent, Component rightComponent) {
    super(orientation, leftComponent, rightComponent);
    setResizeWeight(0.5);
    super.setDividerLocation(defaultDividerLocation);
  }

  public void setPreferences(Preferences prefs) {
    this.prefs = prefs;
    dividerLocation = Integer.valueOf(prefs != null ? prefs.getInt(SPLIT_PREF_KEY, -1) : -1);
  }

  public void setDefaultDividerLocation(double defaultDividerLocation) {
    this.defaultDividerLocation = defaultDividerLocation;
  }

  @Override
  public void setDividerLocation(int dividerLocation) {
    this.dividerLocation = dividerLocation;
    super.setDividerLocation(dividerLocation);
  }

  public void propertyChange(PropertyChangeEvent pce) {
    String propName = pce.getPropertyName();

    if (isShowing() && JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(propName)) {
      int divLoc = getDividerLocation();
      if (prefs != null) {
        prefs.putInt(SPLIT_PREF_KEY, divLoc);
        try {
          prefs.flush();
        } catch (BackingStoreException bse) {
          /**/
        }
      }
      dividerLocation = Integer.valueOf(divLoc);
    }
  }

  @Override
  public void add(java.awt.Component comp, Object constraints) {
    super.add(comp, constraints);
    comp.setMinimumSize(CHILD_MIN_SIZE);
  }

  @Override
  public void addNotify() {
    super.addNotify();
    addPropertyChangeListener(this);
  }

  @Override
  public void removeNotify() {
    removePropertyChangeListener(this);
    super.removeNotify();
  }

  @Override
  public void doLayout() {
    if (dividerLocation != null && dividerLocation.intValue() >= 0) {
      // this one takes an absolute integer value
      setDividerLocation(dividerLocation.intValue());
    } else {
      setDividerLocation(defaultDividerLocation);
    }
    super.doLayout();
  }
}
