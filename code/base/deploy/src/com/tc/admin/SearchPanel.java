/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.RolloverButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextField;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

/**
 * TODO: the text component should be a JTextPane and the feedback mechanism should be a style instead of setting the
 * selection.
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

  private static ImageIcon   fNextIcon;
  private static ImageIcon   fPreviousIcon;

  static {
    fNextIcon = new ImageIcon(SearchPanel.class.getResource("/com/tc/admin/icons/next_nav.gif"));
    fPreviousIcon = new ImageIcon(SearchPanel.class.getResource("/com/tc/admin/icons/previous_nav.gif"));
  }

  public SearchPanel(ApplicationContext appContext) {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(3, 3, 3, 3);

    add(new XLabel("Find:"), gbc);
    gbc.gridx++;

    add(fFindField = new XTextField(), gbc);
    fFindField.setColumns(16);
    fFindField.setMinimumSize(fFindField.getPreferredSize());
    fFindField.getDocument().addDocumentListener(new FieldDocListener());
    gbc.gridx++;

    add(fFindNextButton = new RolloverButton(appContext.getString("next")), gbc);
    fFindNextButton.setIcon(fNextIcon);
    fFindNextButton.setEnabled(false);
    gbc.gridx++;

    add(fFindPreviousButton = new RolloverButton(appContext.getString("previous")), gbc);
    fFindPreviousButton.setIcon(fPreviousIcon);
    fFindPreviousButton.setEnabled(false);

    // filler
    gbc.weightx = 1.0;
    add(new XLabel(""), gbc);
  }

  public SearchPanel(ApplicationContext appContext, JTextComponent textComponent) {
    this(appContext);
    setTextComponent(textComponent);
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
      if (searchPanel != null) {
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
    boolean hasContent = doc != null && doc.getLength() > 0;
    setEnabled(hasContent);
    // if (!hasContent) {
    // fFindNextButton.setEnabled(false);
    // fFindPreviousButton.setEnabled(false);
    // }
    fFindNextButton.setEnabled(hasContent);
    fFindPreviousButton.setEnabled(hasContent);
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
      if (!incremental) { return; }
      if (fFindField.isFocusOwner() && fTextComponent != null && !fTextComponent.isEditable()) {
        doSearch(true);
      }
    }

    public void removeUpdate(DocumentEvent e) {
      testSetFieldEnabled(e.getDocument());
      if (!incremental) { return; }
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
    String searchText = getSearchText().toLowerCase();
    String fullText = fTextComponent.getText().toLowerCase();
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
          fullText = fTextComponent.getText().toLowerCase();
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
      centerSelection(fTextComponent);
      if (!fTextComponent.isFocusOwner()) {
        fTextComponent.requestFocusInWindow();
      }
    } else {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  private static void centerSelection(JTextComponent textComponent) {
    JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, textComponent);
    if (viewport != null) {
      try {
        Rectangle r = textComponent.modelToView(textComponent.getCaretPosition());
        int extentHeight = viewport.getExtentSize().height;
        int viewHeight = viewport.getViewSize().height;
        int y = Math.max(0, r.y - (extentHeight / 2));
        y = Math.min(y, viewHeight - extentHeight);
        viewport.setViewPosition(new Point(0, y));
      } catch (BadLocationException ble) {/**/
      }
    }
  }
}
