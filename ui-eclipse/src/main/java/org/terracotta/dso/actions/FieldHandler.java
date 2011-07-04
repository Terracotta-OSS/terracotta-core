/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;

public class FieldHandler extends BaseMenuCreator {
  private RootFieldAction      m_rootAction;
  private TransientFieldAction m_transientAction;

  public FieldHandler() {
    super();
    
    m_rootAction      = new RootFieldAction();
    m_transientAction = new TransientFieldAction();
  }
  
  protected IJavaElement getJavaElement(ISelection selection) {
    IField field = null;
    String label = "Fields";
    
    if((field = ActionUtil.findSelectedField(selection)) != null) {    
     label = "Field " + field.getElementName();
    }
    m_delegateAction.setText(label);
    
    return field;
  }
  
  protected void fillMenu(Menu menu) {
    if(m_element != null) {
      m_rootAction.setField((IField)m_element);
      addMenuAction(menu, m_rootAction);

      m_transientAction.setField((IField)m_element);
      addMenuAction(menu, m_transientAction);
    }
  }  
}
