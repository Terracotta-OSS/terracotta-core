/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlError;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTextPane;

import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class ConfigTextPane extends XTextPane {
  private static final int       SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  private SaveAction             m_saveAction;
  private UndoAction             m_undoAction;
  private RedoAction             m_redoAction;

  private static final String    SAVE_CMD          = "Save";
  private static final String    UNDO_CMD          = "Undo";
  private static final String    REDO_CMD          = "Redo";

  private static final KeyStroke SAVE_STROKE       = KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY_MASK, false);
  private static final KeyStroke UNDO_STROKE       = KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK, false);
  private static final KeyStroke REDO_STROKE       = KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK
                                                                                           | InputEvent.SHIFT_MASK,
                                                                            false);

  private ConfigTextListener     m_configTextListener;
  private SimpleAttributeSet     m_errorAttrSet;
  private Timer                  m_parseTimer;
  private MyUndoManager          m_undoManager;
  private JViewport              m_viewPort;

  public ConfigTextPane() {
    super();

    m_errorAttrSet = new SimpleAttributeSet();
    StyleConstants.setForeground(m_errorAttrSet, Color.red);

    m_undoManager = new MyUndoManager();

    m_undoAction = new UndoAction();
    m_redoAction = new RedoAction();
    m_saveAction = new SaveAction();

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

  public void addNotify() {
    super.addNotify();
    m_viewPort = (JViewport) getAncestorOfClass(JViewport.class);
  }

  protected JPopupMenu createPopup() {
    JPopupMenu popupMenu = super.createPopup();

    popupMenu.add(new JSeparator());
    popupMenu.add(m_undoAction);
    popupMenu.add(m_redoAction);
    popupMenu.add(new JSeparator());
    popupMenu.add(m_saveAction);
    popupMenu.add(new SaveAsAction());

    return popupMenu;
  }

  private void removeDocumentListener() {
    m_parseTimer.stop();
    getDocument().removeDocumentListener(m_configTextListener);
  }

  private void removeAllListeners() {
    removeDocumentListener();
    removeUndoableEditListener();
  }

  private void addDocumentListener() {
    getDocument().addDocumentListener(m_configTextListener);
  }

  private void addAllListeners() {
    addDocumentListener();
    addUndoableEditListener();
  }

  public void load(String filename) {
    FileReader reader = null;

    removeAllListeners();
    try {
      reader = new FileReader(new File(filename));
      setText(IOUtils.toString(reader));
      hasErrors();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(reader);
    }
    addAllListeners();
  }

  class UndoableSetAction extends AbstractUndoableEdit {
    private final String  m_origText;
    private final String  m_newText;
    private final boolean m_isModified;

    UndoableSetAction(String origText, String newText, boolean isModified) {
      m_origText = origText;
      m_newText = newText;
      m_isModified = isModified;
    }

    private void update(String text, boolean isModified) {
      set(text, false);
      handleContentChange();
      m_saveAction.setEnabled(isModified);
      getMainFrame().setXmlModified(isModified);
    }

    public void undo() throws CannotUndoException {
      super.undo();
      update(m_origText, m_isModified);
    }

    public void redo() throws CannotRedoException {
      super.redo();
      update(m_newText, true);
    }
  }

  private UndoableEditEvent createUndoableSetEditEvent(String newText) {
    return new UndoableEditEvent(this, new UndoableSetAction(getText(), newText, m_saveAction.isEnabled()));
  }

  public void set(String text) {
    set(text, true);
  }

  public void set(String text, boolean createUndoEvent) {
    String curText = getText();
    if (curText != null && curText.equals(text)) return;

    Point viewPosition = null;
    if (m_viewPort != null) {
      viewPosition = m_viewPort.getViewPosition();
    }
    removeAllListeners();
    if (createUndoEvent) {
      m_undoManager.undoableEditHappened(createUndoableSetEditEvent(text));
    }
    setText(text);
    hasErrors();
    m_saveAction.setEnabled(true);
    addAllListeners();
    if (viewPosition != null) {
      final Point viewPos = viewPosition;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          m_viewPort.setViewPosition(viewPos);
        }
      });
    }
  }

  private void restartParseTimer() {
    m_parseTimer.stop();
    m_parseTimer.start();
  }

  class ConfigTextListener implements DocumentListener {
    public void insertUpdate(DocumentEvent e) {
      m_saveAction.setEnabled(true);
      restartParseTimer();
    }

    public void removeUpdate(DocumentEvent e) {
      m_saveAction.setEnabled(true);
      restartParseTimer();
    }

    public void changedUpdate(DocumentEvent e) {/**/
    }
  }

  private void handleContentChange() {
    boolean errors = checkForErrors();
    if (!errors) {
      // getMainFrame().modelChanged();
    }
  }

  class ParseTimerAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      handleContentChange();
    }
  }

  private boolean checkForErrors() {
    setEditable(false);
    removeAllListeners();
    boolean errors = hasErrors();
    addAllListeners();
    setEditable(true);
    return errors;
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
    clearAllStyles();
    TextLineInfo lineInfo = getLineInfo();
    ConfigHelper configHelper = getMainFrame().getConfigHelper();
    List errors = configHelper.validate(getText());
    handleErrors(errors, lineInfo);
    return errors.size() > 0;
  }

  private void clearAllStyles() {
    StyledDocument doc = (StyledDocument) getDocument();
    doc.setCharacterAttributes(0, doc.getLength(), SimpleAttributeSet.EMPTY, true);
  }

  private void handleErrors(List errorList, TextLineInfo lineInfo) {
    StyledDocument doc = (StyledDocument) getDocument();
    Iterator errors = errorList.iterator();
    XmlError error;

    while (errors.hasNext()) {
      error = (XmlError) errors.next();

      int line = error.getLine();
      int col = error.getColumn();
      int start = lineInfo.offset(line - 1) + col - 1;
      int len = getElementLength(start);

      doc.setCharacterAttributes(start, len, m_errorAttrSet, true);
    }

    getMainFrame().setConfigErrors(errorList);
  }

  private int getElementLength(int start) {
    StyledDocument doc = (StyledDocument) getDocument();

    try {
      String text = doc.getText(start, doc.getLength() - start);
      int nameEnd = text.indexOf('>');
      String name = text.substring(1, nameEnd);
      String endTok = "</" + name + ">";
      int endIndex = text.indexOf(endTok);

      if (endIndex != -1) {
        return endIndex + endTok.length();
      } else {
        return nameEnd + 1;
      }
    } catch (Exception e) {
      return 1;
    }
  }

  private void addUndoableEditListener() {
    ((DefaultStyledDocument) getDocument()).addUndoableEditListener(m_undoManager);
  }

  private void removeUndoableEditListener() {
    ((DefaultStyledDocument) getDocument()).removeUndoableEditListener(m_undoManager);
  }

  private TextLineInfo getLineInfo() {
    try {
      return new TextLineInfo(new StringReader(getText()));
    } catch (Exception e) {
      return new TextLineInfo();
    }
  }

  void selectError(XmlError error) {
    TextLineInfo lineInfo = getLineInfo();
    int line = error.getLine();
    int col = error.getColumn();
    int start = lineInfo.offset(line - 1) + col - 1;
    int len = getElementLength(start);

    setCaretPosition(start);
    moveCaretPosition(start + len);

    requestFocusInWindow();
  }

  private SessionIntegratorFrame getMainFrame() {
    return (SessionIntegratorFrame) getAncestorOfClass(SessionIntegratorFrame.class);
  }

  void save() {
    SessionIntegratorFrame frame = getMainFrame();

    removeAllListeners();
    if (hasErrors()) {
      String msg = "There are configuration errors.  Save anyway?";
      String title = frame.getTitle();
      int type = JOptionPane.YES_NO_OPTION;
      int answer = JOptionPane.showConfirmDialog(frame, msg, title, type);

      if (answer == JOptionPane.YES_OPTION) {
        frame.saveXML(getText());
      }
    } else {
      frame.saveXML(getText());
    }
    addAllListeners();
//    m_undoManager.discardAllEdits();
    m_saveAction.setEnabled(false);
//    m_undoAction.setEnabled(false);
//    m_redoAction.setEnabled(false);
  }

  private void saveAs() {
    SessionIntegratorFrame frame = getMainFrame();

    removeAllListeners();
    if (hasErrors()) {
      String msg = "There are configuration errors.  Save anyway?";
      String title = frame.getTitle();
      int type = JOptionPane.YES_NO_OPTION;
      int answer = JOptionPane.showConfirmDialog(frame, msg, title, type);

      if (answer == JOptionPane.YES_OPTION) {
        frame.exportConfiguration();
      }
    } else {
      frame.exportConfiguration();
    }
    addAllListeners();
  }

  class MyUndoManager extends UndoManager {
    public UndoableEdit nextUndoable() {
      return editToBeUndone();
    }

    public UndoableEdit nextRedoable() {
      return editToBeRedone();
    }

    public void undoableEditHappened(UndoableEditEvent e) {
      UndoableEdit edit = e.getEdit();
      if (edit instanceof DefaultDocumentEvent && ((DefaultDocumentEvent) edit).getType() == EventType.CHANGE) { return; }
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

      if (next != null) {
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

      if (next != null) {
        m_undoManager.redo();
        setEnabled(m_undoManager.canRedo());
        m_undoAction.setEnabled(m_undoManager.canUndo());
      }
    }
  }

  Action getSaveAction() {
    return m_saveAction;
  }

  Action getUndoAction() {
    return m_undoAction;
  }

  Action getRedoAction() {
    return m_redoAction;
  }

  Action getCutAction() {
    return m_helper.getCutAction();
  }

  Action getCopyAction() {
    return m_helper.getCopyAction();
  }

  Action getPasteAction() {
    return m_helper.getPasteAction();
  }
}
