package org.terracotta.dso.views;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.terracotta.dso.TcPlugin;

class ConfigLabelProvider extends LabelProvider {
  private ConfigViewer fViewer;
  private static Image m_rootsImage;
  private static Image m_bootClassesImage;
  private static Image m_distributedMethodsImage;
  private static Image m_instrumentedClassesImage;
  private static Image m_locksImage;
  private static Image m_transientsImage;
  private static Image m_missingElementImage;
  
  private static JavaElementLabelProvider m_javaLabelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS);

  static {
    m_rootsImage = TcPlugin.createImage("/images/eclipse/roots.gif");
    m_locksImage = TcPlugin.createImage("/images/eclipse/locks.gif");
    m_transientsImage = TcPlugin.createImage("/images/eclipse/transients.gif");
    m_bootClassesImage = TcPlugin.createImage("/images/eclipse/jar_obj.gif");
    m_distributedMethodsImage = TcPlugin.createImage("/images/eclipse/jmeth_obj.gif");
    m_instrumentedClassesImage = TcPlugin.createImage("/images/eclipse/class_obj.gif");
    m_missingElementImage = TcPlugin.createImage("/images/eclipse/hprio_tsk.gif");
  }
    
  ConfigLabelProvider(ConfigViewer viewer) {
    super();
    fViewer = viewer;
  }
  
  public String getText(Object element) {
    if(element instanceof RootsWrapper) {
      return "Roots";
    } else if(element instanceof InstrumentedClassesWrapper) {
      return "Instrumented classes";
    } else if(element instanceof LocksWrapper) {
      return "Locks";
    } else if(element instanceof DistributedMethodsWrapper) {
      return "Distributed methods";
    } else if(element instanceof AdditionalBootJarClassesWrapper) {
      return "Additional boot classes";
    } else if(element instanceof TransientFieldsWrapper) {
      return "Transient fields";
    }     
    return super.getText(element);
  }

  public Image getImage(Object element) {
    if(element instanceof RootWrapper) {
      RootWrapper wrapper = (RootWrapper)element;
      if(wrapper.isSetFieldName()) {
        return getJavaElementImage(fViewer.getPart().getField(wrapper.getFieldName()));
      } else if(wrapper.isSetFieldExpression()) {
        return null;
      }
    } else if(element instanceof RootsWrapper) {
      return m_rootsImage;
    } else if(element instanceof LocksWrapper) {
      return m_locksImage;
    } else if(element instanceof DistributedMethodsWrapper) {
      return m_distributedMethodsImage;
    } else if(element instanceof AdditionalBootJarClassesWrapper) {
      return m_bootClassesImage;
    } else if(element instanceof TransientFieldsWrapper) {
      return m_transientsImage;
    } else if(element instanceof InstrumentedClassesWrapper) {
      return m_instrumentedClassesImage;
    } else if(element instanceof BootClassWrapper) {
      String className = ((BootClassWrapper)element).getClassName();
      return getJavaElementImage(fViewer.getPart().getType(className));
    } else if(element instanceof TransientFieldWrapper) {
      String fieldName = ((TransientFieldWrapper)element).getFieldName();
      return getJavaElementImage(fViewer.getPart().getField(fieldName));
    }
    
    return null;
  }
  
  private Image getJavaElementImage(IJavaElement element) {
    if(element != null) {
      return m_javaLabelProvider.getImage(element);
    }
    return m_missingElementImage;
  }
}
