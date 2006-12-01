/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.FrameResource;
import org.dijon.HelpManager;
import org.dijon.Label;
import org.dijon.Separator;
import org.dijon.UndoMonger;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.PrefsHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XMenu;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XMenuItem;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTextField;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class AdminClientPanel extends XContainer
  implements AdminClientController,
             UndoMonger
{
  private   NavTree         m_tree;
  private   XContainer      m_nodeView;
  private   JSplitPane      m_mainSplitter;
  private   Integer         m_mainDivLoc;
  private   JSplitPane      m_leftSplitter;
  private   Integer         m_leftDivLoc;
  private   DividerListener m_dividerListener;
  private   XTabbedPane     m_bottomPane;
  private   XTextArea       m_logArea;
  private   ArrayList       m_logListeners;
  private   Icon            m_infoIcon;
  private   XTextField      m_statusLine;
  protected UndoAction      m_undoCmd;
  protected RedoAction      m_redoCmd;
  protected UndoManager     m_undoManager;
  protected NewServerAction m_newServerAction;
  protected HelpAction      m_helpAction;
  protected AboutAction     m_aboutAction;

  public static final String UNDO = "Undo";
  public static final String REDO = "Redo";

  protected MouseAdapter
    m_statusCleaner = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        setStatus(null);
      }
    };
  
  public AdminClientPanel() {
    super();

    AdminClientContext acc = AdminClient.getContext();

    acc.controller = this;
    
    FrameResource frameRes = acc.topRes.getFrame("MyFrame");
    load(frameRes.getContentPane());

    m_tree       = (NavTree)findComponent("Tree");
    m_nodeView   = (XContainer)findComponent("NodeView");
    m_bottomPane = (XTabbedPane)findComponent("BottomPane");
    m_logArea    = (XTextArea)m_bottomPane.findComponent("LogArea");
    m_statusLine = (XTextField)findComponent("StatusLine");

    m_nodeView.setLayout(new BorderLayout());

    m_tree.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent me) {
        TreePath path = m_tree.getPathForLocation(me.getX(), me.getY());

        if(path != null) {
          m_tree.requestFocus();
          
          XTreeNode node = (XTreeNode)path.getLastPathComponent();
          if(node != null) {
            select(node);
          }
        }
      }

      public void mouseClicked(MouseEvent me) {
        TreePath path = m_tree.getPathForLocation(me.getX(), me.getY());

        if(path != null) {
          m_tree.requestFocus();
          
          XTreeNode node = (XTreeNode)path.getLastPathComponent();
          if(node != null) {
            node.nodeClicked(me);
          }
        }
      }
    });
    
    m_tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent tse) {
        TreePath path = tse.getNewLeadSelectionPath();

        m_nodeView.removeAll();

        if(path != null) {
          m_tree.requestFocus();
          
          XTreeNode node = (XTreeNode)path.getLastPathComponent();
          if(node != null) {
            node.nodeSelected(tse);
            
            if(node instanceof ComponentNode) {
              ComponentNode      cnode = (ComponentNode)node;
              java.awt.Component comp  = (java.awt.Component)cnode.getComponent();
              
              if(comp != null) {
                m_nodeView.add(comp);
              }
            }
          }
        }

        m_nodeView.revalidate();
        m_nodeView.repaint();
      }
    });

    m_infoIcon = LogHelper.getHelper().getInfoIcon();

    m_logListeners = new ArrayList();
    LogDocumentListener ldl = new LogDocumentListener(m_logArea);
    m_logListeners.add(ldl);
    m_logArea.getDocument().addDocumentListener(ldl);

    m_bottomPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        int index = m_bottomPane.getSelectedIndex();
        m_bottomPane.setIconAt(index, null);
      }
    });

    m_tree.setModel(new NavTreeModel());

    setHelpPath("/com/tc/admin/AdminClient.html");

    addMouseListener(m_statusCleaner);

    addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        setStatus(null);
      }
    });

    getActionMap().put(UNDO, m_undoCmd = new UndoAction());
    getActionMap().put(REDO, m_redoCmd = new RedoAction());
    
    initNavTreeMenu();
  }

  protected NewServerAction getNewServerAction() {
    if(m_newServerAction == null) {
      m_newServerAction = new NewServerAction();
    }
    return m_newServerAction;
  }
  
  protected HelpAction getHelpAction() {
    if(m_helpAction == null) {
      m_helpAction = new HelpAction();
    }
    return m_helpAction;
  }
  
  protected AboutAction getAboutAction() {
    if(m_aboutAction == null) {
      m_aboutAction = new AboutAction();
    }
    return m_aboutAction;
  }
  
  protected void initNavTreeMenu() {
    JPopupMenu popup = new JPopupMenu("ProjectTree Actions");
    
    popup.add(getNewServerAction());
    popup.add(new Separator());
    popup.add(getHelpAction());
    if(shouldAddAboutItem()) {
      popup.add(getAboutAction());
    }
    
    m_tree.setPopupMenu(popup);
  }
  
  private String getBundleString(String key) {
    return AdminClient.getContext().getMessage(key);
  }
  
  public void initMenubar(XMenuBar menuBar) {
    XMenu menu = new XMenu(getBundleString("file.menu.label"));

    menu.add(m_newServerAction = new NewServerAction());
    menu.add(new JSeparator());
    menu.add(new QuitAction());

    menuBar.add(menu);

    menu = new XMenu(getBundleString("help.menu.label"));
    XMenuItem mitem = new XMenuItem("AdminClient Help",
                                    HelpHelper.getHelper().getHelpIcon());
    mitem.setAction(m_helpAction = new HelpAction());
    menu.add(mitem);
    menu.addSeparator();
    menu.add(new ContactTerracottaAction("Visit Terracotta Forums",
                                         "http://www.terracottatech.com/forums/"));
    menu.add(new ContactTerracottaAction("Contact Terracotta Technical Support",
                                         "http://www.terracottatech.com/support_services.shtml"));
    /*
    menu.add(new ContactTerracottaAction("Contact Terracotta Field Engineering",
                                         "http://www.terracottatech.com/contact/field/"));
    menu.add(new ContactTerracottaAction("Contact Terracotta Sales",
                                         "http://www.terracottatech.com/contact/"));
    */
    menu.addSeparator();
    menu.add(m_aboutAction = new AboutAction());
    
    menuBar.add(menu);
  }

  class HelpAction extends XAbstractAction {
    HelpAction() {
      super("AdminClient Help");
    }
    
    public void actionPerformed(ActionEvent ae) {
      block();
      HelpManager.getInstance().showHelp((JComponent)AdminClientPanel.this);
      unblock();
    }
  }
  
  public boolean isExpanded(XTreeNode node) {
    return node != null &&
           m_tree.isExpanded(new TreePath(node.getPath()));
  }

  public void expand(XTreeNode node) {
    if(node != null) {
      m_tree.expandPath(new TreePath(node.getPath()));
    }
  }

  public boolean isSelected(XTreeNode node) {
    return node != null &&
           m_tree.isPathSelected(new TreePath(node.getPath()));
  }

  public void select(XTreeNode node) {
    if(node != null) {
      m_tree.requestFocus();
      m_tree.setSelectionPath(new TreePath(node.getPath()));
    }
  }

  public void remove(XTreeNode node) {
    XTreeModel model    = (XTreeModel)m_tree.getModel();
    XTreeNode  parent   = (XTreeNode)node.getParent();
    int        index    = parent.getIndex(node);
    TreePath   nodePath = new TreePath(node.getPath());
    TreePath   selPath  = m_tree.getSelectionPath();

    node.tearDown();
    model.removeNodeFromParent(node);

    if(nodePath.isDescendant(selPath)) {
      int count = parent.getChildCount();

      if(count > 0) {
        node = (XTreeNode)parent.getChildAt(index < count ? index : count-1);
      }
      else {
        node = parent;
      }

      m_tree.setSelectionPath(new TreePath(node.getPath()));
    }
  }

  public void nodeStructureChanged(XTreeNode node) {
    TreeModel treeModel = m_tree.getModel();
    
    if(treeModel instanceof XTreeModel) {
      ((XTreeModel)treeModel).nodeStructureChanged(node);
    }
  }

  public void nodeChanged(XTreeNode node) {
    TreeModel treeModel = m_tree.getModel();
    
    if(treeModel instanceof XTreeModel) {
      ((XTreeModel)treeModel).nodeChanged(node);
    }
  }

  protected Preferences getPreferences() {
    AdminClientContext acc = AdminClient.getContext();
    return acc.prefs.node("AdminClientFrame");
  }

  protected void storePreferences() {
    AdminClientContext acc = AdminClient.getContext();
    acc.client.storePrefs();
  }

  private int getSplitPref(JSplitPane splitter) {
    Preferences        prefs      = getPreferences();
    Preferences        splitPrefs = prefs.node(splitter.getName());

    return splitPrefs.getInt("Split", -1);
  }

  private JSplitPane getMainSplitter() {
    if(m_mainSplitter == null) {
      m_mainSplitter = (JSplitPane)findComponent("MainSplitter");
      m_mainDivLoc   = new Integer(getSplitPref(m_mainSplitter));

      if(m_dividerListener == null) {
        m_dividerListener = new DividerListener();
      }
    }

    return m_mainSplitter;
  }

  private JSplitPane getLeftSplitter() {
    if(m_leftSplitter == null) {
      m_leftSplitter = (JSplitPane)findComponent("LeftSplitter");
      m_leftDivLoc   = new Integer(getSplitPref(m_leftSplitter));

      if(m_dividerListener == null) {
        m_dividerListener = new DividerListener();
      }
    }

    return m_leftSplitter;
  }

  public void updateServerPrefs() {
    XRootNode          root        = m_tree.getRootNode();
    int                count       = root.getChildCount();
    AdminClientContext acc         = AdminClient.getContext();
    PrefsHelper        helper      = PrefsHelper.getHelper();
    Preferences        prefs       = acc.prefs.node("AdminClient");
    Preferences        serverPrefs = prefs.node(ServersHelper.SERVERS);
    Preferences        serverPref;
    ServerNode         serverNode;

    helper.clearChildren(serverPrefs);

    for(int i = 0; i < count; i++) {
      serverNode = (ServerNode)root.getChildAt(i);
      serverPref = serverPrefs.node("server-"+i);
      serverNode.setPreferences(serverPref);
    }

    storePreferences();
  }

  public void disconnectAll() {
    XRootNode  root   = m_tree.getRootNode();
    int        count  = root.getChildCount();
    ServerNode serverNode;

    for(int i = 0; i < count; i++) {
      serverNode = (ServerNode)root.getChildAt(i);

      if(serverNode.isConnected()) {
        serverNode.disconnectOnExit();
      }
    }

    root.tearDown();
    storePreferences();
  }

  class DividerListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent pce) {
      JSplitPane splitter = (JSplitPane)pce.getSource();
      String     propName = pce.getPropertyName();

      if(splitter.isShowing() == false ||
         JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(propName) == false)
      {
        return;
      }

      int         divLoc    = splitter.getDividerLocation();
      Integer     divLocObj = new Integer(divLoc);
      Preferences prefs     = getPreferences();
      String      name      = splitter.getName();
      Preferences node      = prefs.node(name);

      node.putInt("Split", divLoc);
      storePreferences();

      if(m_mainSplitter.getName().equals(name)) {
        m_mainDivLoc = divLocObj;
      }
      else {
        m_leftDivLoc = divLocObj;
      }
    }
  }

  public void doLayout() {
    super.doLayout();

    JSplitPane splitter = getMainSplitter();
    if(m_mainDivLoc != null) {
      splitter.setDividerLocation(m_mainDivLoc.intValue());
    }
    else {
      splitter.setDividerLocation(0.7);
    }

    splitter = getLeftSplitter();
    if(m_leftDivLoc != null) {
      splitter.setDividerLocation(m_leftDivLoc.intValue());
    }
    else {
      splitter.setDividerLocation(0.25);
    }
  }

  public void log(String s) {
    m_logArea.append(s+System.getProperty("line.separator"));
    m_logArea.setCaretPosition(m_logArea.getDocument().getLength()-1);
  }

  public void log(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter  pw = new PrintWriter(sw);

    e.printStackTrace(pw);
    pw.close();

    log(sw.toString());
  }

  public void setStatus(String msg) {
    m_statusLine.setText(msg);
  }

  public void clearStatus() {
    setStatus("");
  }

  public void addNotify() {
    super.addNotify();

    getMainSplitter().addPropertyChangeListener(m_dividerListener);
    getLeftSplitter().addPropertyChangeListener(m_dividerListener);

    // TODO: what's up with this in the plugin?
    //m_tree.requestFocusInWindow();
  }

  public void removeNotify() {
    getMainSplitter().removePropertyChangeListener(m_dividerListener);
    getLeftSplitter().removePropertyChangeListener(m_dividerListener);

    super.removeNotify();
  }

  class NewServerAction extends XAbstractAction {
    NewServerAction() {
      super(getBundleString("new.server.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      XTreeModel model      = (XTreeModel)m_tree.getModel();
      XTreeNode  root       = (XTreeNode)model.getRoot();
      int        index      = root.getChildCount();
      ServerNode serverNode = new ServerNode();

      model.insertNodeInto(serverNode, root, index);
      TreePath path = new TreePath(serverNode.getPath());
      m_tree.makeVisible(path);
      m_tree.setSelectionPath(path);
      
      AdminClientContext acc     = AdminClient.getContext();
      PrefsHelper        helper  = PrefsHelper.getHelper();
      Preferences        prefs   = acc.prefs.node("AdminClient");
      Preferences        servers = prefs.node(ServersHelper.SERVERS);
      int                count   = helper.childrenNames(servers).length;

      serverNode.setPreferences(servers.node("server-"+count));
      storePreferences();
    }
  }

  class QuitAction extends XAbstractAction {
    QuitAction() {
      super(getBundleString("quit.action.label"));

      setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                               MENU_SHORTCUT_KEY_MASK,
                               true));
    }

    public void actionPerformed(ActionEvent ae) {
      disconnectAll();

      // We do this because the ServerConnectionManager messages its listener
      // in a background thread, so the listener needs to invokerLater itself.
      // This ensures that work gets done, including resetting any ConnectionContexts
      // such as the ones used by the ClusterMemberList in ServerPanel.  If we don't
      // handle it this way, the process will exit resulting in messy JMX errors
      // in the log.
      
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          System.exit(0);
        }
      });
    }
  }

  private java.awt.Frame getFrame() {
    return (java.awt.Frame)
      SwingUtilities.getAncestorOfClass(java.awt.Frame.class, this);
  }
  
  protected boolean shouldAddAboutItem() {
    return true;
  }
  
  class AboutAction extends XAbstractAction {
    Dialog m_aboutDialog;
    
    AboutAction() {
      super(getBundleString("about.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      if(m_aboutDialog == null) {
        AdminClientContext acc   = AdminClient.getContext();
        java.awt.Frame     frame = getFrame();

        m_aboutDialog = frame != null ? new Dialog(frame) : new Dialog();
        m_aboutDialog.load((DialogResource)acc.topRes.child("AboutDialog"));

        AdminClientInfoPanel info;
        String title = getBundleString("title");
        info = (AdminClientInfoPanel)m_aboutDialog.findComponent("AdminClientInfoPanel");
        info.init(title, new ProductInfo());
        Label monikerLabel = (Label)m_aboutDialog.findComponent("MonikerLabel");
        monikerLabel.setText(title);
        m_aboutDialog.getContentPane().addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent me) {
            m_aboutDialog.setVisible(false);
          }
        });
      }

      m_aboutDialog.center(AdminClientPanel.this);
      m_aboutDialog.setVisible(true);
    }
  }

  public void addServerLog(ConnectionContext cc) {
    ServerLog   log      = new ServerLog(cc);
    JScrollPane scroller = new JScrollPane(log);
    int         index    = m_bottomPane.getTabCount();

    m_bottomPane.addTab(cc.toString(), (Icon)null, scroller, null);
    m_bottomPane.setSelectedIndex(index);

    LogDocumentListener ldl = new LogDocumentListener(log);
    log.getDocument().addDocumentListener(ldl);
    m_logListeners.add(ldl);
  }

  public void removeServerLog(ConnectionContext cc) {
    JScrollPane         scroller;
    ServerLog           log;
    LogDocumentListener ldl;

    for(int i = 1; i < m_bottomPane.getTabCount(); i++) {
      scroller = (JScrollPane)m_bottomPane.getComponentAt(i);
      log      = (ServerLog)scroller.getViewport().getView();

      if(cc.equals(log.getConnectionContext())) {
        ldl = (LogDocumentListener)m_logListeners.remove(i);
        log.getDocument().removeDocumentListener(ldl);
        m_bottomPane.removeTabAt(i);

        int index = m_bottomPane.getSelectedIndex();
        m_bottomPane.setIconAt(index, null);

        return;
      }
    }
  }

  class LogDocumentListener implements DocumentListener {
    JTextComponent textComponent;

    LogDocumentListener(JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    public void insertUpdate(DocumentEvent e) {
      int index = m_logListeners.indexOf(this);

      if(!textComponent.isShowing() &&
         m_bottomPane.getIconAt(index) == null)
      {
        m_bottomPane.setIconAt(index, m_infoIcon);
      }
    }

    public void removeUpdate(DocumentEvent e) {/**/}
    public void changedUpdate(DocumentEvent e) {/**/}
  }
  
  public void block() {
    /**/
  }

  public void unblock() {
    /**/
  }

  public UndoManager getUndoManager() {
    if(m_undoManager == null) {
      m_undoManager = new MyUndoManager();
    }

    return m_undoManager;
  }

  class UndoAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      UndoManager  undoMan = getUndoManager();
      UndoableEdit next    = ((MyUndoManager)undoMan).nextUndoable();

      if(next != null) {
        undoMan.undo();
        setStatus("Undid '"+next.getPresentationName()+"'");
      }
    }

    public boolean isEnabled() {
      return getUndoManager().canUndo();
    }
  }

  class RedoAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      UndoManager  undoMan = getUndoManager();
      UndoableEdit next    = ((MyUndoManager)undoMan).nextRedoable();

      if(next != null) {
        undoMan.redo();
        setStatus("Redid '"+next.getPresentationName()+"'");
      }
    }

    public boolean isEnabled() {
      return getUndoManager().canRedo();
    }
  }

  class MyUndoManager extends UndoManager {
    public UndoableEdit nextUndoable() {
      return editToBeUndone();  
    }

    public UndoableEdit nextRedoable() {
      return editToBeRedone();  
    }
  }

  public String toString() {
    return getName();
  }

  protected void addEdit(UndoableEdit edit) {
    getUndoManager().addEdit(edit);
  }
}
