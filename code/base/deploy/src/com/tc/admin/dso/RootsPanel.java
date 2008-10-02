/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.Dialog;
import org.dijon.Frame;
import org.dijon.Label;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextField;
import com.tc.admin.model.BasicTcObject;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterNode;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.ProductInfo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class RootsPanel extends XContainer {
  protected IClusterModel       m_clusterModel;
  protected IClusterNode        m_clusterNode;
  protected Label               m_liveObjectCountValueLabel;
  protected BasicObjectSetPanel m_objectSetPanel;
  protected MouseListener       m_objectSetMouseListener;
  protected XTextField          m_inspectObjectField;
  protected XButton             m_inspectObjectButton;
  
  public RootsPanel(IClusterModel clusterModel, IClusterNode clusterNode, IBasicObject[] roots) {
    super();

    load((ContainerResource) AdminClient.getContext().getComponent("RootsPanel"));

    m_clusterModel = clusterModel;
    m_clusterNode = clusterNode;
    String tip = AdminClient.getContext().getString("liveObjectCount.tip");
    ((Label) findComponent("LiveObjectCountLabel")).setToolTipText(tip);
    m_liveObjectCountValueLabel = (Label) findComponent("LiveObjectCountValueLabel");
    m_liveObjectCountValueLabel.setToolTipText(tip);
    ((Button) findComponent("HelpButton")).addActionListener(new LiveObjectHelpAction());
    m_objectSetPanel = (BasicObjectSetPanel) findComponent("ObjectSetPanel");

    m_objectSetPanel.setObjects(clusterNode, roots);
    m_objectSetMouseListener = new ObjectSetMouseListener();
    m_objectSetPanel.getTree().addMouseListener(m_objectSetMouseListener);
    
    InspectObjectAction inspectAction = new InspectObjectAction();
    m_inspectObjectField = (XTextField) findComponent("InspectObjectField");
    m_inspectObjectField.addActionListener(new InspectFieldHandler());
    m_inspectObjectField.getDocument().addDocumentListener(inspectAction);
    m_inspectObjectButton = (XButton) findComponent("InspectObjectButton");
    m_inspectObjectButton.addActionListener(inspectAction);
  }

  private class InspectFieldHandler implements ActionListener {
    private boolean handling;
    
    public void actionPerformed(ActionEvent ae) {
      if(!handling && m_inspectObjectButton.isEnabled()) {
        handling = true;
        m_inspectObjectButton.doClick();
        handling = false;
      }
    }
  }
  
  private class InspectObjectAction extends XAbstractAction implements DocumentListener {
    private long fObjectID;

    InspectObjectAction() {
      super("Show...");
    }

    public void actionPerformed(ActionEvent ae) {
      ObjectID oid = new ObjectID(fObjectID);
      try {
        int maxFields = ConnectionContext.DSO_SMALL_BATCH_SIZE;
        ManagedObjectFacade mof = m_clusterModel.lookupFacade(oid, maxFields);
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        Dialog dialog = new Dialog(frame, Long.toString(fObjectID), false);
        BasicTcObject dsoObject = new BasicTcObject(m_clusterModel, "", mof, mof.getClassName(), null);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(new BasicObjectSetPanel(m_clusterModel, new IBasicObject[] { dsoObject }));
        dialog.pack();
        dialog.center(frame);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(RootsPanel.this, e.getMessage());
      }
    }

    long getLockObjectID() {
      String text = m_inspectObjectField.getText().trim();
      if(text.length() > 1 && text.charAt(0) == '@') {
        text = text.substring(1);
      }
      try {
        return Long.parseLong(text);
      } catch(NumberFormatException nfe) {
        /**/
      }
      return -1;
    }
    
    void handleChanged() {
      fObjectID = getLockObjectID();
      m_inspectObjectButton.setEnabled(fObjectID != -1);
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
    public void mouseClicked(MouseEvent e) {
      updateLiveObjectCount();
    }
  }

  private void updateLiveObjectCount() {
    m_liveObjectCountValueLabel.setText(Integer.toString(m_clusterNode.getLiveObjectCount()));
  }

  public void setObjects(IBasicObject[] roots) {
    updateLiveObjectCount();
    m_objectSetPanel.setObjects(m_clusterNode, roots);
  }

  public void clearModel() {
    m_objectSetPanel.clearModel();
  }

  public void refresh() {
    updateLiveObjectCount();
    m_objectSetPanel.refresh();
  }

  public void add(IBasicObject root) {
    m_objectSetPanel.add(root);
  }
  
  private class LiveObjectHelpAction implements ActionListener {
    private String getKitID() {
      String kitID = ProductInfo.getInstance().kitID();
      if(ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
        kitID = System.getProperty("com.tc.kitID", "42.0");
      }
      return kitID;
    }
    
    public void actionPerformed(ActionEvent e) {
      String kitID =getKitID();
      String loc = "http://www.terracotta.org/kit/reflector?kitID=" + kitID
                   + "&pageID=ConsoleGuide#AdminConsoleGuide-Roots";
      BrowserLauncher.openURL(loc);
    }
  }

  public void tearDown() {
    m_objectSetPanel.getTree().removeMouseListener(m_objectSetMouseListener);

    super.tearDown();

    m_clusterNode = null;
    m_liveObjectCountValueLabel = null;
    m_objectSetPanel = null;
    m_objectSetMouseListener = null;
  }
}
