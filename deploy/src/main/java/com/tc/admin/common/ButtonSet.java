package com.tc.admin.common;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

public class ButtonSet extends XContainer {
  private final ButtonGroup buttonGroup;
  private JRadioButton      nullSelection;

  public ButtonSet() {
    super();
    buttonGroup = new ButtonGroup();
    buttonGroup.add(nullSelection = new JRadioButton());
  }

  public ButtonSet(LayoutManager layout) {
    this();
    setLayout(layout);
  }

  public void setSelectedIndex(int index) {
    buttonGroup.setSelected(_model(index), true);
  }

  public int getSelectedIndex() {
    int count = getComponentCount();
    for (int i = 0; i < count; i++) {
      if (buttonGroup.isSelected(_model(i))) { return i; }
    }
    return -1;
  }

  public void setSelected(String name) {
    if (name == null) { return; }
    for (Component comp : getComponents()) {
      if (name.equals(comp.getName())) {
        ((JToggleButton) comp).setSelected(true);
        return;
      }
    }
  }

  public String getSelected() {
    int i = getSelectedIndex();
    if (i != -1) { return getComponent(i).getName(); }
    return null;
  }

  private ButtonModel _model(int index) {
    int count = getComponentCount();
    if (index < count && index > -1) { return ((AbstractButton) getComponent(index)).getModel(); }
    return nullSelection.getModel();
  }

  @Override
  protected void addImpl(java.awt.Component comp, Object constraints, int index) {
    if (comp == null) { return; }
    if (!JToggleButton.class.isAssignableFrom(comp.getClass())) { throw new IllegalArgumentException(
                                                                                                     "Must extend JToggleButton"); }
    super.addImpl(comp, constraints, index);

    AbstractButton button = (AbstractButton) comp;
    buttonGroup.add(button);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        fireActionPerformed(ae);
      }
    });
  }

  @Override
  public void remove(int index) {
    buttonGroup.remove((AbstractButton) getComponent(index));
    super.remove(index);
  }

  public void addActionListener(ActionListener l) {
    listenerList.add(ActionListener.class, l);
  }

  public void removeActionListener(ActionListener l) {
    listenerList.remove(ActionListener.class, l);
  }

  public ActionListener[] getActionListeners() {
    return listenerList.getListeners(ActionListener.class);
  }

  public String getActionCommand() {
    return "SelectionChanged";
  }

  protected void fireActionPerformed(ActionEvent event) {
    Object[] listeners = listenerList.getListenerList();
    ActionEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ActionListener.class) {
        if (e == null) {
          String actionCmd;
          if ((actionCmd = event.getActionCommand()) == null) {
            actionCmd = getActionCommand();
          }
          e = new ActionEvent(ButtonSet.this, ActionEvent.ACTION_PERFORMED, actionCmd, event.getWhen(),
                              event.getModifiers());
        }
        ((ActionListener) listeners[i + 1]).actionPerformed(e);
      }
    }
  }
}
