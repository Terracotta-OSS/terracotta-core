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
import java.awt.Font;
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

  private SaveAction             saveAction;
  private UndoAction             undoAction;
  private RedoAction             redoAction;

  private static final String    SAVE_CMD          = "Save";
  private static final String    UNDO_CMD          = "Undo";
  private static final String    REDO_CMD          = "Redo";

  private static final KeyStroke SAVE_STROKE       = KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY_MASK, false);
  private static final KeyStroke UNDO_STROKE       = KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK, false);
  private static final KeyStroke REDO_STROKE       = KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK
                                                                                           | InputEvent.SHIFT_MASK,
                                                                            false);

  private ConfigTextListener     configTextListener;
  private SimpleAttributeSet     errorAttrSet;
  private Timer                  parseTimer;
  private MyUndoManager          undoManager;
  private JViewport              viewPort;

  public ConfigTextPane() {
    super();

    setFont(new Font("monospaced", Font.PLAIN, 12));

    errorAttrSet = new SimpleAttributeSet();
    StyleConstants.setForeground(errorAttrSet, Color.red);

    undoManager = new MyUndoManager();

    undoAction = new UndoAction();
    redoAction = new RedoAction();
    saveAction = new SaveAction();

    getActionMap().put(SAVE_CMD, saveAction);
    getActionMap().put(UNDO_CMD, undoAction);
    getActionMap().put(REDO_CMD, redoAction);

    getInputMap().put(SAVE_STROKE, SAVE_CMD);
    getInputMap().put(UNDO_STROKE, UNDO_CMD);
    getInputMap().put(REDO_STROKE, REDO_CMD);

    parseTimer = new Timer(2000, new ParseTimerAction());
    parseTimer.setRepeats(false);

    configTextListener = new ConfigTextListener();

    // force popup actions to be created early
    testInitPopupMenu();
  }

  public void addNotify() {
    super.addNotify();
    viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
  }

  protected JPopupMenu createPopup() {
    JPopupMenu popupMenu = super.createPopup();

    popupMenu.add(new JSeparator());
    popupMenu.add(undoAction);
    popupMenu.add(redoAction);
    popupMenu.add(new JSeparator());
    popupMenu.add(saveAction);
    popupMenu.add(new SaveAsAction());

    return popupMenu;
  }

  private void removeDocumentListener() {
    parseTimer.stop();
    getDocument().removeDocumentListener(configTextListener);
  }

  private void removeAllListeners() {
    removeDocumentListener();
    removeUndoableEditListener();
  }

  private void addDocumentListener() {
    getDocument().addDocumentListener(configTextListener);
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
    private final String  origText;
    private final String  newText;
    private final boolean isModified;

    UndoableSetAction(String origText, String newText, boolean isModified) {
      this.origText = origText;
      this.newText = newText;
      this.isModified = isModified;
    }

    private void update(String text, boolean modified) {
      set(text, false);
      handleContentChange();
      saveAction.setEnabled(modified);
      getMainFrame().setXmlModified(modified);
    }

    public void undo() throws CannotUndoException {
      super.undo();
      update(origText, isModified);
    }

    public void redo() throws CannotRedoException {
      super.redo();
      update(newText, true);
    }
  }

  private UndoableEditEvent createUndoableSetEditEvent(String newText) {
    return new UndoableEditEvent(this, new UndoableSetAction(getText(), newText, saveAction.isEnabled()));
  }

  public void set(String text) {
    set(text, true);
  }

  public void set(String text, boolean createUndoEvent) {
    String curText = getText();
    if (curText != null && curText.equals(text)) return;

    Point viewPosition = null;
    if (viewPort != null) {
      viewPosition = viewPort.getViewPosition();
    }
    removeAllListeners();
    if (createUndoEvent) {
      undoManager.undoableEditHappened(createUndoableSetEditEvent(text));
    }
    setText(text);
    hasErrors();
    saveAction.setEnabled(true);
    addAllListeners();
    if (viewPosition != null) {
      final Point viewPos = viewPosition;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          viewPort.setViewPosition(viewPos);
        }
      });
    }
  }

  private void restartParseTimer() {
    parseTimer.stop();
    parseTimer.start();
  }

  class ConfigTextListener implements DocumentListener {
    public void insertUpdate(DocumentEvent e) {
      saveAction.setEnabled(true);
      restartParseTimer();
    }

    public void removeUpdate(DocumentEvent e) {
      saveAction.setEnabled(true);
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

      doc.setCharacterAttributes(start, len, errorAttrSet, true);
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
    ((DefaultStyledDocument) getDocument()).addUndoableEditListener(undoManager);
  }

  private void removeUndoableEditListener() {
    ((DefaultStyledDocument) getDocument()).removeUndoableEditListener(undoManager);
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
    return (SessionIntegratorFrame) SwingUtilities.getAncestorOfClass(SessionIntegratorFrame.class, this);
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
    saveAction.setEnabled(false);
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
      undoAction.setEnabled(canUndo());
      redoAction.setEnabled(canRedo());
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
      UndoableEdit next = undoManager.nextUndoable();

      if (next != null) {
        undoManager.undo();
        setEnabled(undoManager.canUndo());
        redoAction.setEnabled(undoManager.canRedo());
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
      UndoableEdit next = undoManager.nextRedoable();
      if (next != null) {
        undoManager.redo();
        setEnabled(undoManager.canRedo());
        undoAction.setEnabled(undoManager.canUndo());
      }
    }
  }

  Action getSaveAction() {
    return saveAction;
  }

  Action getUndoAction() {
    return undoAction;
  }

  Action getRedoAction() {
    return redoAction;
  }

  Action getCutAction() {
    return helper.getCutAction();
  }

  Action getCopyAction() {
    return helper.getCopyAction();
  }

  Action getPasteAction() {
    return helper.getPasteAction();
  }
}
