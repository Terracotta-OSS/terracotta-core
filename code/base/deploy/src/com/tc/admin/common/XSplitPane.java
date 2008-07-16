/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.Component;
import org.dijon.SplitPane;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JSplitPane;

public class XSplitPane extends SplitPane implements PropertyChangeListener {
  private Integer                m_dividerLocation;
  private Preferences            m_prefs;
  private static final double    DEFAULT_DIVIDER_LOCATION = 0.7;
  private static final String    SPLIT_PREF_KEY           = "Split";
  private static final Dimension CHILD_MIN_SIZE           = new Dimension();

  public XSplitPane() {
    super();
  }

  public XSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
    super(newOrientation, newLeftComponent, newRightComponent);
  }

  public void setPreferences(Preferences prefs) {
    m_prefs = prefs;
    m_dividerLocation = Integer.valueOf(prefs != null ? prefs.getInt(SPLIT_PREF_KEY, -1) : -1);
  }

  public void propertyChange(PropertyChangeEvent pce) {
    String propName = pce.getPropertyName();

    if (isShowing() && JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(propName)) {
      int divLoc = getDividerLocation();
      if (m_prefs != null) {
        m_prefs.putInt(SPLIT_PREF_KEY, divLoc);
        try {
          m_prefs.flush();
        } catch (BackingStoreException bse) {
          /**/
        }
      }
      m_dividerLocation = Integer.valueOf(divLoc);
    }
  }

  public void add(java.awt.Component comp, Object constraints) {
    super.add(comp, constraints);
    comp.setMinimumSize(CHILD_MIN_SIZE);
  }

  public void addNotify() {
    super.addNotify();
    addPropertyChangeListener(this);
  }

  public void removeNotify() {
    removePropertyChangeListener(this);
    super.removeNotify();
  }

  public void doLayout() {
    super.doLayout();
    if (m_dividerLocation != null) {
      // this one takes an absolute integer value
      setDividerLocation(m_dividerLocation.intValue());
    } else {
      // this one takes an relative double value
      setDividerLocation(DEFAULT_DIVIDER_LOCATION);
    }
  }
}
