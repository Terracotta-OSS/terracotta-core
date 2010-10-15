/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.RolloverButton;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextField;
import com.tc.admin.model.BasicTcObject;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.ILiveObjectCountProvider;
import com.tc.admin.model.ManagedObjectFacadeProvider;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.ProductInfo;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class RootsPanel extends XContainer implements PropertyChangeListener {
  protected IAdminClientContext         adminClientContext;
  protected ManagedObjectFacadeProvider facadeProvider;
  protected ILiveObjectCountProvider    liveObjectCountProvider;
  protected IClient                     client;
  protected XLabel                      liveObjectCountValueLabel;
  protected XLabel                      explainationLabel;
  protected BasicObjectSetPanel         objectSetPanel;
  protected MouseListener               objectSetMouseListener;
  protected XTextField                  inspectObjectField;
  protected XButton                     inspectObjectButton;

  private static final ImageIcon        helpIcon = new ImageIcon(RootsPanel.class
                                                     .getResource("/com/tc/admin/icons/help.gif"));

  public RootsPanel(IAdminClientContext adminClientContext, ManagedObjectFacadeProvider facadeProvider,
                    ILiveObjectCountProvider liveObjectCountProvider, IBasicObject[] roots) {
    this(adminClientContext, facadeProvider, liveObjectCountProvider, null, roots);
  }

  public RootsPanel(IAdminClientContext adminClientContext, ManagedObjectFacadeProvider facadeProvider,
                    ILiveObjectCountProvider liveObjectCountProvider, IClient client, IBasicObject[] roots) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.facadeProvider = facadeProvider;
    this.liveObjectCountProvider = liveObjectCountProvider;
    this.client = client;

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;

    String tip = adminClientContext.getString("liveObjectCount.tip");
    XLabel liveObjectCountLabel = new XLabel("Live object count:");
    liveObjectCountLabel.setToolTipText(tip);
    topPanel.add(liveObjectCountLabel, gbc);
    gbc.gridx++;

    topPanel.add(liveObjectCountValueLabel = new XLabel("0"), gbc);
    liveObjectCountValueLabel.setToolTipText(tip);
    gbc.gridx++;

    RolloverButton helpButton = new RolloverButton();
    helpButton.setIcon(helpIcon);
    helpButton.addActionListener(new LiveObjectHelpAction());
    helpButton.setFocusable(false);
    topPanel.add(helpButton, gbc);
    gbc.gridx++;

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 1.0;
    explainationLabel = new XLabel();
    topPanel.add(explainationLabel, gbc);

    add(topPanel, BorderLayout.NORTH);

    objectSetPanel = new BasicObjectSetPanel();
    objectSetPanel.setObjects(adminClientContext, client, roots);
    objectSetMouseListener = new ObjectSetMouseListener();
    objectSetPanel.getTree().addMouseListener(objectSetMouseListener);
    add(objectSetPanel, BorderLayout.CENTER);

    XContainer bottomPanel = new XContainer(new GridBagLayout());
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.weightx = 0.0;
    gbc.anchor = GridBagConstraints.WEST;

    bottomPanel.add(new XLabel("Inspect object:"), gbc);
    gbc.gridx++;

    inspectObjectField = new XTextField();
    inspectObjectField.setColumns(16);
    inspectObjectField.setMinimumSize(inspectObjectField.getPreferredSize());
    inspectObjectField.addActionListener(new InspectFieldHandler());
    InspectObjectAction inspectAction = new InspectObjectAction();
    inspectObjectField.getDocument().addDocumentListener(inspectAction);
    bottomPanel.add(inspectObjectField, gbc);
    gbc.gridx++;

    inspectObjectButton = new XButton("Show...");
    inspectObjectButton.setEnabled(false);
    inspectObjectButton.addActionListener(inspectAction);
    bottomPanel.add(inspectObjectButton, gbc);
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    bottomPanel.add(new XLabel(), gbc);

    add(bottomPanel, BorderLayout.SOUTH);

    if (client != null) {
      client.addPropertyChangeListener(this);
    }
  }

  public void setExplainationText(String text) {
    explainationLabel.setText(text);
  }

  private class InspectFieldHandler implements ActionListener {
    private boolean handling;

    public void actionPerformed(ActionEvent ae) {
      if (!handling && inspectObjectButton.isEnabled()) {
        handling = true;
        inspectObjectButton.doClick();
        handling = false;
      }
    }
  }

  private class InspectObjectAction extends XAbstractAction implements DocumentListener {
    private long fObjectID;

    InspectObjectAction() {
      super(adminClientContext.getMessage("roots.inspect.show"));
    }

    public void actionPerformed(ActionEvent ae) {
      ObjectID oid = new ObjectID(fObjectID);
      try {
        int maxFields = ConnectionContext.DSO_SMALL_BATCH_SIZE;
        ManagedObjectFacade mof = facadeProvider.lookupFacade(oid, maxFields);
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, RootsPanel.this);
        JDialog dialog = new JDialog(frame, Long.toString(fObjectID), false);
        BasicTcObject dsoObject = new BasicTcObject(facadeProvider, "", mof, mof.getClassName(), null);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(
                                    new BasicObjectSetPanel(adminClientContext, client,
                                                            new IBasicObject[] { dsoObject }));
        dialog.pack();
        WindowHelper.center(dialog, frame);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(RootsPanel.this, e.getMessage());
      }
    }

    long getLockObjectID() {
      String text = inspectObjectField.getText().trim();
      if (text.length() > 1 && text.charAt(0) == '@') {
        text = text.substring(1);
      }
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException nfe) {
        /**/
      }
      return -1;
    }

    void handleChanged() {
      fObjectID = getLockObjectID();
      inspectObjectButton.setEnabled(fObjectID != -1);
    }

    public void changedUpdate(DocumentEvent e) {
      /**/
    }

    public void insertUpdate(DocumentEvent e) {
      handleChanged();
    }

    public void removeUpdate(DocumentEvent e) {
      handleChanged();
    }
  }

  private class ObjectSetMouseListener extends MouseAdapter implements Serializable {
    @Override
    public void mouseClicked(MouseEvent e) {
      updateLiveObjectCount();
    }
  }

  private void updateLiveObjectCount() {
    try {
      int count = liveObjectCountProvider.getLiveObjectCount();
      liveObjectCountValueLabel.setText(NumberFormat.getNumberInstance().format(count));
    } catch (Exception e) {
      /**/
    }
  }

  public void setObjects(IBasicObject[] roots) {
    objectSetPanel.setObjects(adminClientContext, client, roots);
    updateLiveObjectCount();
  }

  public void clearModel() {
    objectSetPanel.clearModel();
  }

  public void refresh() {
    updateLiveObjectCount();
    objectSetPanel.refresh();
  }

  public void add(IBasicObject root) {
    objectSetPanel.add(root);
    updateLiveObjectCount();
  }

  private class LiveObjectHelpAction implements ActionListener {
    private String getKitID() {
      String kitID = ProductInfo.getInstance().kitID();
      if (ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
        kitID = System.getProperty("com.tc.kitID", "3.0");
      }
      return kitID;
    }

    public void actionPerformed(ActionEvent e) {
      String kitID = getKitID();
      String loc = adminClientContext.format("console.guide.url", kitID, "ConsoleGuide")
                   + "#TerracottaDeveloperConsole-Roots";
      BrowserLauncher.openURL(loc);
    }
  }

  @Override
  public void tearDown() {
    objectSetPanel.getTree().removeMouseListener(objectSetMouseListener);
    if (client != null) {
      client.removePropertyChangeListener(this);
    }

    super.tearDown();

    adminClientContext = null;
    facadeProvider = null;
    client = null;
    liveObjectCountValueLabel = null;
    objectSetPanel = null;
    objectSetMouseListener = null;
    inspectObjectField = null;
    inspectObjectButton = null;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (client == null) return;
    String prop = evt.getPropertyName();
    if (IClusterModelElement.PROP_READY.equals(prop)) {
      final boolean isReady = client.isReady();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          objectSetPanel.setEnabled(isReady);
          inspectObjectField.setEnabled(isReady);
          inspectObjectButton.setEnabled(isReady);
        }
      });
    }
  }
}
