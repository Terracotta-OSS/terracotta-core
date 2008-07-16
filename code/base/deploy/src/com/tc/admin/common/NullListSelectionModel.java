/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

public class NullListSelectionModel implements ListSelectionModel {
  public static final int NULL_SELECTION = 3;

  public int getAnchorSelectionIndex() {
    return -1;
  }

  public int getLeadSelectionIndex() {
    return -1;
  }

  public int getMaxSelectionIndex() {
    return -1;
  }

  public int getMinSelectionIndex() {
    return -1;
  }

  public int getSelectionMode() {
    return NULL_SELECTION;
  }

  public void clearSelection() {/**/
  }

  public boolean getValueIsAdjusting() {
    return false;
  }

  public boolean isSelectionEmpty() {
    return true;
  }

  public void setAnchorSelectionIndex(int index) {/**/
  }

  public void setLeadSelectionIndex(int index) {/**/
  }

  public void setSelectionMode(int selectionMode) {/**/
  }

  public boolean isSelectedIndex(int index) {
    return false;
  }

  public void addSelectionInterval(int index0, int index1) {/**/
  }

  public void removeIndexInterval(int index0, int index1) {/**/
  }

  public void removeSelectionInterval(int index0, int index1) {/**/
  }

  public void setSelectionInterval(int index0, int index1) {/**/
  }

  public void insertIndexInterval(int index, int length, boolean before) {/**/
  }

  public void setValueIsAdjusting(boolean valueIsAdjusting) {/**/
  }

  public void addListSelectionListener(ListSelectionListener x) {/**/
  }

  public void removeListSelectionListener(ListSelectionListener x) {/**/
  }
}
