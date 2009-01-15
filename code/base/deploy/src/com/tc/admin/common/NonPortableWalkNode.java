/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.object.appevent.NonPortableObjectState;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

public class NonPortableWalkNode extends XTreeNode {
  private NonPortableObjectState objectState;

  private static final ImageIcon NOT_PORTABLE_ICON     = new ImageIcon(NonPortableWalkNode.class
                                                           .getResource("/com/tc/admin/icons/field_protected_obj.gif"));
  private static final ImageIcon NEVER_PORTABLE_ICON   = new ImageIcon(NonPortableWalkNode.class
                                                           .getResource("/com/tc/admin/icons/field_private_obj.gif"));
  private static final ImageIcon TRANSIENT_ICON        = new ImageIcon(NonPortableWalkNode.class
                                                           .getResource("/com/tc/admin/icons/field_public_obj.gif"));
  private static final ImageIcon PORTABLE_ICON         = new ImageIcon(NonPortableWalkNode.class
                                                           .getResource("/com/tc/admin/icons/field_default_obj.gif"));
  private static final ImageIcon PRE_INSTRUMENTED_ICON = new ImageIcon(NonPortableWalkNode.class
                                                           .getResource("/com/tc/admin/icons/owned_ovr.gif"));

  public NonPortableWalkNode(Object userObject) {
    super();
    setUserObject(userObject);
    if (userObject instanceof NonPortableObjectState) {
      setObjectState((NonPortableObjectState) userObject);
    }
  }

  void setObjectState(NonPortableObjectState objectState) {
    if ((this.objectState = objectState) != null) {
      ImageIcon icon = PORTABLE_ICON;

      if (isPreInstrumented()) {
        icon = PRE_INSTRUMENTED_ICON;
      } else if (isNeverPortable()) {
        icon = NEVER_PORTABLE_ICON;
      } else if (!isPortable()) {
        icon = NOT_PORTABLE_ICON;
      } else if (isTransient()) {
        icon = TRANSIENT_ICON;
      }

      setIcon(icon);
    }
  }

  public NonPortableObjectState getObjectState() {
    return this.objectState;
  }

  public String getLabel() {
    return this.objectState != null ? this.objectState.getLabel() : null;
  }

  public String getFieldName() {
    return this.objectState != null ? this.objectState.getFieldName() : null;
  }

  public String getTypeName() {
    return this.objectState != null ? this.objectState.getTypeName() : null;
  }

  public boolean isPortable() {
    return this.objectState != null ? this.objectState.isPortable() : false;
  }

  public boolean isTransient() {
    return this.objectState != null ? this.objectState.isTransient() : false;
  }

  public boolean isNeverPortable() {
    return this.objectState != null ? this.objectState.isNeverPortable() : false;
  }

  public boolean isPreInstrumented() {
    return this.objectState != null ? this.objectState.isPreInstrumented() : false;
  }

  private static class MakeTransientAction extends XAbstractAction {
    public MakeTransientAction() {
      super("Make transient");
    }

    public void actionPerformed(ActionEvent e) {
      // TODO
    }
  }

  private static class IncludeForInstrumentationAction extends XAbstractAction {
    public IncludeForInstrumentationAction() {
      super("Include from instrumentation");
    }

    public void actionPerformed(ActionEvent e) {
      // TODO
    }
  }

  private static class ExcludeForInstrumentationAction extends XAbstractAction {
    public ExcludeForInstrumentationAction() {
      super("Exclude from instrumentation");
    }

    public void actionPerformed(ActionEvent e) {
      // TODO
    }
  }

  private static class MakeBootClassAction extends XAbstractAction {
    public MakeBootClassAction() {
      super("Add to BootJar");
    }

    public void actionPerformed(ActionEvent e) {
      // TODO
    }
  }

  public JPopupMenu getPopupMenu() {
    JPopupMenu popupMenu = super.getPopupMenu();
    if (popupMenu == null) {
      popupMenu = new JPopupMenu();
      if (isNeverPortable()) {
        popupMenu.add(new MakeTransientAction());
      } else if (!isPortable()) {
        popupMenu.add(new IncludeForInstrumentationAction());
        popupMenu.add(new MakeBootClassAction());
        if (getFieldName() != null) {
          popupMenu.add(new MakeTransientAction());
        }
      } else {
        popupMenu.add(new ExcludeForInstrumentationAction());
      }
    }
    return popupMenu;
  }
}
