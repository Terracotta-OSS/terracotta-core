/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import java.text.MessageFormat;

/**
 * Add the <code>StatusBarUpdater</code> to your ViewPart to have the statusbar describing the selected elements.
 */
public class ConfigViewStatusBarUpdater implements ISelectionChangedListener {

  private ConfigViewPart     fConfigViewPart;
  private IStatusLineManager fStatusLineManager;

  private final long         LABEL_FLAGS = JavaElementLabels.DEFAULT_QUALIFIED | JavaElementLabels.ROOT_POST_QUALIFIED
                                           | JavaElementLabels.APPEND_ROOT_PATH | JavaElementLabels.M_PARAMETER_TYPES
                                           | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_APP_RETURNTYPE
                                           | JavaElementLabels.M_EXCEPTIONS | JavaElementLabels.F_APP_TYPE_SIGNATURE
                                           | JavaElementLabels.T_TYPE_PARAMETERS;

  public ConfigViewStatusBarUpdater(ConfigViewPart configViewPart, IStatusLineManager statusLineManager) {
    fConfigViewPart = configViewPart;
    fStatusLineManager = statusLineManager;
  }

  /*
   * @see ISelectionChangedListener#selectionChanged
   */
  public void selectionChanged(SelectionChangedEvent event) {
    String statusBarMessage = formatMessage(event.getSelection());
    fStatusLineManager.setMessage(statusBarMessage);
  }

  protected String formatMessage(ISelection sel) {
    if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
      IStructuredSelection selection = (IStructuredSelection) sel;

      int nElements = selection.size();
      if (nElements > 1) {
        return MessageFormat.format("{0} items selected", String.valueOf(nElements));
      } else {
        Object elem = selection.getFirstElement();
        if (elem instanceof RootWrapper) {
          String rootField = ((RootWrapper) elem).getFieldName();
          IField field = fConfigViewPart.getField(rootField);
          if (field != null && field.exists()) {
            return formatJavaElement(field);
          } else {
            return MessageFormat.format("Root field {0} does not exist.", rootField);
          }
        } else if (elem instanceof RootsWrapper) {
          int rootCount = ((RootsWrapper) elem).sizeOfRootArray();
          String noun = rootCount == 1 ? "root" : "roots";
          return MessageFormat.format("{0} {1} declared", rootCount, noun);
        } else if (elem instanceof BootClassWrapper) {
          String bootClassName = ((BootClassWrapper) elem).getClassName();
          IType bootType = fConfigViewPart.getType(bootClassName);
          if (bootType != null) {
            return formatJavaElement(bootType);
          } else {
            return MessageFormat.format("Boot class {0} does not exist.", bootClassName);
          }
        } else if (elem instanceof TransientFieldWrapper) {
          String transientField = ((TransientFieldWrapper) elem).getFieldName();
          IField field = fConfigViewPart.getField(transientField);
          if (field != null && field.exists()) {
            return formatJavaElement(field);
          } else {
            return MessageFormat.format("Transient field {0} does not exist.", transientField);
          }
        } else if (elem instanceof TransientFieldsWrapper) {
          int transientFieldCount = ((TransientFieldsWrapper) elem).sizeOfFieldNameArray();
          String noun = transientFieldCount == 1 ? "field" : "fields";
          return MessageFormat.format("{0} transient {1} declared", transientFieldCount, noun); }
      }
    }
    return "";
  }

  private String formatJavaElement(IJavaElement element) {
    return JavaElementLabels.getElementLabel(element, LABEL_FLAGS);
  }
}
