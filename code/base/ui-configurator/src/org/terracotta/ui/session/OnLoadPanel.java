/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.ButtonSet;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTextField;
import com.terracottatech.config.Include;
import com.terracottatech.config.OnLoad;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

public class OnLoadPanel extends XContainer {
  private Include            include;
  private XLabel             classExprLabel;
  private ButtonSet          selectorGroup;
  private PagedView          pagedView;
  private XTextField         methodNameField;
  private XTextArea          codePane;
  private XCheckBox          honorTransientToggle;

  public static final String NOOP_VIEW    = "NoOp";
  public static final String CALL_VIEW    = "Call";
  public static final String EXECUTE_VIEW = "Execute";

  public OnLoadPanel() {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 1, 1, 1);

    add(classExprLabel = new XLabel(), gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;
    XContainer mainPanel = new XContainer(new BorderLayout());
    mainPanel.add(selectorGroup = createSelectorPanel(), BorderLayout.NORTH);
    mainPanel.add(pagedView = createPagedView(), BorderLayout.CENTER);
    mainPanel.setBorder(BorderFactory.createTitledBorder("On Load"));
    add(mainPanel, gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;

    honorTransientToggle = new XCheckBox("Honor transient");
    honorTransientToggle.addActionListener(new HonorTransientHandler());
    add(honorTransientToggle, gbc);
  }

  private ButtonSet createSelectorPanel() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    ButtonSet buttonSet = new ButtonSet(new GridBagLayout());
    buttonSet.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        pagedView.setPage(selectorGroup.getSelected());
      }
    });

    JRadioButton btn = new JRadioButton("Do nothing");
    btn.setName(NOOP_VIEW);
    buttonSet.add(btn, gbc);
    gbc.gridx++;

    btn = new JRadioButton("Call a method");
    btn.setName(CALL_VIEW);
    buttonSet.add(btn, gbc);
    gbc.gridx++;

    btn = new JRadioButton("Execute some code");
    btn.setName(EXECUTE_VIEW);
    buttonSet.add(btn, gbc);

    return buttonSet;
  }

  private class HonorTransientHandler implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (include != null) {
        boolean selected = honorTransientToggle.isSelected();
        if (selected) {
          include.setHonorTransient(true);
        } else {
          include.unsetHonorTransient();
        }
      }
    }
  }

  private PagedView createPagedView() {
    PagedView view = new PagedView();

    XContainer noopPage = new XContainer();
    noopPage.setName(NOOP_VIEW);
    view.addPage(noopPage);

    XContainer callPage = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    callPage.add(new XLabel("Method name:"), gbc);
    gbc.gridx++;
    methodNameField = new XTextField();
    methodNameField.setColumns(20);
    callPage.add(methodNameField, gbc);
    callPage.setName(CALL_VIEW);
    view.addPage(callPage);

    XContainer executePage = new XContainer(new GridBagLayout());
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 1, 1, 1);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    XLabel label = new XLabel("Code to execute:");
    label.setHorizontalAlignment(SwingConstants.LEFT);
    executePage.add(label, gbc);
    gbc.gridy++;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    executePage.add(new XScrollPane(codePane = new XTextArea()), gbc);
    codePane.setRows(3);
    codePane.setColumns(40);
    executePage.setName(EXECUTE_VIEW);
    view.addPage(executePage);

    return view;
  }

  public OnLoad ensureOnLoad() {
    OnLoad onLoad = include.getOnLoad();
    return onLoad != null ? onLoad : include.addNewOnLoad();
  }

  public void ensureOnLoadUnset() {
    if (include.isSetOnLoad()) {
      include.unsetOnLoad();
    }
  }

  class ButtonSetHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String selected = selectorGroup.getSelected();

      if (selected == NOOP_VIEW) {
        methodNameField.setText(null);
        codePane.setText(null);
        include.unsetOnLoad();
      } else if (selected == CALL_VIEW) {
        codePane.setText(null);
        OnLoad onLoad = ensureOnLoad();
        if (onLoad.isSetExecute()) {
          onLoad.unsetExecute();
        }
      } else {
        methodNameField.setText(null);
        OnLoad onLoad = ensureOnLoad();
        if (onLoad.isSetMethod()) {
          onLoad.unsetMethod();
        }
      }

      pagedView.setPage(selected);
    }
  }

  public void setInclude(Include include) {
    this.include = include;
    classExprLabel.setText(include.getClassExpression());

    OnLoad onLoad = include.getOnLoad();
    String view = NOOP_VIEW;

    codePane.setText(null);
    methodNameField.setText(null);

    if (onLoad != null) {
      if (onLoad.isSetExecute()) {
        view = EXECUTE_VIEW;
        codePane.setText(onLoad.getExecute());
      } else if (onLoad.isSetMethod()) {
        view = CALL_VIEW;
        methodNameField.setText(onLoad.getMethod());
      }
    }

    pagedView.setPage(view);
    selectorGroup.setSelected(view);

    honorTransientToggle.setSelected(include.getHonorTransient());
  }

  public void updateInclude() {
    String selected = selectorGroup.getSelected();

    if (selected == null) return;

    if (selected.equals(NOOP_VIEW)) {
      ensureOnLoadUnset();
    } else {
      OnLoad onLoad = ensureOnLoad();
      if (selected.equals(CALL_VIEW)) {
        String methodName = methodNameField.getText().trim();
        if (methodName == null || methodName.length() == 0) {
          ensureOnLoadUnset();
        } else {
          if (onLoad.isSetExecute()) {
            onLoad.unsetExecute();
          }
          onLoad.setMethod(methodName);
        }
      } else {
        String code = codePane.getText().trim();
        if (code == null || code.length() == 0) {
          ensureOnLoadUnset();
        } else {
          if (onLoad.isSetMethod()) {
            onLoad.unsetMethod();
          }
          onLoad.setExecute(code);
        }
      }
    }

    boolean honorTransientSelected = honorTransientToggle.isSelected();
    if (honorTransientSelected) {
      include.setHonorTransient(true);
    } else {
      include.unsetHonorTransient();
    }
  }

  public Include getInclude() {
    return include;
  }
}
