/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xml;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.ScrollBar;
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
//    updateState(getEditorInput());
//    validateState(getEditorInput());
//    performSave(true, progressMonitor);
  }

  public void doSaveWork(IProgressMonitor progressMonitor) {
    updateState(getEditorInput());
    validateState(getEditorInput());
    performSave(true, progressMonitor);
  }
  
  public StyledText getTextWidget() {
    return getSourceViewer().getTextWidget();
  }
  
  public int getTopIndex() {
    return getSourceViewer().getTopIndex();
  }
  
  public void setTopIndex(int index) {
    getSourceViewer().setTopIndex(index);
  }
  
  public Point getScrollLocation() {
    Point loc = new Point(0, 0);

    ScrollBar vertBar = getSourceViewer().getTextWidget().getVerticalBar();
    ScrollBar horzBar = getSourceViewer().getTextWidget().getHorizontalBar();
    
    if(vertBar != null) loc.y = vertBar.getSelection();
    if(horzBar != null) loc.x = horzBar.getSelection();

    return loc;
  }
  
  public void setScrollLocation(Point loc) {
    StyledText textWidget = getSourceViewer().getTextWidget();
    ScrollBar vertBar = textWidget.getVerticalBar();
    ScrollBar horzBar = textWidget.getHorizontalBar();
    
    if(vertBar != null) vertBar.setSelection(loc.y);
    if(horzBar != null) horzBar.setSelection(loc.x);
    
    textWidget.layout(false);
    textWidget.redraw();
  }
}
