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
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class SearchPanel extends XContainer {
  private XTextField         fFindField;
  private JButton            fFindNextButton;
  private JButton            fFindPreviousButton;
  private JTextComponent     fTextComponent;

  public static final String SEARCH_AGAIN = "search-again";

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
      if (doc != null) {
        enabled = doc.getLength() > 0;
        doc.addDocumentListener(new SearchDocListener());
      }
      setHandlers(new FindNextHandler(), new FindPreviousHandler());
    }
    setEnabled(enabled);

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    textComponent.getInputMap().put(ks, SEARCH_AGAIN);
    textComponent.getActionMap().put(SEARCH_AGAIN, new SearchAgainAction());
  }

  private class SearchAgainAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      if (fFindNextButton.isEnabled()) {
        fFindNextButton.doClick();
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
    }

    public void removeUpdate(DocumentEvent e) {
      testSetFieldEnabled(e.getDocument());
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

  private void doSearch(boolean next) {
    String searchText = getSearchText();
    String fullText = fTextComponent.getText();
    int selStart = fTextComponent.getSelectionStart();
    int selEnd = fTextComponent.getSelectionEnd();
    int index = -1;

    if (next) {
      index = fullText.indexOf(searchText, selEnd);
    } else {
      fullText = fullText.substring(0, selStart);
      index = fullText.lastIndexOf(searchText);
    }

    if (index != -1) {
      fTextComponent.select(index, index + searchText.length());
      fTextComponent.requestFocusInWindow();
    } else {
      Toolkit.getDefaultToolkit().beep();
    }
  }
}
