/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.terracotta.dso.JdtUtils;
import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.util.StackElementInfo;
import org.terracotta.dso.views.ConfigUI;
import org.terracotta.ui.util.SWTUtil;

import com.tc.admin.common.AbstractResolutionAction;
import com.tc.admin.common.AbstractWorkState;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.NonPortableMessages;
import com.tc.admin.common.NonPortableWorkState;
import com.tc.object.appevent.AbstractLockEvent;
import com.tc.object.appevent.AbstractLockEventContext;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.Include;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.NamedLock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class AbstractLockDialog extends AbstractApplicationEventDialog {
  private AbstractWorkState      fWorkState;
  private List<StackElementInfo> fStackElementInfos;
  private NonPortableWorkState   fTargetObjectState;
  private Table                  fStackTable;
  private LockRulePanel          fLockRuleView;

  protected static final Image   WRITE_ICON             = TcPlugin.createImage("/images/eclipse/occ_write.gif"); //$NON-NLS-1$
  protected static final Image   READ_ICON              = TcPlugin.createImage("/images/eclipse/occ_read.gif"); //$NON-NLS-1$
  protected static final Image   CONCURRENT_ICON        = TcPlugin.createImage("/images/eclipse/occ_write.gif"); //$NON-NLS-1$
  protected static final Image   SYNCHRONOUS_WRITE_ICON = TcPlugin.createImage("/images/eclipse/sync_write.gif"); //$NON-NLS-1$

  public AbstractLockDialog(Shell parentShell, String title, AbstractLockEvent lockEvent) {
    super(parentShell, title, lockEvent, new String[] { //$NON-NLS-1$
        IDialogConstants.CANCEL_LABEL, IDialogConstants.OK_LABEL, //$NON-NLS-1$
        }, 1);
    fWorkState = createWorkState(lockEvent);
    populateStackElementInfos();
  }

  protected abstract AbstractWorkState createWorkState(AbstractLockEvent lockEvent);

  protected AbstractWorkState getWorkState() {
    return fWorkState;
  }

  protected abstract String getIssueName();

  private AbstractLockEvent getAbstractLockEvent() {
    return (AbstractLockEvent) getApplicationEvent();
  }

  private AbstractLockEventContext getAbstractLockEventContext() {
    return getAbstractLockEvent().getAbstractLockEventContext();
  }

  private void populateStackElementInfos() {
    StackTraceElement[] elements = getAbstractLockEventContext().getStackElements();
    ArrayList<StackTraceElement> list = new ArrayList<StackTraceElement>();

    for (int i = 0; i < elements.length; i++) {
      StackTraceElement element = elements[i];
      if (element.getMethodName().startsWith("__tc_wrapped")) {
        int nextIndex = i + 1;
        StackTraceElement nextElement = elements[nextIndex];
        if (nextElement.getLineNumber() < 0 && element.getLineNumber() >= 1) {
          try {
            Field[] fields = nextElement.getClass().getDeclaredFields();
            for (Field f : fields) {
              if (f.getName().equals("lineNumber")) {
                f.setAccessible(true);
                f.setInt(nextElement, element.getLineNumber());
                break;
              }
            }
          } catch (Exception e) {/**/
          }
        }
      } else {
        list.add(element);
      }
    }

    int startIndex = 0;
    for (int i = list.size() - 1; i >= 0; i--) {
      StackTraceElement element = list.get(i);
      if (element.getMethodName().startsWith("__tc_") || element.getLineNumber() < 0) {
        startIndex = i + 1;
        break;
      }
    }

    fStackElementInfos = new ArrayList<StackElementInfo>();
    for (int i = startIndex; i < list.size(); i++) {
      fStackElementInfos.add(new StackElementInfo(list.get(i)));
    }
  }

  protected void createIssueDescriptionArea(Composite parent) {
    fStackTable = new Table(parent, SWT.V_SCROLL | SWT.BORDER | SWT.READ_ONLY);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.widthHint = SWTUtil.textColumnsToPixels(fStackTable, 80);
    gridData.heightHint = SWTUtil.textRowsToPixels(fStackTable, 8);
    fStackTable.setLayoutData(gridData);
  }

  protected Control createCustomArea(Composite parent) {
    Control customArea = super.createCustomArea(parent);

    fLockRuleView = new LockRulePanel(fActionPanel);

    populateStackTable();

    fSummaryLabel.setText(fTargetObjectState != null ? fTargetObjectState.summary() : "Thread stack");
    fSummaryLabel.setImage(fTargetObjectState != null ? imageFor(fTargetObjectState) : null);

    fActionTreeViewer.setInput(fWorkState);
    fActionTreeViewer.expandAll();

    return customArea;
  }

  protected boolean anySelectedActions() {
    return fWorkState.hasSelectedActions();
  }

  protected void initIssueList() {
    TreeItem treeItem = fObjectTree.getItem(0);

    fIssueTable.setRedraw(false);
    try {
      fIssueTable.setItemCount(0);
      TableItem tableItem = new TableItem(fIssueTable, SWT.NONE);

      tableItem.setData(treeItem);
      tableItem.setImage(anyActionSelected() ? RESOLVED_ICON : BLANK_ICON);
      tableItem.setText(getIssueName());
      fIssueTable.setSelection(0);
    } finally {
      fIssueTable.setRedraw(true);
    }
  }

  protected void handleTreeSelectionChange() {
  // The object browser isn't tied to any actions directly, it's just for context
  // and for browsing to the field's source defintion.
  }

  protected void apply() {
    for (AbstractResolutionAction action : fWorkState.getActions()) {
      if (action.isSelected()) action.apply();
    }

    super.apply();
  }

  protected void initTreeItem(TreeItem item, DefaultMutableTreeNode node) {
    super.initTreeItem(item, node);

    Object userObject = node.getUserObject();
    if (userObject instanceof NonPortableWorkState) {
      NonPortableWorkState workState = (NonPortableWorkState) userObject;

      String fieldName = getAbstractLockEventContext().getFieldName();
      if (fieldName != null && fieldName.equals(workState.getFieldName())) {
        item.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_YELLOW));
        fObjectTree.showItem(item);
        fTargetObjectState = workState;
      }
    }
  }

  void populateStackTable() {
    Iterator<StackElementInfo> iter = fStackElementInfos.iterator();

    while (iter.hasNext()) {
      TableItem item = new TableItem(fStackTable, SWT.NONE);
      StackElementInfo stackElemInfo = iter.next();
      StackTraceElement stackElement = stackElemInfo.getStackElement();
      Image image = BLANK_ICON;
      String typeName = stackElement.getClassName();
      int lineNumber = stackElement.getLineNumber() - 1;

      try {
        IType type = JdtUtils.findType(fJavaProject, typeName);

        if (type != null) {
          IRegion region = null;
          IMethod method = null;

          stackElemInfo.setType(type);
          IMethod[] methods = JdtUtils.findMethods(type, stackElement.getMethodName());
          if (methods.length == 1) method = methods[0];

          IBuffer buffer;
          if (lineNumber >= 1 && (buffer = getBufferForMember(type)) != null) {
            Document document = new Document(buffer.getContents());

            try {
              region = document.getLineInformation(lineNumber);
            } catch (BadLocationException e) {
              TcPlugin.getDefault().openError("Type ("+type+") lineNumber("+lineNumber+")", e);
            }
          } else if (method != null) {
            ISourceRange srcRange = method.getNameRange();
            if (srcRange != null) {
              region = new Region(srcRange.getOffset(), srcRange.getLength());
            }
          }

          if (region != null) {
            stackElemInfo.setRegion(region);
            if (method == null) {
              ITypeRoot typeRoot = type.getTypeRoot();
              IJavaElement elem = typeRoot.getElementAt(region.getOffset());
              if (elem instanceof IMethod) {
                method = (IMethod) elem;
              }
            }
          }

          if (method != null) {
            XmlObject lock = fConfigHelper.getLock(method);
            if (lock != null) {
              LockLevel.Enum level;
              if (lock instanceof Autolock) {
                Autolock autolock = (Autolock) lock;
                level = autolock.getLockLevel();
              } else {
                NamedLock namedLock = (NamedLock) lock;
                level = namedLock.getLockLevel();
              }
              if (level == LockLevel.WRITE) image = WRITE_ICON;
              else if (level == LockLevel.READ) image = READ_ICON;
              else if (level == LockLevel.CONCURRENT) image = CONCURRENT_ICON;
              else if (level == LockLevel.SYNCHRONOUS_WRITE) image = SYNCHRONOUS_WRITE_ICON;
            }
            stackElemInfo.setMethod(method);
          }
        }
      } catch (JavaModelException jme) {
        TcPlugin.getDefault().openError("stackElementInfo("+stackElemInfo+")", jme);
      }

      item.setImage(image);
      item.setText(stackElemInfo.toString());
    }

    fStackTable.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent e) {
        e.widget.getDisplay().asyncExec(new Runnable() {
          public void run() {
            handleStackElementSelectionChange(fStackTable.getSelectionIndex());
          }
        });
      }
    });
  }

  protected void handleStackElementSelectionChange(int frameElementIndex) {
    if (frameElementIndex >= 0) {
      StackElementInfo stackElementInfo = fStackElementInfos.get(frameElementIndex);
      IType type = stackElementInfo.getType();

      if (type != null) {
        IRegion region = stackElementInfo.getRegion();

        if (region != null) {
          ConfigUI.jumpToRegion(type, region);
        }
      }
    }
  }

  private IBuffer getBufferForMember(IJavaElement member) {
    IBuffer buffer = null;
    try {
      IOpenable openable = member.getOpenable();
      if (openable != null && member.exists()) {
        buffer = openable.getBuffer();
      }
    } catch (JavaModelException e) {
      TcPlugin.getDefault().openError("member("+member+")", e);
    }
    return buffer;
  }

  protected Iterator<StackElementInfo> createStackElementInfoIterator() {
    return fStackElementInfos.iterator();
  }

  protected StackElementInfo getStackElementInfo(int index) {
    return fStackElementInfos.get(index);
  }

  protected AbstractResolutionAction[] createActions(AbstractWorkState workState) {
    ArrayList list = new ArrayList();
    String fieldName = getAbstractLockEventContext().getFieldName();

    if (fieldName != null && !fConfigHelper.isTransient(fieldName)) {
      list.add(new MakeTransientAction());
    }

    return (AbstractResolutionAction[]) list.toArray(new AbstractResolutionAction[0]);
  }

  protected void updateButtons() {
    getButton(IDialogConstants.OK_ID).setEnabled(anyActionSelected());
  }

  class MakeTransientAction extends AbstractResolutionAction {
    public void showControl(Object parentControl) {
      String fieldName = getAbstractLockEventContext().getFieldName();
      String declaringType = fieldName.substring(0, fieldName.lastIndexOf('.'));
      Include include = fConfigHelper.includeRuleFor(declaringType);

      if (include != null) {
        fActionStackLayout.topControl = fIncludeRuleView;
        fIncludeRuleView.setInclude(include);
        fActionPanel.layout();
        fActionPanel.redraw();
      }
    }

    public String getText() {
      return NonPortableMessages.getString("DO_NOT_SHARE"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      String fieldName = getAbstractLockEventContext().getFieldName();
      if (selected) {
        fConfigHelper.ensureTransient(fieldName, NULL_SIGNALLER);
      } else {
        fConfigHelper.ensureNotTransient(fieldName, NULL_SIGNALLER);
      }
    }
  }

  class AddLockAction extends AbstractResolutionAction {
    public void showControl(Object parentControl) {
      fActionStackLayout.topControl = fLockRuleView;
      fActionPanel.layout();
      fActionPanel.redraw();
    }

    public String getText() {
      return "Add lock"; //$NON-NLS-1$
    }

    public void apply() {
      IMethod method = fLockRuleView.getMethod();
      LockLevel.Enum level = fLockRuleView.getLockLevel();
      String expr;

      try {
        expr = PatternHelper.getJavadocSignature(method);
      } catch (Exception e) {
        StackTraceElement stackElement = fLockRuleView.getStackElement();
        expr = "* " + stackElement.getClassName() + "." + stackElement.getMethodName() + "(..)";
      }

      if (fLockRuleView.isAuto()) {
        Autolock lock = fConfigHelper.addNewAutolock(expr, level, NULL_SIGNALLER);
        lock.setAutoSynchronized(fLockRuleView.isAutoSynchronized());
      } else {
        fConfigHelper.addNewNamedLock(fLockRuleView.getLockName(), expr, level, NULL_SIGNALLER);
      }
    }
  }

  class LockRulePanel extends Composite {
    Combo            fStackCombo;

    Button           fTypeAutoButton;
    Button           fTypeNamedButton;
    SelectionAdapter fTypeListener;

    Button           fLevelReadButton;
    Button           fLevelWriteButton;
    Button           fLevelConcurrentButton;
    Button           fLevelSyncWriteButton;
    SelectionAdapter fLevelListener;

    Composite        fTypeSpecificPanel;
    StackLayout      fTypeSpecificStackLayout;
    Button           fAutoSyncButton;
    Composite        fNamePanel;
    Text             fNameText;

    LockRulePanel(Composite parent) {
      super(parent, SWT.NONE);
      setLayout(new GridLayout());
      setLayoutData(new GridData(GridData.FILL_BOTH));
      Composite methodChooser = new Composite(this, SWT.NONE);
      methodChooser.setLayout(new GridLayout(2, false));
      methodChooser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      Label label = new Label(methodChooser, SWT.NONE);
      label.setText("Method"); //$NON-NLS-1$
      label.setLayoutData(new GridData());
      fStackCombo = new Combo(methodChooser, SWT.BORDER | SWT.READ_ONLY);
      fStackCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fStackCombo.setFont(parent.getFont());
      fStackCombo.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          getDisplay().asyncExec(new Runnable() {
            public void run() {
              handleStackElementSelectionChange(fStackCombo.getSelectionIndex());
            }
          });
        }
      });
      populateStackCombo();

      fTypeListener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent event) {
          if (event.widget == fTypeAutoButton) {
            fTypeSpecificStackLayout.topControl = fAutoSyncButton;
            fTypeNamedButton.setSelection(false);
          } else {
            fTypeSpecificStackLayout.topControl = fNamePanel;
            fTypeAutoButton.setSelection(false);
          }
          fTypeSpecificPanel.layout();
          fTypeSpecificPanel.redraw();
        }
      };

      Composite detailsGroup = new Composite(this, SWT.NONE);
      detailsGroup.setLayout(new GridLayout(3, false));
      detailsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      Group typeGroup = new Group(detailsGroup, SWT.SHADOW_NONE);
      typeGroup.setLayout(new GridLayout());
      typeGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
      typeGroup.setText("Lock type");
      Composite autoGroup = new Composite(typeGroup, SWT.NONE);
      GridLayout autoGroupLayout = new GridLayout(2, false);
      autoGroupLayout.marginWidth = autoGroupLayout.marginHeight = 0;
      autoGroup.setLayout(autoGroupLayout);
      autoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fTypeAutoButton = new Button(autoGroup, SWT.RADIO);
      fTypeAutoButton.setLayoutData(new GridData());
      fTypeAutoButton.setText("Auto");
      fTypeAutoButton.setSelection(true);
      fTypeAutoButton.addSelectionListener(fTypeListener);
      ImageHyperlink autoHelpLink = getFormToolkit().createImageHyperlink(autoGroup, SWT.NONE);
      autoHelpLink.setImage(HELP_ICON);
      autoHelpLink.addHyperlinkListener(new HyperlinkAdapter() {
        public void linkActivated(HyperlinkEvent e) {
          BrowserLauncher
              .openURL("http://www.terracotta.org/confluence/display/docs1/Concept+and+Architecture+Guide#ConceptandArchitectureGuide-Locks");
        }
      });
      autoHelpLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

      Composite namedGroup = new Composite(typeGroup, SWT.NONE);
      GridLayout namedGroupLayout = new GridLayout(2, false);
      namedGroupLayout.marginWidth = autoGroupLayout.marginHeight = 0;
      namedGroup.setLayout(autoGroupLayout);
      namedGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fTypeNamedButton = new Button(namedGroup, SWT.RADIO);
      fTypeNamedButton.setLayoutData(new GridData());
      fTypeNamedButton.setText("Named");
      fTypeNamedButton.addSelectionListener(fTypeListener);
      ImageHyperlink namedHelpLink = getFormToolkit().createImageHyperlink(namedGroup, SWT.NONE);
      namedHelpLink.setImage(HELP_ICON);
      namedHelpLink.addHyperlinkListener(new HyperlinkAdapter() {
        public void linkActivated(HyperlinkEvent e) {
          BrowserLauncher
              .openURL("http://www.terracotta.org/confluence/display/docs1/Concept+and+Architecture+Guide#ConceptandArchitectureGuide-Locks");
        }
      });
      namedHelpLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

      Group levelGroup = new Group(detailsGroup, SWT.SHADOW_NONE);
      levelGroup.setLayout(new GridLayout());
      levelGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
      levelGroup.setText("Lock level");
      fLevelReadButton = new Button(levelGroup, SWT.RADIO);
      fLevelReadButton.setLayoutData(new GridData());
      fLevelReadButton.setText("Read");
      fLevelWriteButton = new Button(levelGroup, SWT.RADIO);
      fLevelWriteButton.setLayoutData(new GridData());
      fLevelWriteButton.setText("Write");
      fLevelWriteButton.setSelection(true);
      fLevelConcurrentButton = new Button(levelGroup, SWT.RADIO);
      fLevelConcurrentButton.setLayoutData(new GridData());
      fLevelConcurrentButton.setText("Concurrent");
      fLevelSyncWriteButton = new Button(levelGroup, SWT.RADIO);
      fLevelSyncWriteButton.setLayoutData(new GridData());
      fLevelSyncWriteButton.setText("Synchronous-write");

      fTypeSpecificPanel = new Composite(detailsGroup, SWT.NONE);
      fTypeSpecificPanel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      fTypeSpecificPanel.setLayout(fTypeSpecificStackLayout = new StackLayout());
      fAutoSyncButton = new Button(fTypeSpecificPanel, SWT.CHECK);
      fAutoSyncButton.setLayoutData(new GridData());
      fAutoSyncButton.setText("Auto-synchronize");
      fNamePanel = new Composite(fTypeSpecificPanel, SWT.NONE);
      fNamePanel.setLayout(new GridLayout(2, false));
      fNamePanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      Label nameLabel = new Label(fNamePanel, SWT.NONE);
      nameLabel.setLayoutData(new GridData());
      nameLabel.setText("Lock name:");
      fNameText = new Text(fNamePanel, SWT.BORDER);
      fNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      fTypeSpecificStackLayout.topControl = fAutoSyncButton;
    }

    void populateStackCombo() {
      fStackCombo.removeAll();
      Iterator iter = fStackElementInfos.iterator();
      while (iter.hasNext()) {
        fStackCombo.add(iter.next().toString());
      }
      fStackCombo.select(0);
    }

    StackTraceElement getStackElement() {
      return fStackElementInfos.get(fStackCombo.getSelectionIndex()).getStackElement();
    }

    IMethod getMethod() {
      return fStackElementInfos.get(fStackCombo.getSelectionIndex()).getMethod();
    }

    boolean isAuto() {
      return fTypeAutoButton.getSelection();
    }

    String getLockName() {
      return fNameText.getText();
    }

    boolean isAutoSynchronized() {
      return fAutoSyncButton.getSelection();
    }

    LockLevel.Enum getLockLevel() {
      if (fLevelReadButton.getSelection()) return LockLevel.READ;
      else if (fLevelWriteButton.getSelection()) return LockLevel.WRITE;
      else if (fLevelConcurrentButton.getSelection()) return LockLevel.CONCURRENT;
      else return LockLevel.SYNCHRONOUS_WRITE;
    }
  }
}
