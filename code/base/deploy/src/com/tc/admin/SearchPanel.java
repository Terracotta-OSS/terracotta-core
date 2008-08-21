/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextField;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

/**
 * TODO: the text component should be a JTextPane and the feedback mechanism should be a style
 * instead of setting the selection.
 */

public class SearchPanel extends XContainer {
  private XTextField         fFindField;
  private JButton            fFindNextButton;
  private JButton            fFindPreviousButton;
  private JTextComponent     fTextComponent;

  public static final String SEARCH_AGAIN         = "search-again";
  public static final String SEARCH_AGAIN_REVERSE = "search-again-reverse";
  public static final String BACKUP_SEARCH        = "backup-search";

  public boolean             incremental          = false;

  public SearchPanel() {
    super();
  }

  public void load(ContainerResource res) {
    super.load((ContainerResource) AdminClient.getContext().getComponent("SearchPanel"));

    fFindField = (XTextField) findComponent("FindField");
    fFindField.setFocusLostBehavior(JFormattedTextField.PERSIST);
    fFindField.getDocument().addDocumentListener(new FieldDocListener());

    fFindNextButton = (XButton) findComponent("FindNextButton");
    fFindPreviousButton = (XButton) findComponent("FindPreviousButton");

    setName(res.getName());
  }

  public void setHandlers(ActionListener findNextHandler, ActionListener findPreviousHandler) {
    fFindField.addActionListener(findNextHandler);
    fFindNextButton.addActionListener(findNextHandler);
    fFindPreviousButton.addActionListener(findPreviousHandler);
  }

  public void setTextComponent(JTextComponent textComponent) {
    fTextComponent = textComponent;

    boolean enabled = false;
    if (fTextComponent != null) {
      Document doc = fTextComponent.getDocument();
      enabled = doc.getLength() > 0;
      doc.addDocumentListener(new SearchDocListener());
      setHandlers(new FindNextHandler(), new FindPreviousHandler());
      fTextComponent.putClientProperty("search-panel", this);

      textComponent.getActionMap().put(SEARCH_AGAIN, new SearchAgainAction());
      textComponent.getActionMap().put(SEARCH_AGAIN_REVERSE, new SearchAgainReverseAction());

      KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
      textComponent.getInputMap().put(ks, SEARCH_AGAIN);

      ks = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK);
      textComponent.getInputMap().put(ks, SEARCH_AGAIN);

      ks = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK);
      textComponent.getInputMap().put(ks, SEARCH_AGAIN_REVERSE);

      if (!fTextComponent.isEditable()) {
        Keymap keymap = textComponent.getKeymap();
        if (keymap != null) {
          if (!(keymap.getDefaultAction() instanceof DefaultKeyTypedAction)) {
            keymap.setDefaultAction(new DefaultKeyTypedAction());
          }
          textComponent.getActionMap().put(DefaultEditorKit.deletePrevCharAction, new BackupAction());
        }
      }
    }
    setEnabled(enabled);
  }

  public static class DefaultKeyTypedAction extends TextAction {
    DefaultKeyTypedAction() {
      super(DefaultEditorKit.defaultKeyTypedAction);
    }

    public void actionPerformed(ActionEvent e) {
      JTextComponent source = (JTextComponent) e.getSource();
      SearchPanel searchPanel = (SearchPanel) source.getClientProperty("search-panel");
      String content = e.getActionCommand();
      int mod = e.getModifiers();
      if ((content != null) && (content.length() > 0)
          && ((mod & ActionEvent.ALT_MASK) == (mod & ActionEvent.CTRL_MASK))) {
        char c = content.charAt(0);
        if ((c >= 0x20) && (c != 0x7F)) {
          searchPanel.doIncrementalSearch(content);
        }
      }
    }
  }

  private class SearchAgainAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      doSearch(true);
    }
  }

  private class SearchAgainReverseAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      doSearch(false);
    }
  }

  private class BackupAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      String text = fFindField.getText();
      int len = text.length();
      if (len > 0) {
        fFindField.setText(text.substring(0, len - 1));
        doSearch(false);
      }
    }
  }

  private void testSetEnabled(Document doc) {
    setEnabled(doc != null && doc.getLength() > 0);
  }

  private class SearchDocListener implements DocumentListener {
    public void changedUpdate(DocumentEvent e) {
      /**/
    }

    public void insertUpdate(DocumentEvent e) {
      testSetEnabled(e.getDocument());
    }

    public void removeUpdate(DocumentEvent e) {
      testSetEnabled(e.getDocument());
    }
  }

  private class FieldDocListener implements DocumentListener {
    public void changedUpdate(DocumentEvent e) {
      /**/
    }

    private void testSetFieldEnabled(Document doc) {
      boolean hasContent = doc != null && doc.getLength() > 0;
      fFindNextButton.setEnabled(hasContent);
      fFindPreviousButton.setEnabled(hasContent);
    }

    public void insertUpdate(DocumentEvent e) {
      testSetFieldEnabled(e.getDocument());
      if(!incremental) return;
      if (fFindField.isFocusOwner() && fTextComponent != null && !fTextComponent.isEditable()) {
        doSearch(true);
      }
    }

    public void removeUpdate(DocumentEvent e) {
      testSetFieldEnabled(e.getDocument());
      if(!incremental) return;
      int selStart = fFindField.getSelectionStart();
      int selEnd = fFindField.getSelectionEnd();
      int docLen = fFindField.getDocument().getLength();
      if (fFindField.isFocusOwner() && fTextComponent != null && !fTextComponent.isEditable() && selStart == selEnd
          && selStart >= docLen) {
        doSearch(false);
      }
    }
  }

  public String getSearchText() {
    return fFindField.getText();
  }

  public JTextField getField() {
    return fFindField;
  }

  public JButton getNextButton() {
    return fFindNextButton;
  }

  public JButton getPreviousButton() {
    return fFindPreviousButton;
  }

  private class FindNextHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      doSearch(true);
    }
  }

  private class FindPreviousHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      doSearch(false);
    }
  }

  void doIncrementalSearch(final String s) {
    fFindField.setText(fFindField.getText() + s);
    doSearch(true);
  }

  private void doSearch(boolean next) {
    String searchText = getSearchText();
    String fullText = fTextComponent.getText();
    int selStart = fTextComponent.getSelectionStart();
    int selEnd = fTextComponent.getSelectionEnd();
    int index = -1;

    if (next) {
      while (true) {
        if ((index = fullText.indexOf(searchText, selStart)) == -1) {
          index = fullText.indexOf(searchText, 0);
          break;
        } else if (index == selStart && searchText.length() == Math.abs(selStart - selEnd)) {
          selStart++;
        } else {
          break;
        }
      }
    } else {
      while (true) {
        fullText = fullText.substring(0, Math.min(fullText.length(), selEnd));
        if ((index = fullText.lastIndexOf(searchText)) == -1) {
          fullText = fTextComponent.getText();
          index = fullText.lastIndexOf(searchText);
          break;
        } else if (index == selStart && searchText.length() == Math.abs(selStart - selEnd)) {
          selEnd--;
        } else {
          break;
        }
      }
    }

    if (index != -1) {
      fTextComponent.select(index, index + searchText.length());
      if (!fTextComponent.isFocusOwner()) {
        fTextComponent.requestFocusInWindow();
      }
    } else {
      Toolkit.getDefaultToolkit().beep();
    }
  }
}
