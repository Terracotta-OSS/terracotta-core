/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.util.runtime.Vm;

public class StandardConfigConfigurator
      extends TerracottaConfiguratorModule {

   private StandardDSOClientConfigHelper configHelper;

   protected void addInstrumentation(final BundleContext context,
         final StandardDSOClientConfigHelper configHelper) {
      super.addInstrumentation(context, configHelper);
      this.configHelper = configHelper;
      configSwingModels();
      configAwtModels();
   }

   private void configSwingModels() {
      // TableModelEvent
      configHelper.addIncludePattern("javax.swing.event.TableModelEvent", true);
      getOrCreateSpec("javax.swing.event.TableModelEvent");

      // AbstractTableModel
      configHelper.addIncludePattern("javax.swing.table.AbstractTableModel",
            true);
      TransparencyClassSpec spec = getOrCreateSpec("javax.swing.table.AbstractTableModel");
      spec.addDistributedMethodCall("fireTableChanged",
            "(Ljavax/swing/event/TableModelEvent;)V", false);
      spec.addTransient("listenerList");

      // DefaultTableModel
      spec = getOrCreateSpec("javax.swing.table.DefaultTableModel");
      spec.setCallConstructorOnLoad(true);
      LockDefinition ld = configHelper.createLockDefinition(
            "tcdefaultTableLock", ConfigLockLevel.WRITE);
      ld.commit();
      addLock("* javax.swing.table.DefaultTableModel.set*(..)", ld);
      addLock("* javax.swing.table.DefaultTableModel.insert*(..)", ld);
      addLock("* javax.swing.table.DefaultTableModel.move*(..)", ld);
      addLock("* javax.swing.table.DefaultTableModel.remove*(..)", ld);
      ld = configHelper.createLockDefinition("tcdefaultTableLock",
            ConfigLockLevel.READ);
      ld.commit();
      addLock("* javax.swing.table.DefaultTableModel.get*(..)", ld);

      // DefaultListModel
      spec = getOrCreateSpec("javax.swing.DefaultListModel");
      spec.setCallConstructorOnLoad(true);
      ld = configHelper.createLockDefinition("tcdefaultListLock",
            ConfigLockLevel.WRITE);
      ld.commit();
      addLock("* javax.swing.DefaultListModel.*(..)", ld);

      // TreePath
      configHelper.addIncludePattern("javax.swing.tree.TreePath", false);
      getOrCreateSpec("javax.swing.tree.TreePath");

      // DefaultMutableTreeNode
      configHelper.addIncludePattern("javax.swing.tree.DefaultMutableTreeNode",
            false);
      getOrCreateSpec("javax.swing.tree.DefaultMutableTreeNode");

      // DefaultTreeModel
      spec = getOrCreateSpec("javax.swing.tree.DefaultTreeModel");
      ld = configHelper.createLockDefinition("tcdefaultTreeLock",
            ConfigLockLevel.WRITE);
      ld.commit();
      addLock("* javax.swing.tree.DefaultTreeModel.get*(..)", ld);
      addLock("* javax.swing.tree.DefaultTreeModel.set*(..)", ld);
      addLock("* javax.swing.tree.DefaultTreeModel.insert*(..)", ld);
      spec.addTransient("listenerList");
      spec.addDistributedMethodCall("fireTreeNodesChanged",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeNodesInserted",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeNodesRemoved",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeStructureChanged",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeStructureChanged",
            "(Ljava/lang/Object;Ljavax/swing/tree/TreePath;)V", false);

      // AbstractListModel
      spec = getOrCreateSpec("javax.swing.AbstractListModel");
      spec.addTransient("listenerList");
      spec.addDistributedMethodCall("fireContentsChanged",
            "(Ljava/lang/Object;II)V", false);
      spec.addDistributedMethodCall("fireIntervalAdded",
            "(Ljava/lang/Object;II)V", false);
      spec.addDistributedMethodCall("fireIntervalRemoved",
            "(Ljava/lang/Object;II)V", false);
   }

   private void configAwtModels() {
      // Color
      configHelper.addIncludePattern("java.awt.Color", true);
      TransparencyClassSpec spec = getOrCreateSpec("java.awt.Color");
      spec.addTransient("cs");

      // MouseMotionAdapter, MouseAdapter
      getOrCreateSpec("java.awt.event.MouseMotionAdapter");
      getOrCreateSpec("java.awt.event.MouseAdapter");

      // Point
      getOrCreateSpec("java.awt.Point");
      getOrCreateSpec("java.awt.geom.Point2D");
      getOrCreateSpec("java.awt.geom.Point2D$Double");
      getOrCreateSpec("java.awt.geom.Point2D$Float");

      // Line
      getOrCreateSpec("java.awt.geom.Line2D");
      getOrCreateSpec("java.awt.geom.Line2D$Double");
      getOrCreateSpec("java.awt.geom.Line2D$Float");

      // Rectangle
      getOrCreateSpec("java.awt.Rectangle");
      getOrCreateSpec("java.awt.geom.Rectangle2D");
      getOrCreateSpec("java.awt.geom.RectangularShape");
      getOrCreateSpec("java.awt.geom.Rectangle2D$Double");
      getOrCreateSpec("java.awt.geom.Rectangle2D$Float");
      getOrCreateSpec("java.awt.geom.RoundRectangle2D");
      getOrCreateSpec("java.awt.geom.RoundRectangle2D$Double");
      getOrCreateSpec("java.awt.geom.RoundRectangle2D$Float");

      // Ellipse2D
      getOrCreateSpec("java.awt.geom.Ellipse2D");
      getOrCreateSpec("java.awt.geom.Ellipse2D$Double");
      getOrCreateSpec("java.awt.geom.Ellipse2D$Float");

      // java.awt.geom.Path2D
      if (Vm.isJDK16Compliant()) {
         getOrCreateSpec("java.awt.geom.Path2D");
         getOrCreateSpec("java.awt.geom.Path2D$Double");
         getOrCreateSpec("java.awt.geom.Path2D$Float");
      }

      // GeneralPath
      getOrCreateSpec("java.awt.geom.GeneralPath");
      // 
      // BasicStroke
      getOrCreateSpec("java.awt.BasicStroke");

      // Dimension
      getOrCreateSpec("java.awt.Dimension");
      getOrCreateSpec("java.awt.geom.Dimension2D");
   }

   private TransparencyClassSpec getOrCreateSpec(final String expr) {
      final TransparencyClassSpec spec = configHelper.getOrCreateSpec(expr);
      spec.markPreInstrumented();
      return spec;
   }

   private synchronized void addLock(final String expr, final LockDefinition ld) {
      configHelper.addLock(expr, ld);
   }

}
