/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.terracotta.ui.util.SWTUtil;
import org.terracotta.ui.util.SWTUtil.TableWeightedResizeHandler;

import com.tc.bundles.BundleSpec;
import com.tc.bundles.OSGiToMaven;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class NewAddModuleDialog extends MessageDialog {
  private final Modules              fModules;
  private Combo                      fGroupIdCombo;
  private static ArrayList<String>   fCachedGroupIds     = new ArrayList<String>();
  private Composite                  fTableHolder;
  private Table                      fTable;
  private TableWeightedResizeHandler resizeHandler;
  private final SelectionListener    fColumnSelectionListener;
  private CLabel                     fPathLabel;
  private Button                     fAddRepoButton;
  private Button                     fShowVersionsButton;
  private TableColumn                fVersionColumn;
  private ValueListener              m_valueListener;

  private static final String        GROUP_ID            = "Group Identifier:";
  private static final String        REPO                = "Repository";
  private static final String        NAME                = "Name";
  private static final String        VERSION             = "Version";

  private static final String        VERSION_PATTERN     = "^[0-9]+\\.[0-9]+\\.[0-9]+(-SNAPSHOT)?$";

  private static final int[]         VERSION_WEIGHTS     = { 2, 2, 1 };
  private static final int[]         VERSIONLESS_WEIGHTS = { 3, 3 };

  public NewAddModuleDialog(Shell parentShell, String title, String message, Modules modules) {
    super(parentShell, "Select modules", null, "Select modules to add to your configuration", MessageDialog.NONE,
          new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
    setShellStyle(SWT.DIALOG_TRIM | getDefaultOrientation() | SWT.RESIZE);
    fModules = modules != null ? (Modules) modules.copy() : Modules.Factory.newInstance();
    fColumnSelectionListener = new ColumnSelectionListener();
  }

  @Override
  protected Control createCustomArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    comp.setLayout(new GridLayout());
    comp.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite groupComp = new Composite(comp, SWT.NONE);
    groupComp.setLayout(new GridLayout(2, false));

    Label groupIdLabel = new Label(groupComp, SWT.NONE);
    groupIdLabel.setText(GROUP_ID);
    groupIdLabel.setLayoutData(new GridData());

    fGroupIdCombo = new Combo(groupComp, SWT.BORDER);
    fGroupIdCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fGroupIdCombo.add("org.terracotta.modules");
    for (String groupId : fCachedGroupIds.toArray(new String[0])) {
      fGroupIdCombo.add(groupId);
    }
    fGroupIdCombo.select(0);
    fGroupIdCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        populateTable();
      }
    });
    fGroupIdCombo.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        Shell shell = getShell();
        if (shell == null || shell.isDisposed()) return;
        String text = fGroupIdCombo.getText();
        if (fGroupIdCombo.indexOf(text) == -1) {
          fGroupIdCombo.add(text);
          fCachedGroupIds.add(text);
        }
        populateTable();
      }
    });

    fTableHolder = new Composite(comp, SWT.NONE);
    fTableHolder.setLayout(new GridLayout());
    fTableHolder.setLayoutData(new GridData(GridData.FILL_BOTH));

    createTable(fTableHolder);

    fPathLabel = new CLabel(comp, SWT.NONE);
    fPathLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Composite controlsGroup = new Composite(comp, SWT.NONE);
    controlsGroup.setLayout(new GridLayout(2, false));
    controlsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    fAddRepoButton = new Button(controlsGroup, SWT.PUSH);
    fAddRepoButton.setText("Add repository...");
    fAddRepoButton.setLayoutData(new GridData());
    fAddRepoButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
        directoryDialog.setText("Terracotta Module Repository Chooser");
        directoryDialog.setMessage("Select a module repository directory");
        String path = directoryDialog.open();
        if (path != null) {
          File dir = new File(path);
          fModules.addRepository(dir.toString());
          populateTable();
        }
      }
    });

    fShowVersionsButton = new Button(controlsGroup, SWT.CHECK);
    fShowVersionsButton.setText("Show Versions");
    fShowVersionsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        addRemoveVersionColumn();
      }
    });
    fShowVersionsButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

    return comp;
  }

  private void addRemoveVersionColumn() {
    boolean showVersions = showVersions();
    int topIndex = fTable.getTopIndex();
    int selectionIndex = fTable.getSelectionIndex();

    if (showVersions) {
      fVersionColumn = new TableColumn(fTable, SWT.NONE);
      fVersionColumn.setResizable(true);
      fVersionColumn.setText(VERSION);
      fVersionColumn.addSelectionListener(fColumnSelectionListener);
    } else {
      fVersionColumn.dispose();
      fVersionColumn = null;
    }

    packTable();
    populateTable();

    fTable.setTopIndex(topIndex);
    if (selectionIndex != -1) {
      fTable.setSelection(selectionIndex);
    }
    fTable.setFocus();
  }

  private boolean showVersions() {
    return fShowVersionsButton != null && fShowVersionsButton.getSelection();
  }

  private void createTable(Composite parent) {
    boolean showVersions = showVersions();
    fTable = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
    fTable.setHeaderVisible(true);
    fTable.setLinesVisible(true);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = SWTUtil.tableRowsToPixels(fTable, 10);
    gridData.widthHint = SWTUtil.textColumnsToPixels(fTable, 100);
    fTable.setLayoutData(gridData);

    TableColumn column0 = new TableColumn(fTable, SWT.NONE);
    column0.setResizable(true);
    column0.setText(REPO);
    column0.addSelectionListener(fColumnSelectionListener);

    TableColumn column1 = new TableColumn(fTable, SWT.NONE);
    column1.setResizable(true);
    column1.setText(NAME);
    column1.addSelectionListener(fColumnSelectionListener);

    TableColumn column2 = null;
    if (showVersions) {
      column2 = new TableColumn(fTable, SWT.NONE);
      column2.setResizable(true);
      column2.setText(VERSION);
      column2.addSelectionListener(fColumnSelectionListener);
    }

    populateTable();
    packTable();

    column0.pack();
    column1.pack();
    if (column2 != null) {
      column2.pack();
    }

    fTable.setFocus();

    fTable.addMouseMoveListener(new MouseMoveListener() {
      public void mouseMove(MouseEvent e) {
        String tip = null;
        TableItem item = fTable.getItem(new Point(e.x, e.y));
        if (item != null) {
          ItemData itemData = (ItemData) item.getData();
          tip = itemData.fArchiveFile.getAbsolutePath();
        }
        fPathLabel.setText(tip);
      }
    });
    fTable.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseExit(MouseEvent e) {
        fPathLabel.setText("");
      }
    });
  }

  private void packTable() {
    if (resizeHandler != null) {
      resizeHandler.dispose();
    }
    int[] colWeights = showVersions() ? VERSION_WEIGHTS : VERSIONLESS_WEIGHTS;
    resizeHandler = SWTUtil.makeTableColumnsResizeWeightedWidth(fTableHolder, fTable, colWeights);
  }

  private void populateTable() {
    fTable.removeAll();
    File installRoot = new File(System.getProperty("tc.install-root"));
    populateTable(new File(installRoot, "platform/modules"), "KIT");
    String[] repos = fModules.getRepositoryArray();
    if (repos != null) {
      for (String repo : repos) {
        File repoDir = null;
        if (repo.startsWith("file:")) {
          try {
            repoDir = new File(new URL(repo).getFile());
          } catch (MalformedURLException e) {/**/
          }
        } else {
          repoDir = new File(repo);
        }

        if (repoDir != null && repoDir.exists() && repoDir.isDirectory()) {
          populateTable(repoDir, null);
        }
      }
    }
  }

  private void populateTable(File repoDir, String nickname) {
    String targetGroupId = fGroupIdCombo.getText();
    File groupDir = new File(repoDir, targetGroupId.replace('.', File.separatorChar));
    File[] names = groupDir.listFiles();
    boolean showVersions = showVersions();

    if (names == null) return;

    for (File nameFile : names) {
      File[] versions = nameFile.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.matches(VERSION_PATTERN);
        }
      });
      if (versions == null) continue;
      for (File versionFile : versions) {
        TableItem item = new TableItem(fTable, SWT.NONE);
        String repo = nickname != null ? nickname : repoDir.getAbsolutePath();
        String name = nameFile.getName();
        String version = versionFile.getName();
        String[] strings = showVersions ? new String[] { repo, name, version } : new String[] { repo, name };
        item.setText(strings);
        File archiveFile = new File(versionFile, name + "-" + version + ".jar");
        item.setData(new ItemData(strings, repoDir, archiveFile));
        if (!showVersions) {
          break;
        }
      }
    }

    // Handle any modules located at the root of the repository

    File[] jarFiles = repoDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar");
      }
    });
    for (File jarFile : jarFiles) {
      Manifest manifest = getManifest(jarFile);
      if (manifest != null) {
        String version = BundleSpec.getVersion(manifest);
        String symbolicName = BundleSpec.getSymbolicName(manifest);

        if (symbolicName != null && version != null) {
          String bundleGroupId = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
          if (!targetGroupId.equals(bundleGroupId)) {
            if (fGroupIdCombo.indexOf(bundleGroupId) == -1) {
              fGroupIdCombo.add(bundleGroupId);
            }
            return;
          }

          TableItem item = new TableItem(fTable, SWT.NONE);
          String repo = nickname != null ? nickname : repoDir.getAbsolutePath();
          String[] strings;
          if (showVersions) {
            strings = new String[] { repo, OSGiToMaven.artifactIdFromSymbolicName(symbolicName),
                OSGiToMaven.bundleVersionToProjectVersion(version) };
          } else {
            strings = new String[] { repo, OSGiToMaven.artifactIdFromSymbolicName(symbolicName) };
          }
          item.setText(strings);
          item.setData(new ItemData(strings, repoDir, jarFile));
        }
      }
    }
  }

  private static Manifest getManifest(final File file) {
    try {
      return getManifest(file.toURI().toURL());
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private static Manifest getManifest(final URL location) {
    try {
      final JarFile bundle = new JarFile(FileUtils.toFile(location));
      return bundle.getManifest();
    } catch (IOException e) {
      return null;
    }
  }

  class ColumnSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      TableColumn col = (TableColumn) e.widget;
      switch (fTable.getSortDirection()) {
        case SWT.DOWN:
          fTable.setSortDirection(SWT.UP);
          break;
        case SWT.UP:
        default:
          fTable.setSortDirection(SWT.DOWN);
      }
      fTable.setSortColumn(col);
      sort();
    }
  }

  void sort() {
    int itemCount = fTable.getItemCount();
    if (itemCount == 0 || itemCount == 1) return;
    Comparator<ItemData> comparator = new Comparator<ItemData>() {
      final int         sortDirection = fTable.getSortDirection();
      final TableColumn sortColumn    = fTable.getSortColumn();
      int               index         = sortColumn == null ? 0 : fTable.indexOf(sortColumn);

      public int compare(ItemData itemData1, ItemData itemData2) {
        if (sortDirection == SWT.UP || sortDirection == SWT.NONE) {
          return itemData1.fStrings[index].compareTo(itemData2.fStrings[index]);
        } else {
          return itemData2.fStrings[index].compareTo(itemData1.fStrings[index]);
        }
      }
    };
    ArrayList<ItemData> selection = new ArrayList<ItemData>();
    for (TableItem item : fTable.getSelection()) {
      selection.add((ItemData) item.getData());
    }
    ItemData[] data = new ItemData[fTable.getItemCount()];
    for (int i = 0; i < fTable.getItemCount(); i++) {
      data[i] = (ItemData) (fTable.getItem(i).getData());
    }
    Arrays.sort(data, 0, itemCount, comparator);
    fTable.setRedraw(false);
    try {
      fTable.deselectAll();
      for (int i = 0; i < fTable.getItemCount(); i++) {
        TableItem item = fTable.getItem(i);
        ItemData itemData = data[i];
        item.setText(itemData.fStrings);
        item.setData(itemData);
        if (selection.contains(itemData)) {
          fTable.select(i);
        }
      }
    } finally {
      fTable.setRedraw(true);
    }
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      boolean showVersions = showVersions();
      TableItem[] items = fTable.getSelection();
      String groupId = fGroupIdCombo.getText();

      for (TableItem item : items) {
        Module module = fModules.addNewModule();
        module.setGroupId(groupId);
        module.setName(item.getText(1));
        if (showVersions) {
          module.setVersion(item.getText(2));
        }
      }
      if (m_valueListener != null) {
        m_valueListener.setValue(fModules);
      }
    }
    super.buttonPressed(buttonId);
  }

  class ItemData {
    String[] fStrings;
    File     fRepoDir;
    File     fArchiveFile;

    ItemData(String[] strings, File repoDir, File moduleFile) {
      fStrings = strings;
      fRepoDir = repoDir;
      fArchiveFile = moduleFile;
    }
  }

  public void addValueListener(ValueListener listener) {
    this.m_valueListener = listener;
  }

  // --------------------------------------------------------------------------------

  public static interface ValueListener {
    void setValue(Modules modules);
  }
}
