/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xml;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IGotoMarker;

public class XMLEditor extends TextEditor implements IGotoMarker {

  private ColorManager colorManager;

  public XMLEditor() {
    super();

    colorManager = new ColorManager();
    setSourceViewerConfiguration(new XMLConfiguration(colorManager));
    setDocumentProvider(new XMLDocumentProvider());
  }
  
  public IDocument getDocument() {
    return getDocumentProvider().getDocument(getEditorInput());
  }

  public void addTextInputListener(ITextInputListener listener) {
    getSourceViewer().addTextInputListener(listener);
  }
  
  public void removeTextInputListener(ITextInputListener listener) {
    getSourceViewer().removeTextInputListener(listener);
  }
  
  public void dispose() {
    colorManager.dispose();
    super.dispose();
  }
  
  public void doSave(IProgressMonitor progressMonitor) {
    updateState(getEditorInput());
    validateState(getEditorInput());
    performSave(true, progressMonitor);
  }
}
