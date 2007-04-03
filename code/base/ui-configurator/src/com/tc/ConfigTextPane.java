/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTextPane;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class ConfigTextPane extends XTextPane {
  private static final int SHORTCUT_KEY_MASK =
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  private SaveAction m_saveAction;
  private UndoAction m_undoAction;
  private RedoAction m_redoAction;
  
  private static final String SAVE_CMD = "Save";
  private static final String UNDO_CMD = "Undo";
  private static final String REDO_CMD = "Redo";
  
  private static final KeyStroke SAVE_STROKE =
    KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY_MASK, false);
  private static final KeyStroke UNDO_STROKE =
    KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK, false);
  private static final KeyStroke REDO_STROKE =
    KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK|InputEvent.SHIFT_MASK, false);
  
  private ConfigTextListener m_configTextListener;
  private SimpleAttributeSet m_errorAttrSet;
  private Timer              m_parseTimer;
  private MyUndoManager      m_undoManager;
  
  public ConfigTextPane() {
    super();

    m_errorAttrSet = new SimpleAttributeSet();
    StyleConstants.setForeground(m_errorAttrSet, Color.red);
    
    m_undoManager = new MyUndoManager();

    JPopupMenu popup = getPopupMenu();
    popup.add(new JSeparator());
    popup.add(m_undoAction = new UndoAction());
    popup.add(m_redoAction = new RedoAction());
    popup.add(new JSeparator());
    popup.add(m_saveAction = new SaveAction());
    popup.add(new SaveAsAction());
    
    getActionMap().put(SAVE_CMD, m_saveAction);
    getActionMap().put(UNDO_CMD, m_undoAction);
    getActionMap().put(REDO_CMD, m_redoAction);
    
    getInputMap().put(SAVE_STROKE, SAVE_CMD);
    getInputMap().put(UNDO_STROKE, UNDO_CMD);
    getInputMap().put(REDO_STROKE, REDO_CMD);
    
    m_parseTimer = new Timer(2000, new ParseTimerAction());
    m_parseTimer.setRepeats(false);
    
    m_configTextListener = new ConfigTextListener();
  }

  private void removeListeners() {
    m_parseTimer.stop();
    getDocument().removeDocumentListener(m_configTextListener);
    removeUndoableEditListener();
  }

  private void addListeners() {
    getDocument().addDocumentListener(m_configTextListener);
    addUndoableEditListener();
  }
  
  public void load(String filename) {
    FileInputStream fis = null;
    
    removeListeners();
    try {
      fis = new FileInputStream(new File(filename));
      setContent(IOUtils.toString(fis));
      hasErrors();
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(fis);
    }
    addListeners();
  }

  public void set(String text) {
    removeListeners();
    try {
      setContent(text);
      hasErrors();
    } catch(Exception e) {
      e.printStackTrace();
    }
    addListeners();
  }

  class ConfigTextListener implements DocumentListener {
    public void insertUpdate(DocumentEvent e)  {
      m_saveAction.setEnabled(true);
      m_parseTimer.stop();
      m_parseTimer.start();
    }

    public void removeUpdate(DocumentEvent e)  {
      m_saveAction.setEnabled(true);
      m_parseTimer.stop();
      m_parseTimer.start();
    }

    public void changedUpdate(DocumentEvent e) {/**/}
  }
  
  class ParseTimerAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      checkForErrors();
    }
  }

  private void checkForErrors() {
    setEditable(false);
    removeListeners();
    hasErrors();
    addListeners();
    setEditable(true);
  }
  
  class SaveAction extends XAbstractAction {
    SaveAction() {
      super("Save");
      setAccelerator(SAVE_STROKE);
      String uri = "/com/tc/admin/icons/save_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      save();
    }
  }
  
  class SaveAsAction extends XAbstractAction {
    SaveAsAction() {
      super("Save As...");
      String uri = "/com/tc/admin/icons/saveas_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      saveAs();
    }
  }

  public boolean hasErrors() {
    boolean hasErrors = false;
    
    clearAllStyles();
    
    try {
      TextLineInfo lineInfo = getLineInfo();

      try {
        ConfigHelper configHelper = getMainFrame().getConfigHelper();
        List         errors       = configHelper.validate(getText());

        hasErrors = errors.size() > 0;
        handleErrors(errors, lineInfo);
      } catch(XmlException e) {
        hasErrors = true;
        
        Collection c = e.getErrors();
        ArrayList  errorList = new ArrayList();
        
        if(c != null) {
          errorList.addAll(c);
          handleErrors(new ArrayList(c), lineInfo);
        } else {
          errorList.add(e);
          getMainFrame().setConfigErrors(errorList);
        }
      }
    } catch(Exception e) {e.printStackTrace();}
    
    return hasErrors;
  }
  
  private void clearAllStyles() {
    StyledDocument doc = (StyledDocument)getDocument();
    doc.setCharacterAttributes(0, doc.getLength(), SimpleAttributeSet.EMPTY, true);
  }
  
  private void handleErrors(List errorList, TextLineInfo lineInfo) {
    StyledDocument doc    = (StyledDocument)getDocument();
    Iterator       errors = errorList.iterator();
    XmlError       error;
    
    while(errors.hasNext()) {
      error = (XmlError)errors.next();

      int line  = error.getLine();
      int col   = error.getColumn();
      int start = lineInfo.offset(line-1) + col-1;
      int len   = getElementLength(start);

      doc.setCharacterAttributes(start, len, m_errorAttrSet, true);
    }
    
    getMainFrame().setConfigErrors(errorList);
  }
  
  private int getElementLength(int start) {
    StyledDocument doc = (StyledDocument)getDocument();

    try {
      String text     = doc.getText(start, doc.getLength()-start);
      int    nameEnd  = text.indexOf('>');
      String name     = text.substring(1, nameEnd);
      String endTok   = "</"+name+">";
      int    endIndex = text.indexOf(endTok);
      
      if(endIndex != -1) {
        return endIndex + endTok.length();
      }
      else {
        return nameEnd+1;
      }
    } catch(Exception e ) {
      return 1;
    }
  }
  
  private void addUndoableEditListener() {
    ((DefaultStyledDocument)getDocument()).addUndoableEditListener(m_undoManager);
  }
  
  private void removeUndoableEditListener() {
    ((DefaultStyledDocument)getDocument()).removeUndoableEditListener(m_undoManager);
  }
  
  private TextLineInfo getLineInfo() {
    try {
      return new TextLineInfo(new StringReader(getText()));
    } catch(Exception e) {
      return new TextLineInfo();
    }
  }
  
  void selectError(XmlError error) {
    TextLineInfo lineInfo = getLineInfo();
    int          line     = error.getLine();
    int          col      = error.getColumn();
    int          start    = lineInfo.offset(line-1) + col-1;
    int          len      = getElementLength(start);
    
    setCaretPosition(start);
    moveCaretPosition(start+len);
    
    requestFocusInWindow();
  }
  
  private SessionIntegratorFrame getMainFrame() {
    return (SessionIntegratorFrame)getAncestorOfClass(SessionIntegratorFrame.class);
  }
  
  private void save() {
    SessionIntegratorFrame frame = getMainFrame();
    
    removeListeners();
    if(hasErrors()) {
      String msg    = "There are configuration errors.  Save anyway?";
      String title  = frame.getTitle();
      int    type   = JOptionPane.YES_NO_OPTION;
      int    answer = JOptionPane.showConfirmDialog(frame, msg, title, type);
      
      if(answer == JOptionPane.YES_OPTION) {
        frame.saveXML(getContent());
      }
    }
    else {
      frame.saveXML(getContent());
    }
    addListeners();
    m_undoManager.discardAllEdits();
    m_saveAction.setEnabled(false);
    m_undoAction.setEnabled(false);
    m_redoAction.setEnabled(false);
  }
  
  private void saveAs() {
    SessionIntegratorFrame frame = getMainFrame();
    
    removeListeners();
    if(hasErrors()) {
      String msg    = "There are configuration errors.  Save anyway?";
      String title  = frame.getTitle();
      int    type   = JOptionPane.YES_NO_OPTION;
      int    answer = JOptionPane.showConfirmDialog(frame, msg, title, type);
      
      if(answer == JOptionPane.YES_OPTION) {
        frame.exportConfiguration();
      }
    }
    else {
      frame.exportConfiguration();
    }
    addListeners();
  }
  
  class MyUndoManager extends UndoManager {
    public UndoableEdit nextUndoable() {
      return editToBeUndone();  
    }

    public UndoableEdit nextRedoable() {
      return editToBeRedone();  
    }
    
    public void undoableEditHappened(UndoableEditEvent e) {
      super.undoableEditHappened(e);
      m_undoAction.setEnabled(canUndo());
      m_redoAction.setEnabled(canRedo());
    }
  }
  
  class UndoAction extends XAbstractAction {
    UndoAction() {
      super("Undo");
      setAccelerator(UNDO_STROKE);
      String uri = "/com/tc/admin/icons/undo_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
      setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent ae) {
      UndoableEdit next = m_undoManager.nextUndoable();

      if(next != null) {
        m_undoManager.undo();

        setEnabled(m_undoManager.canUndo());
        m_redoAction.setEnabled(m_undoManager.canRedo());
      }
    }
  }

  class RedoAction extends XAbstractAction {
    RedoAction() {
      super("Redo");
      setAccelerator(REDO_STROKE);
      String uri = "/com/tc/admin/icons/redo_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      UndoableEdit next = m_undoManager.nextRedoable();

      if(next != null) {
        m_undoManager.redo();
        setEnabled(m_undoManager.canRedo());
        m_undoAction.setEnabled(m_undoManager.canUndo());
      }
    }
  }
  
  Action getSaveAction() { return m_saveAction; }
  Action getUndoAction() { return m_undoAction; }
  Action getRedoAction() { return m_redoAction; }
  Action getCutAction()  { return m_helper.getCutAction(); }
  Action getCopyAction() { return m_helper.getCopyAction(); }
  Action getPasteAction() { return m_helper.getPasteAction(); }
}
