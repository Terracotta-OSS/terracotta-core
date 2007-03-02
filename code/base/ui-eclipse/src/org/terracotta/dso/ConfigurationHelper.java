/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.terracotta.dso.editors.ConfigurationEditor;

import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Application;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.MethodNameExpression;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Utility singleton for use by the various popup actions in
 * org.terracotta.dso.popup.actions.
 * 
 * @see org.terracotta.dso.actions.AdaptableAction
 * @see org.terracotta.dso.actions.AutolockAction
 * @see org.terracotta.dso.actions.BootJarTypeAction
 * @see org.terracotta.dso.actions.DistributedMethodAction
 * @see org.terracotta.dso.actions.ExcludedTypeAction
 * @see org.terracotta.dso.actions.NameLockedAction
 * @see org.terracotta.dso.actions.RootFieldAction
 * @see org.terracotta.dso.actions.TransientFieldAction
 */

public class ConfigurationHelper {
  private TcPlugin      m_plugin;
  private IProject      m_project;
  private IJavaProject  m_javaProject;
  private PatternHelper m_patternHelper;

  public ConfigurationHelper(IProject project) {
    m_plugin        = TcPlugin.getDefault();    
    m_project       = project;
    m_javaProject   = JavaCore.create(m_project);
    m_patternHelper = PatternHelper.getHelper();
  }
  
  public boolean isAdaptable(IJavaElement element) {
    if(element instanceof ICompilationUnit) {
      return isAdaptable((ICompilationUnit)element);
    }
    else if(element instanceof IClassFile) {
      return isAdaptable((IClassFile)element);
    }
    else if(element instanceof IType) {
      return isAdaptable((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      return isAdaptable((IPackageDeclaration)element);
    }
    else if(element instanceof IPackageFragment) {
      return isAdaptable((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      return isAdaptable((IJavaProject)element);
    }
    
    return false;
  }
  
  public boolean isAdaptable(ICompilationUnit module) {
    return isAdaptable(module.findPrimaryType());
  }

  public boolean isAdaptable(IClassFile classFile) {
    try {
      return isAdaptable(classFile.getType());
    } catch(JavaModelException jme) {
      return false;
    }
  }

  public boolean isAdaptable(IType type) {
    if(type != null) {
      return m_plugin.isBootClass(type) ||
             isAdaptable(PatternHelper.getFullyQualifiedName(type)); 
    }
    return false;
  }
  
  public boolean isAdaptable(IPackageDeclaration packageDecl) {
    TcConfig config = getConfig();
    
    if(config != null) {
      InstrumentedClasses classes = getInstrumentedClasses();
      
      if(classes != null) {
        XmlObject[] objects = classes.selectPath("*");
        
        if(objects != null && objects.length > 0) {
          XmlObject object;
          String    expr;
          
          for(int i = objects.length-1; i >= 0; i--) {
            object = objects[i];
            
            if(object instanceof Include) {
              expr = ((Include)object).getClassExpression();
              
              if(m_patternHelper.matchesPackageDeclaration(expr, packageDecl)) {
                return true;
              }
            }
            else if(object instanceof ClassExpression) {
              expr = ((ClassExpression)object).getStringValue();
              
              if(m_patternHelper.matchesPackageDeclaration(expr, packageDecl)) {
                return false;
              }
            }
          }
        }
      }
    }
    
    return false;
  }
  
  public boolean isAdaptable(IPackageFragment fragment) {
    TcConfig config = getConfig();
    
    if(config != null) {
      InstrumentedClasses classes = getInstrumentedClasses();
      
      if(classes != null) {
        XmlObject[] objects = classes.selectPath("*");
        
        if(objects != null && objects.length > 0) {
          XmlObject object;
          String    expr;
          
          for(int i = objects.length-1; i >= 0; i--) {
            object = objects[i];
            
            if(object instanceof Include) {
              expr = ((Include)object).getClassExpression();
              
              if(m_patternHelper.matchesPackageFragment(expr, fragment)) {
                return true;
              }
            }
            else if(object instanceof ClassExpression) {
              expr = ((ClassExpression)object).getStringValue();
              
              if(m_patternHelper.matchesPackageFragment(expr, fragment)) {
                return false;
              }
            }
          }
        }
      }
    }
    
    return false;
  }
  
  public boolean isAdaptable(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      if(fragments.length > 0) {
        for(int i = 0; i < fragments.length; i++) {
          if(!isAdaptable(fragments[i])) {
            return false;
          }
        }
        
        return true;
      }
    }
    
    return false;
  }

  public boolean isAdaptable(String classExpr) {
    TcConfig config = getConfig();
    
    if(config != null) {
      InstrumentedClasses classes = getInstrumentedClasses();
      
      if(classes != null) {
        XmlObject[] objects = classes.selectPath("*");
        
        if(objects != null && objects.length > 0) {
          XmlObject object;
          String    expr;
          
          for(int i = objects.length-1; i >= 0; i--) {
            object = objects[i];
            
            if(object instanceof Include) {
              expr = ((Include)object).getClassExpression();
              
              if(m_patternHelper.matchesClass(expr, classExpr)) {
                return true;
              }
            }
            else if(object instanceof ClassExpression) {
              expr = ((ClassExpression)object).getStringValue();
              
              if(m_patternHelper.matchesClass(expr, classExpr)) {
                return false;
              }
            }
          }
        }
      }
    }
    
    return m_plugin.isBootClass(classExpr);
  }

  public void ensureAdaptable(IJavaElement element) {
    if(element instanceof ICompilationUnit) {
      ensureAdaptable((ICompilationUnit)element);
    }
    else if(element instanceof IClassFile) {
      ensureAdaptable((IClassFile)element);
    }
    else if(element instanceof IType) {
      ensureAdaptable((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      ensureAdaptable((IPackageDeclaration)element);
    }
    else if(element instanceof IPackageFragment) {
      ensureAdaptable((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      ensureAdaptable((IJavaProject)element); 
    }
  }
  
  public void ensureAdaptable(final ICompilationUnit module) {
    ensureAdaptable(module.findPrimaryType());
  }
  
  public void ensureAdaptable(final IClassFile classFile) {
    try {
      ensureAdaptable(classFile.getType());
    } catch(JavaModelException jme) {
      openError("Error ensuring '"+
                classFile.getElementName()+"' instrumented", jme);
    }
  }

  public void ensureAdaptable(IType type) {
    while(type != null) {
      if(!isInterface(type)) {
        if(!isAdaptable(type)) {
          internalEnsureAdaptable(type);
        }
      }
      else {
        break;
      }
      
      IType parentType = type;
      while(parentType != null) {
        try {
          String superTypeSig  = parentType.getSuperclassTypeSignature();
          
          if(superTypeSig == null) {
            break;
          }
          
          String superTypeName = JdtUtils.getResolvedTypeName(superTypeSig, type);
          if(superTypeName == null || superTypeName.equals("java.lang.Object")) {
            break;
          }
          else {
            IType superType = JdtUtils.findType(m_javaProject, superTypeName);

            if(superType == null) {
              break;
            }
            else if(!isInterface(superType)) {
              if(!isAdaptable(superType)) {
                internalEnsureAdaptable(superType);
              }
              else {
                break;
              }
            }
            parentType = superType;
          }
        } catch(JavaModelException jme) {
          break;
        }
      }

      type = type.getDeclaringType();
    }
    
    ConfigurationEditor editor = getConfigurationEditor();
    if(false && editor != null) {
      editor.updateInstrumentedClassesPanel();
    }
    else {
      persistConfiguration();
    }
  }
  
  private void internalEnsureAdaptable(IType type) {
    internalEnsureAdaptable(PatternHelper.getFullyQualifiedName(type));
    
    int              filter   = IJavaSearchScope.SYSTEM_LIBRARIES;
    IJavaElement[]   elements = new IJavaElement[]{m_javaProject};
    IJavaSearchScope scope    = SearchEngine.createJavaSearchScope(elements, filter);
    
    if(scope.encloses(type)) {
      internalEnsureBootJarClass(type);
    }
  }
  
  public void ensureAdaptable(IPackageDeclaration packageDecl) {
    if(packageDecl != null && !isAdaptable(packageDecl)) {
      internalEnsureAdaptable(packageDecl);
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureAdaptable(IPackageDeclaration packageDecl) {
    internalEnsureAdaptable(PatternHelper.getWithinPattern(packageDecl));
  }

  public void ensureAdaptable(IPackageFragment fragment) {
    if(fragment != null && !isAdaptable(fragment)) {
      internalEnsureAdaptable(fragment);
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureAdaptable(IPackageFragment fragment) {
    internalEnsureAdaptable(PatternHelper.getWithinPattern(fragment));
  }
  
  public void ensureAdaptable(final IJavaProject javaProject) {
    if(javaProject != null && !isAdaptable(javaProject)) {
      internalEnsureAdaptable(javaProject);
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureAdaptable(final IJavaProject javaProject) {
    IPackageFragment[] fragments = getSourceFragments(javaProject);
    
    for(int i = 0; i < fragments.length; i++) {
      if(!isAdaptable(fragments[i])) {
        internalEnsureAdaptable(fragments[i]);
      }
    }
  }

  public void ensureAdaptable(final String classExpr) {
    if(!isAdaptable(classExpr)) {
      internalEnsureAdaptable(classExpr);
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureAdaptable(final String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    Include             include = classes.addNewInclude();
          
    include.setClassExpression(classExpr);
  }

  public void ensureNotAdaptable(IJavaElement element) {
    if(element instanceof ICompilationUnit) {
      ensureNotAdaptable((ICompilationUnit)element);
    }
    else if(element instanceof IType) {
      ensureNotAdaptable((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      ensureNotAdaptable((IPackageDeclaration)element);
    }
    else if(element instanceof IPackageFragment) {
      ensureNotAdaptable((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      ensureNotAdaptable((IJavaProject)element);
    }
  }
  
  public void ensureNotAdaptable(final ICompilationUnit module) {
    if(module != null) {
      internalEnsureNotAdaptable(module);
    }
  }
  
  private void internalEnsureNotAdaptable(final ICompilationUnit module) {
    IType primaryType = module.findPrimaryType();
    
    if(primaryType != null) {
      internalEnsureNotAdaptable(primaryType);
    }
  }

  public void ensureNotAdaptable(final IType type) {
    if(isAdaptable(type)) {
      baseEnsureNotAdaptable(type);
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  public void baseEnsureNotAdaptable(final IType type) {
    internalEnsureNotAdaptable(type);
  }
  
  private void internalEnsureNotAdaptable(final IType type) {
    internalEnsureNotLocked(type);
    internalEnsureNotBootJarClass(type);
    internalEnsureNotAdaptable(PatternHelper.getFullyQualifiedName(type));
    
    try {
      IField[] fields = type.getFields();
      
      if(fields != null) {
        for(int i = 0; i < fields.length; i++) {
          internalEnsureNotRoot(fields[i]);
        }
      }
      
      IType[] childTypes = type.getTypes();
      
      if(childTypes != null) {
        for(int i = 0; i < childTypes.length; i++) {
          internalEnsureNotAdaptable(childTypes[i]);
          internalEnsureNotBootJarClass(childTypes[i]);
        }
      }
      
      IMethod[] methods = type.getMethods();
      
      if(methods != null) {
        for(int i = 0; i < methods.length; i++) {
          internalEnsureNotLocked(methods[i]);
          internalEnsureLocalMethod(methods[i]);
        }
      }
    } catch(JavaModelException jme) {/**/}
  }

  public void ensureNotAdaptable(final IPackageDeclaration packageDecl) {
    if(isAdaptable(packageDecl)) {
      internalEnsureNotAdaptable(packageDecl);
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotAdaptable(final IPackageDeclaration packageDecl) {
    internalEnsureNotLocked(packageDecl);
    internalEnsureNotAdaptable(PatternHelper.getWithinPattern(packageDecl));
  }
  
  public void ensureNotAdaptable(final IPackageFragment fragment) {
    if(isAdaptable(fragment)) {
      internalEnsureNotAdaptable(fragment);
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotAdaptable(final IPackageFragment fragment) {
    internalEnsureNotLocked(fragment);
    
    try {
      ICompilationUnit[] cus = fragment.getCompilationUnits();
  
      if(cus != null) {
        for(int i = 0; i < cus.length; i++) {
          internalEnsureNotAdaptable(cus[i]);
        }
      }
    } catch(JavaModelException jme) {
      internalEnsureNotAdaptable(PatternHelper.getWithinPattern(fragment));
    }
  }

  public void ensureNotAdaptable(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      for(int i = 0; i < fragments.length; i++) {
        internalEnsureNotAdaptable(fragments[i]);
      }
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  public void ensureNotAdaptable(final String classExpr) {
    if(isAdaptable(classExpr)) {
      internalEnsureNotAdaptable(classExpr);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotAdaptable(final String classExpr) {
    InstrumentedClasses classes = getInstrumentedClasses();
    
    if(classes != null) {
      int    size = classes.sizeOfIncludeArray();
      String expr;
    
      for(int i = size-1; i >= 0; i--) {
        expr = classes.getIncludeArray(i).getClassExpression();

        if(m_patternHelper.matchesClass(expr, classExpr)) {
          classes.removeInclude(i);
        }
      }
    }
  }

  public boolean isExcluded(IJavaElement element) {
    if(element instanceof ICompilationUnit) {
      return isExcluded((ICompilationUnit)element);
    }
    else if(element instanceof IType) {
      return isExcluded((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      return isExcluded(element.getElementName());
    }
    else if(element instanceof IPackageFragment) {
      return isExcluded((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      return isExcluded((IJavaProject)element);
    }
    
    return false;
  }
  
  public boolean isExcluded(ICompilationUnit module) {
    return isExcluded(module.findPrimaryType());
  }

  public boolean isExcluded(IType type) {
    return type != null && isExcluded(PatternHelper.getFullyQualifiedName(type));
  }

  public boolean isExcluded(IPackageFragment fragment) {
    TcConfig config = getConfig();
    
    if(config != null) {
      InstrumentedClasses classes = getInstrumentedClasses();
      
      if(classes != null) {
        XmlObject[] objects = classes.selectPath("*");
        
        if(objects != null && objects.length > 0) {
          XmlObject object;
          String    expr;
          
          for(int i = objects.length-1; i >= 0; i--) {
            object = objects[i];
            
            if(object instanceof Include) {
              expr = ((Include)object).getClassExpression();
              
              if(m_patternHelper.matchesPackageFragment(expr, fragment)) {
                return false;
              }
            }
            else if(object instanceof ClassExpression) {
              expr = ((ClassExpression)object).getStringValue();
              
              if(m_patternHelper.matchesPackageFragment(expr, fragment)) {
                return true;
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  public boolean isExcluded(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      if(fragments.length > 0) {
        for(int i = 0; i < fragments.length; i++) {
          if(!isExcluded(fragments[i])) {
            return false;
          }
        }
        
        return true;
      }
    }
    
    return false;
  }

  public boolean isExcluded(String classExpr) {
    TcConfig config = getConfig();
    
    if(config != null) {
      InstrumentedClasses classes = getInstrumentedClasses();
      
      if(classes != null) {
        XmlObject[] objects = classes.selectPath("*");
        
        if(objects != null && objects.length > 0) {
          XmlObject object;
          String    expr;
          
          for(int i = objects.length-1; i >= 0; i--) {
            object = objects[i];
            
            if(object instanceof Include) {
              expr = ((Include)object).getClassExpression();
              
              if(m_patternHelper.matchesClass(expr, classExpr)) {
                return false;
              }
            }
            else if(object instanceof ClassExpression) {
              expr = ((ClassExpression)object).getStringValue();
              
              if(m_patternHelper.matchesClass(expr, classExpr)) {
                return true;
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  public void ensureExcluded(final IJavaElement element) {
    if(element instanceof ICompilationUnit) {
      ensureExcluded((ICompilationUnit)element);
    }
    else if(element instanceof IType) {
      ensureExcluded((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      ensureExcluded(element.getElementName());
    }
    else if(element instanceof IPackageFragment) {
      ensureExcluded((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      ensureExcluded((IJavaProject)element);
    }
  }
  
  public void ensureExcluded(final ICompilationUnit module) {
    if(module != null) {
      internalEnsureExcluded(module);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureExcluded(final ICompilationUnit module) {
    internalEnsureExcluded(module.findPrimaryType());
  }

  public void ensureExcluded(final IType type) {
    if(type != null && !isExcluded(type)) {
      internalEnsureExcluded(type);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureExcluded(final IType type) {
    internalEnsureExcluded(PatternHelper.getFullyQualifiedName(type));
  }

  public void ensureExcluded(final IPackageFragment fragment) {
    if(fragment != null && !isExcluded(fragment)) {
      internalEnsureExcluded(fragment);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureExcluded(final IPackageFragment fragment) {
    internalEnsureExcluded(PatternHelper.getWithinPattern(fragment));
  }

  public void ensureExcluded(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      for(int i = 0; i < fragments.length; i++) {
        internalEnsureExcluded(fragments[i]);
      }
      
      persistConfiguration();
    }
  }

  public void ensureExcluded(final String className) {
    if(className != null && !isExcluded(className)) {
      internalEnsureExcluded(className);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureExcluded(final String className) {
    ensureInstrumentedClasses().addExclude(className);
  }

  public void ensureNotExcluded(IJavaElement element) {
    if(element instanceof ICompilationUnit) {
      ensureNotExcluded((ICompilationUnit)element);
    }
    else if(element instanceof IType) {
      ensureNotExcluded((IType)element);
    }
    else if(element instanceof IPackageFragment) {
      ensureNotExcluded((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      ensureNotExcluded((IJavaProject)element);
    }
  }
  
  public void ensureNotExcluded(final ICompilationUnit module) {
    if(module != null) {
      internalEnsureNotExcluded(module);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotExcluded(final ICompilationUnit module) {
    internalEnsureNotExcluded(module.findPrimaryType());
  }

  public void ensureNotExcluded(final IType type) {
    if(type != null && isExcluded(type)) {
      baseEnsureNotExcluded(PatternHelper.getFullyQualifiedName(type));

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  public void baseEnsureNotExcluded(final IType type) {
    internalEnsureNotExcluded(type);
  }
  
  private void internalEnsureNotExcluded(final IType type) {
    internalEnsureNotExcluded(PatternHelper.getFullyQualifiedName(type));
  }

  public void ensureNotExcluded(final IPackageFragment fragment) {
    if(fragment != null) {
      ensureNotExcluded(PatternHelper.getWithinPattern(fragment));
    }
  }
  
  private void internalEnsureNotExcluded(final IPackageFragment fragment) {
    internalEnsureNotExcluded(PatternHelper.getWithinPattern(fragment));
  }

  public void ensureNotExcluded(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      for(int i = 0; i < fragments.length; i++) {
        internalEnsureNotExcluded(fragments[i]);
      }
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  public void ensureNotExcluded(final String classExpr) {
    if(isExcluded(classExpr)) {
      baseEnsureNotExcluded(classExpr);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateInstrumentedClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }  

  public void baseEnsureNotExcluded(final String classExpr) {
    internalEnsureNotExcluded(classExpr);
  }
  
  private void internalEnsureNotExcluded(final String classExpr) {
    InstrumentedClasses classes = getInstrumentedClasses();
    
    if(classes != null) {
      int    size = classes.sizeOfExcludeArray();
      String expr;
    
      for(int i = size-1; i >= 0; i--) {
        expr = classes.getExcludeArray(i);

        if(m_patternHelper.matchesClass(expr, classExpr)) {
          classes.removeExclude(i);
        }
      }
    }
  }  

  public static String getFullName(IField field) {
    IType  type       = field.getDeclaringType();
    String parentType = PatternHelper.getFullyQualifiedName(type);
    String fieldName  = field.getElementName();
    
    return parentType+"."+fieldName;
  }
  
  public boolean isRoot(IField field) {
    return field != null && isRoot(getFullName(field));
  }
  
  public boolean isRoot(String className, String fieldName) {
    return isRoot(className+"."+fieldName);
  }
  
  public boolean isRoot(String fieldName) {
    TcConfig config = getConfig();
    
    if(config != null) {
      Roots roots = getRoots();
      
      if(roots != null) {
        int size = roots.sizeOfRootArray();
        
        for(int i = 0; i < size; i++) {
          if(fieldName.equals(roots.getRootArray(i).getFieldName())) {
            return true;
          }
        }
      }
    }
    
    return false;
  }
  
  public void ensureRoot(IField field) {
    if(!isRoot(field)) {
      IType   declaringType      = field.getDeclaringType();
      boolean updateInstrumented = false;
      boolean updateTransients   = false;
      
      if(declaringType != null       &&
         !isInterface(declaringType) &&
         !isAdaptable(declaringType))
      {
        internalEnsureAdaptable(declaringType);
        updateInstrumented = true;
      }
      
      IType fieldType = getFieldType(field);
      if(fieldType != null       &&
         !isInterface(fieldType) &&
         !isAdaptable(fieldType))
      {
        internalEnsureAdaptable(fieldType);
        updateInstrumented = true;        
      }

      if(isTransient(field)) {
        ensureNotTransient(field);
        updateTransients = true;
      }

      internalEnsureRoot(getFullName(field));

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        if(updateInstrumented) {
          editor.updateInstrumentedClassesPanel();
        }
        if(updateTransients) {
          editor.updateTransientsPanel();
        }
        editor.updateRootsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  public IType getFieldType(IField field) {
    try {
      String sig           = field.getTypeSignature();
      IType  declaringType = field.getDeclaringType();
      String typeName      = JdtUtils.getResolvedTypeName(sig, declaringType);
      
      if(typeName != null) {
        return JdtUtils.findType(m_javaProject, typeName);
      }
    } catch(JavaModelException jme) {/**/}

    return null;
  }

  public IField getField(String fieldName) {
    int    lastDot           = fieldName.lastIndexOf('.');
    String declaringTypeName = fieldName.substring(0, lastDot);
    
    try {
      IType declaringType = JdtUtils.findType(m_javaProject, declaringTypeName);
      
      if(declaringType != null) {
        return declaringType.getField(fieldName.substring(lastDot+1));
      }
    } catch(JavaModelException jme) {/**/}
    
    return null;
  }
  
  public IType getFieldType(String fieldName) {
    IField field = getField(fieldName);
        
    if(field != null) {
      try {
        String sig           = field.getTypeSignature();
        IType  declaringType = field.getDeclaringType();
        String typeName      = JdtUtils.getResolvedTypeName(sig, declaringType);

        return JdtUtils.findType(m_javaProject, typeName);
      } catch(JavaModelException jme) {/**/}

    }
    
    return null;
  }
  
  public void ensureRoot(final String fieldName) {
    if(!isRoot(fieldName)) {
      IType   fieldType          = getFieldType(fieldName);
      boolean updateInstrumented = false;
      boolean updateTransients   = false;
      
      if(fieldType != null && !isAdaptable(fieldType)) {
        ensureAdaptable(fieldType);
        updateInstrumented = true;
      }
      
      if(isTransient(fieldName)) {
        ensureNotTransient(fieldName);
        updateTransients = true;
      }
      
      internalEnsureRoot(fieldName);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        if(updateInstrumented) {
          editor.updateInstrumentedClassesPanel();
        }
        if(updateTransients) {
          editor.updateTransientsPanel();
        }
        editor.updateRootsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureRoot(final String fieldName) {
    ensureRoots().addNewRoot().setFieldName(fieldName);
  }

  public void ensureNotRoot(final IField field) {
    if(field != null && isRoot(field)) {
      baseEnsureNotRoot(field);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateRootsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  public void baseEnsureNotRoot(final IField field) {
    internalEnsureNotRoot(field);
  }
  
  private void internalEnsureNotRoot(final IField field) {
    internalEnsureNotRoot(getFullName(field));
  }

  public void ensureNotRoot(final String fieldName) {
    if(isRoot(fieldName)) {
      baseEnsureNotRoot(fieldName);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateRootsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  public void baseEnsureNotRoot(final String fieldName) {
    internalEnsureNotRoot(fieldName);
  }
  
  private void internalEnsureNotRoot(final String fieldName) {
    Roots roots = getRoots();

    if(roots != null) {
      int size = roots.sizeOfRootArray();
      
      for(int i = size-1; i >= 0; i--) {
        if(fieldName.equals(roots.getRootArray(i).getFieldName())) {
          roots.removeRoot(i);
        }
      }
    
      testRemoveRoots();
    }
  }

  public void renameRoot(final String fieldName, final String newFieldName) {
    if(isRoot(fieldName)) {
      internalRenameRoot(fieldName, newFieldName);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateRootsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalRenameRoot(final String fieldName,
                                  final String newFieldName)
  {
    Roots roots = getRoots();

    if(roots != null) {
      Root root;
      int  size = roots.sizeOfRootArray();
      
      for(int i = 0; i < size; i++) {
        root = roots.getRootArray(i);
        
        if(fieldName.equals(root.getFieldName())) {
          root.setFieldName(newFieldName);
          return;
        }
      }
    }
  }

  public boolean isTransient(IField field) {
    return field != null && isTransient(getFullName(field));
  }
  
    public boolean isTransient(String className, String fieldName) {
    return isTransient(className+"."+fieldName);
  }
  
  public boolean isTransient(String fieldName) {
    TcConfig config = getConfig();
    
    if(config != null) {
      TransientFields transients = getTransientFields();
      
      if(transients != null) {
        int size = transients.sizeOfFieldNameArray();
        
        for(int i = 0; i < size; i++) {
          if(fieldName.equals(transients.getFieldNameArray(i))) {
            return true;
          }
        }
      }
    }
    
    return false;
  }
  
  public void ensureTransient(final IField field) {
    if(field != null && !isTransient(field)) {
      if(isRoot(field)) {
        internalEnsureNotRoot(field);

        ConfigurationEditor editor = getConfigurationEditor();
        if(false && editor != null) {
          editor.updateRootsPanel();
        }
      }

      IType declaringType = field.getDeclaringType();
      if(!isAdaptable(declaringType)) {
        internalEnsureAdaptable(declaringType);
        ConfigurationEditor editor = getConfigurationEditor();
        if(false && editor != null) {
          editor.updateInstrumentedClassesPanel();
        }
      }
      
      ensureTransient(getFullName(field));
    }
  }
  
  public void ensureTransient(final String fieldName) {
    if(!isTransient(fieldName)) {
      boolean updateInstrumented = false;
      boolean updateRoots        = false;
      
      IField field = getField(fieldName);
      if(field != null) {
        IType fieldType = field.getDeclaringType();
        if(!isAdaptable(fieldType)) {
          ensureAdaptable(fieldType);
          updateInstrumented = true;        
        }
      }
      
      if(isRoot(fieldName)) {
        ensureNotRoot(fieldName);
        updateRoots = true;
      }
      
      internalEnsureTransient(fieldName);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        if(updateInstrumented) {
          editor.updateInstrumentedClassesPanel();
        }
        if(updateRoots) {
          editor.updateRootsPanel();
        }
        editor.updateTransientsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureTransient(final String fieldName) {
    ensureTransientFields().addFieldName(fieldName);
  }

  public void ensureNotTransient(final IField field) {
    if(field != null) {
      ensureNotTransient(getFullName(field));
    }
  }
  
  public void ensureNotTransient(final String fieldName) {
    if(isTransient(fieldName)) {
      internalEnsureNotTransient(fieldName);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateTransientsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotTransient(final String fieldName) {
    TransientFields transients = getTransientFields();

    if(transients != null) {
      int size = transients.sizeOfFieldNameArray();
      
      for(int i = size-1; i >= 0; i--) {
        if(fieldName.equals(transients.getFieldNameArray(i))) {
          transients.removeFieldName(i);
        }
      }
    
      testRemoveTransientFields();
    }
  }

  private TransientFields getTransientFields() {
    DsoApplication dsoApp = getDsoApplication();
    return dsoApp != null ? dsoApp.getTransientFields() : null;
  }
  
  private TransientFields ensureTransientFields() {
    DsoApplication dsoApp      = ensureDsoApplication();
    TransientFields transients = dsoApp.getTransientFields();
    
    if(transients == null) {
      transients = dsoApp.addNewTransientFields();
    }
   
    return transients;
  }

  private void testRemoveTransientFields() {
    DsoApplication dsoApp = getDsoApplication();
    
    if(dsoApp != null) {
      TransientFields transients = dsoApp.getTransientFields();
      
      if(transients != null) {
        if(transients.sizeOfFieldNameArray() == 0) {
          dsoApp.unsetTransientFields();
          testRemoveDsoApplication();
        }
      }
    }
  }
  
  public boolean matches(final String expression, final MemberInfo methodInfo) {
    return m_patternHelper.matchesMember(expression, methodInfo);
  }

  public boolean matches(final String expression, final IMethod method) {
    return m_patternHelper.matchesMethod(expression, method);
  }

  public boolean isDistributedMethod(final MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();
    
    if(binding != null && !binding.getDeclaringClass().isInterface()) {
      return isDistributedMethod(PatternHelper.methodDecl2IMethod(methodDecl));
    }
    
    return false;
  }

  public boolean isDistributedMethod(final IMethod method) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
    return methodInfo != null && isDistributedMethod(methodInfo);
  }

  public boolean isDistributedMethod(final MethodInfo methodInfo) {
    TcConfig config = getConfig();
    
    if(config != null) {
      DistributedMethods methods = getDistributedMethods();
      
      if(methods != null) {
        int    size = methods.sizeOfMethodExpressionArray();
        String expr;
        
        for(int i = 0; i < size; i++) {
          expr = methods.getMethodExpressionArray(i).getStringValue();
          
          if(m_patternHelper.matchesMember(expr, methodInfo)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }
  
  public void ensureDistributedMethod(final IMethod method) {
    if(!isDistributedMethod(method)) {
      internalEnsureDistributedMethod(method);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateDistributedMethodsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureDistributedMethod(final IMethod method) {
    IType declaringType = method.getDeclaringType();
    
    if(!isAdaptable(declaringType)) {
      internalEnsureAdaptable(declaringType);
    }
    
    DistributedMethods   methods = ensureDistributedMethods();
    MethodNameExpression expr    = methods.addNewMethodExpression();
      
    try {
      expr.setStringValue(PatternHelper.getJavadocSignature(method));
    } catch(JavaModelException jme) {
      openError("Error ensuring method '"+
                method.getElementName()+"' distributed", jme);
      return;
    }
  }

  public void ensureLocalMethod(final IMethod method) {
    if(isDistributedMethod(method)) {
      internalEnsureLocalMethod(method);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateDistributedMethodsPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureLocalMethod(final IMethod method) {
    DistributedMethods methods = getDistributedMethods();
    
    if(methods != null) {
      int    size = methods.sizeOfMethodExpressionArray();
      String expr;
      
      for(int i = size-1; i >= 0; i--) {
        expr = methods.getMethodExpressionArray(i).getStringValue();
        
        if(m_patternHelper.matchesMethod(expr, method)) {
          methods.removeMethodExpression(i);
        }
      }
    
      testRemoveDistributedMethods();
    }
  }

  public boolean isLocked(final IMethod method) {
    try {
      if(!method.getDeclaringType().isInterface()) {
        return isAutolocked(method) || isNameLocked(method);
      }
    } catch(JavaModelException jme) {/**/}
    
    return false;
  }

  public boolean isLocked(final MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();
    
    if(binding != null && !binding.getDeclaringClass().isInterface()) {
      return isLocked(PatternHelper.methodDecl2IMethod(methodDecl));
    }
    
    return false;
  }

  public boolean isAutolocked(final IJavaElement element) {
    if(element instanceof IMethod) {
      return isAutolocked((IMethod)element);
    }
    else if(element instanceof IType) {
      return isAutolocked((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      return isAutolocked((IPackageDeclaration)element);
    }
    else if(element instanceof IPackageFragment) {
      return isAutolocked((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      return isAutolocked((IJavaProject)element);
    }
    
    return false;
  }
  
  public boolean isAutolocked(final IMethod method) {
    try {
      if(!method.getDeclaringType().isInterface()) {
        MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
        return methodInfo != null && isAutolocked(methodInfo);
      }
    } catch(JavaModelException jme) {/**/}
    
    return false;
  }

  public boolean isAutolocked(final MethodInfo methodInfo) {
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int      size = locks.sizeOfAutolockArray();
        Autolock autolock;
        String   expr;
        
        for(int i = 0; i < size; i++) {
          autolock = locks.getAutolockArray(i);
          expr     = autolock.getMethodExpression();
          
          if(m_patternHelper.matchesMember(expr, methodInfo)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }

  public boolean isAutolocked(final MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();
    
    if(binding != null && !binding.getDeclaringClass().isInterface()) {
      return isAutolocked(PatternHelper.methodDecl2IMethod(methodDecl));
    }
    
    return false;
  }
  
  public boolean isAutolocked(final IType type) {
    try {
      if(type.isInterface()) {
        return false;
      }
    } catch(JavaModelException jme) {/**/}
    
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int      size = locks.sizeOfAutolockArray();
        Autolock autolock;
        String   expr;
        String   typeExpr;
        
        typeExpr = PatternHelper.getExecutionPattern(type);
        
        for(int i = 0; i < size; i++) {
          autolock = locks.getAutolockArray(i);
          expr     = autolock.getMethodExpression();
          
          if(typeExpr.equals(expr)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }

  public boolean isAutolocked(final IPackageDeclaration packageDecl) {
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int      size = locks.sizeOfAutolockArray();
        Autolock autolock;
        String   expr;
        String   fragExpr;
        
        fragExpr = PatternHelper.getExecutionPattern(packageDecl);
        
        for(int i = 0; i < size; i++) {
          autolock = locks.getAutolockArray(i);
          expr     = autolock.getMethodExpression();
          
          if(fragExpr.equals(expr)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }
  
  public boolean isAutolocked(final IPackageFragment fragment) {
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int      size = locks.sizeOfAutolockArray();
        Autolock autolock;
        String   expr;
        String   fragExpr;
        
        fragExpr = PatternHelper.getExecutionPattern(fragment);
        
        for(int i = 0; i < size; i++) {
          autolock = locks.getAutolockArray(i);
          expr     = autolock.getMethodExpression();
          
          if(fragExpr.equals(expr)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }

  public boolean isAutolocked(final IJavaProject javaProject) {
    if(javaProject != null) {
        IPackageFragment[] fragments = getSourceFragments(javaProject);
        
        if(fragments.length > 0) {
          for(int i = 0; i < fragments.length; i++) {
            if(!isAutolocked(fragments[i])) {
              return false;
            }
          }
          
          return true;
        }



    }
    
    return false;
  }
  
  public boolean isNameLocked(final IJavaElement element) {
    if(element instanceof IMethod) {
      return isNameLocked((IMethod)element);
    }
    else if(element instanceof IType) {
      return isNameLocked((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      return isNameLocked((IPackageDeclaration)element);
    }
    else if(element instanceof IPackageFragment) {
      return isNameLocked((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      return isNameLocked((IJavaProject)element);
    }
    
    return false;
  }
  
  public boolean isNameLocked(final IMethod method) {
    try {
      if(!method.getDeclaringType().isInterface()) {
        MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
        return methodInfo != null && isNameLocked(methodInfo);
      }
    } catch(JavaModelException jme) {/**/}
    
    return false;
  }

  public boolean isNameLocked(final MethodInfo methodInfo) {
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int       size = locks.sizeOfNamedLockArray();
        NamedLock namedLock;
        String    expr;

        for(int i = 0; i < size; i++) {
          namedLock = locks.getNamedLockArray(i);
          expr      = namedLock.getMethodExpression();
          
          if(m_patternHelper.matchesMember(expr, methodInfo)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }

  public boolean isNameLocked(final MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();
    
    if(binding != null && !binding.getDeclaringClass().isInterface()) {
      return isNameLocked(PatternHelper.methodDecl2IMethod(methodDecl));
    }
    
    return false;
  }
  
  public boolean isNameLocked(final IType type) {
    try {
      if(type.isInterface()) {
        return false;
      }
    } catch(JavaModelException jme) {/**/}
    
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int       size = locks.sizeOfNamedLockArray();
        NamedLock namedLock;
        String    expr;
        String    typeExpr;
        
        typeExpr = PatternHelper.getExecutionPattern(type);

        for(int i = 0; i < size; i++) {
          namedLock = locks.getNamedLockArray(i);
          expr      = namedLock.getMethodExpression();
          
          if(typeExpr.equals(expr)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }

  public boolean isNameLocked(final IPackageDeclaration packageDecl) {
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int       size = locks.sizeOfNamedLockArray();
        NamedLock namedLock;
        String    expr;
        String    fragExpr;
        
        fragExpr = PatternHelper.getExecutionPattern(packageDecl);
        
        for(int i = 0; i < size; i++) {
          namedLock = locks.getNamedLockArray(i);
          expr      = namedLock.getMethodExpression();
          
          if(fragExpr.equals(expr)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }
  
  public boolean isNameLocked(final IPackageFragment fragment) {
    TcConfig config = getConfig();
    
    if(config != null) {
      Locks locks = getLocks();
      
      if(locks != null) {
        int       size = locks.sizeOfNamedLockArray();
        NamedLock namedLock;
        String    expr;
        String    fragExpr;
        
        fragExpr = PatternHelper.getExecutionPattern(fragment);
        
        for(int i = 0; i < size; i++) {
          namedLock = locks.getNamedLockArray(i);
          expr      = namedLock.getMethodExpression();
          
          if(fragExpr.equals(expr)) {
            return true;
          }
        }
      }
    }
    
    return false;
  }

  public boolean isNameLocked(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      if(fragments.length > 0) {
        for(int i = 0; i < fragments.length; i++) {
          if(!isNameLocked(fragments[i])) {
            return false;
          }
        }
        
        return true;
      }
    }
    
    return false;
  }

  public void ensureNameLocked(final IJavaElement element) {
    ensureNameLocked(element, "LockName", LockLevel.WRITE);
  }

  public void ensureNameLocked(
    final IJavaElement   element,
    final String         name,
    final LockLevel.Enum level)
  {
    if(element instanceof IMethod) {
      ensureNameLocked((IMethod)element, name, level);
    }
    else if(element instanceof IType) {
      ensureNameLocked((IType)element, name, level);
    }
    else if(element instanceof IPackageDeclaration) {
      ensureNameLocked((IPackageDeclaration)element, name, level);
    }
    else if(element instanceof IPackageFragment) {
      ensureNameLocked((IPackageFragment)element, name, level);
    }
    else if(element instanceof IJavaProject) {
      ensureNameLocked((IJavaProject)element, name, level);
    }
  }
  
  public void ensureNameLocked(
    final IMethod        method,
    final String         name,
    final LockLevel.Enum level)
  {
    if(!isNameLocked(method)) {
      internalEnsureNameLocked(method, name, level);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNameLocked(
    final IMethod        method,
    final String         name,
    final LockLevel.Enum level)
  {
    IType declaringType = method.getDeclaringType();
    
    if(!isAdaptable(declaringType)) {
      internalEnsureAdaptable(declaringType);
    }

    Locks     locks = ensureLocks();
    NamedLock lock  = locks.addNewNamedLock();
      
    try {
      lock.setMethodExpression(PatternHelper.getJavadocSignature(method));
      lock.setLockLevel(level);
      lock.setLockName(name);
    } catch(JavaModelException jme) {
      openError("Error ensuring method '"+
                method.getElementName()+"' name-locked", jme);
      return;
    }
  }

  public void ensureNameLocked(
    final IType          type,
    final String         name,
    final LockLevel.Enum level)
  {
    if(!isNameLocked(type)) {
      internalEnsureNameLocked(type, name, level);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNameLocked(
    final IType          type,
    final String         name,
    final LockLevel.Enum level)
  {
    if(!isAdaptable(type)) {
      internalEnsureAdaptable(type);
    }

    Locks     locks  = ensureLocks();
    NamedLock lock   = locks.addNewNamedLock();
    String    expr   = PatternHelper.getExecutionPattern(type);
      
    lock.setMethodExpression(expr);
    lock.setLockLevel(level);
    lock.setLockName(name);
  }

  public void ensureNameLocked(
     final IPackageDeclaration packageDecl,
     final String              name,
     final LockLevel.Enum      level)
  {
    if(!isNameLocked(packageDecl)) {
      internalEnsureNameLocked(packageDecl, name, level);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

   private void internalEnsureNameLocked(
     final IPackageDeclaration packageDecl,
     final String              name,
     final LockLevel.Enum      level)
   {
     if(!isAdaptable(packageDecl)) {
       internalEnsureAdaptable(packageDecl);
     }

     Locks     locks = ensureLocks();
     NamedLock lock  = locks.addNewNamedLock();
     String    expr  = PatternHelper.getExecutionPattern(packageDecl);
       
     lock.setMethodExpression(expr);
     lock.setLockName(name);
     lock.setLockLevel(level);
   }

  public void ensureNameLocked(
    final IPackageFragment fragment,
    final String           name,
    final LockLevel.Enum   level)
  {
    if(!isNameLocked(fragment)) {
      internalEnsureNameLocked(fragment, name, level);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNameLocked(
    final IPackageFragment fragment,
    final String           name,
    final LockLevel.Enum   level)
  {
    if(!isAdaptable(fragment)) {
      internalEnsureAdaptable(fragment);
    }

    Locks     locks = ensureLocks();
    NamedLock lock  = locks.addNewNamedLock();
    String    expr  = PatternHelper.getExecutionPattern(fragment);
      
    lock.setMethodExpression(expr);
    lock.setLockName(name);
    lock.setLockLevel(level);
  }

  public void ensureNameLocked(
    final IJavaProject   javaProject,
    final String         name,
    final LockLevel.Enum level)
  {
    if(javaProject != null && !isNameLocked(javaProject)) {
      internalEnsureNameLocked(javaProject, name, level);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNameLocked(
    final IJavaProject   javaProject,
    final String         name,
    final LockLevel.Enum level)
  {
    IPackageFragment[] fragments = getSourceFragments(javaProject);
    
    for(int i = 0; i < fragments.length; i++) {
      if(!isNameLocked(fragments[i])) {
        internalEnsureNameLocked(fragments[i], name, level);
      }
    }
  }

  public void ensureAutolocked(final IJavaElement element) {
    if(element instanceof IMethod) {
      ensureAutolocked((IMethod)element);
    }
    else if(element instanceof IType) {
      ensureAutolocked((IType)element);
    }
    else if(element instanceof IPackageDeclaration) {
      ensureAutolocked((IPackageDeclaration)element);
    }
    else if(element instanceof IPackageFragment) {
      ensureAutolocked((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      ensureAutolocked((IJavaProject)element);
    }
  }
  
  public void ensureAutolocked(final IMethod method) {
    if(!isAutolocked(method)) {
      internalEnsureAutolocked(method);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureAutolocked(final IMethod method) {
    IType declaringType = method.getDeclaringType();
    
    if(!isAdaptable(declaringType)) {
      internalEnsureAdaptable(declaringType);
    }

    Locks    locks = ensureLocks();
    Autolock lock  = locks.addNewAutolock();
    
    try {
      lock.setMethodExpression(PatternHelper.getJavadocSignature(method));
      lock.setLockLevel(LockLevel.WRITE);
    } catch(JavaModelException jme) {
      openError("Error ensuring method '"+
                method.getElementName()+"' auto-locked", jme);
      return;
    }
  }

  public void ensureAutolocked(final IType type) {
    if(!isAutolocked(type)) {
      internalEnsureAutolocked(type);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureAutolocked(final IType type) {
    if(!isAdaptable(type)) {
      internalEnsureAdaptable(type);
    }

    Locks    locks = ensureLocks();
    Autolock lock  = locks.addNewAutolock();
    String   expr  = PatternHelper.getExecutionPattern(type);
    
    lock.setMethodExpression(expr);
    lock.setLockLevel(LockLevel.WRITE);
  }

  public void ensureAutolocked(final IPackageDeclaration packageDecl) {
    if(!isAutolocked(packageDecl)) {
      internalEnsureAutolocked(packageDecl);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureAutolocked(final IPackageDeclaration packageDecl) {
    if(!isAdaptable(packageDecl)) {
      internalEnsureAdaptable(packageDecl);
    }

    Locks    locks = ensureLocks();
    Autolock lock  = locks.addNewAutolock();
    String   expr  = PatternHelper.getExecutionPattern(packageDecl);
    
    lock.setMethodExpression(expr);
    lock.setLockLevel(LockLevel.WRITE);
  }
  
  public void ensureAutolocked(final IPackageFragment fragment) {
    if(!isAutolocked(fragment)) {
      internalEnsureAutolocked(fragment);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureAutolocked(final IPackageFragment fragment) {
    if(!isAdaptable(fragment)) {
      internalEnsureAdaptable(fragment);
    }

    Locks    locks = ensureLocks();
    Autolock lock  = locks.addNewAutolock();
    String   expr  = PatternHelper.getExecutionPattern(fragment);
    
    lock.setMethodExpression(expr);
    lock.setLockLevel(LockLevel.WRITE);
  }

  public void ensureAutolocked(final IJavaProject javaProject) {
    if(javaProject != null && !isAutolocked(javaProject)) {
      internalEnsureAutolocked(javaProject);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureAutolocked(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      for(int i = 0; i < fragments.length; i++) {
        if(!isAutolocked(fragments[i])) {
          internalEnsureAutolocked(fragments[i]);
        }
      }
    }
  }

  public void ensureNotNameLocked(final IJavaElement element) {
    if(element instanceof IMethod) {
      ensureNotNameLocked((IMethod)element);
    }
    else if(element instanceof IType) {
      ensureNotNameLocked((IType)element);
    }
    else if(element instanceof IPackageFragment) {
      ensureNotNameLocked((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      ensureNotNameLocked((IJavaProject)element);
    }
  }

  public void ensureNotNameLocked(final IMethod method) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
    
    if(methodInfo != null) {
      ensureNotNameLocked(methodInfo);
    }
  }
  
  public void ensureNotNameLocked(final MethodInfo methodInfo) {
    if(isNameLocked(methodInfo)) {
      internalEnsureNotNameLocked(methodInfo);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotNameLocked(final MethodInfo methodInfo) {
    Locks locks = getLocks();
    
    if(locks != null) {
      int    size = locks.sizeOfNamedLockArray();
      String expr;
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getNamedLockArray(i).getMethodExpression();
        
        if(m_patternHelper.matchesMember(expr, methodInfo)) {
          locks.removeNamedLock(i);
        }
      }
      
      testRemoveLocks();
    }
  }

  public void ensureNotNameLocked(final IType type) {
    if(isNameLocked(type)) {
      internalEnsureNotNameLocked(type);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotNameLocked(final IType type) {
    Locks locks = getLocks();
    
    if(locks != null) {
      int    size = locks.sizeOfNamedLockArray();
      String expr;
      String typeExpr;
      
      typeExpr = PatternHelper.getExecutionPattern(type);
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getNamedLockArray(i).getMethodExpression();
        
        if(typeExpr.equals(expr)) {
          locks.removeNamedLock(i);
        }
      }
      
      testRemoveLocks();
    }
  }

  public void ensureNotNameLocked(final IPackageDeclaration packageDecl) {
    if(isNameLocked(packageDecl)) {
      internalEnsureNotNameLocked(packageDecl);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotNameLocked(final IPackageDeclaration packageDecl) {
    Locks locks = getLocks();
    
    if(locks != null) {
      int    size = locks.sizeOfNamedLockArray();
      String expr;
      String fragExpr;
      
      fragExpr = PatternHelper.getExecutionPattern(packageDecl);
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getNamedLockArray(i).getMethodExpression();
        
        if(fragExpr.equals(expr)) {
          locks.removeNamedLock(i);
        }
      }
      
      testRemoveLocks();
    }
  }
  
  public void ensureNotNameLocked(final IPackageFragment fragment) {
    if(isNameLocked(fragment)) {
      internalEnsureNotNameLocked(fragment);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotNameLocked(final IPackageFragment fragment) {
    Locks locks = getLocks();
    
    if(locks != null) {
      int    size = locks.sizeOfNamedLockArray();
      String expr;
      String fragExpr;
      
      fragExpr = PatternHelper.getExecutionPattern(fragment);
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getNamedLockArray(i).getMethodExpression();
        
        if(fragExpr.equals(expr)) {
          locks.removeNamedLock(i);
        }
      }
      
      testRemoveLocks();
    }
  }

  public void ensureNotNameLocked(final IJavaProject javaProject) {
    if(javaProject != null && isNameLocked(javaProject)) {
      internalEnsureNotNameLocked(javaProject);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotNameLocked(final IJavaProject javaProject) {
    IPackageFragment[] fragments = getSourceFragments(javaProject);
    
    for(int i = 0; i < fragments.length; i++) {
      if(isNameLocked(fragments[i])) {
        internalEnsureNotNameLocked(fragments[i]);
      }
    }
  }

  public void ensureNotAutolocked(final IJavaElement element) {
    if(element instanceof IMethod) {
      ensureNotAutolocked((IMethod)element);
    }
    else if(element instanceof IType) {
      ensureNotAutolocked((IType)element);
    }
    else if(element instanceof IPackageFragment) {
      ensureNotAutolocked((IPackageFragment)element);
    }
    else if(element instanceof IJavaProject) {
      ensureNotAutolocked((IJavaProject)element);
    }
  }
  
  public void ensureNotAutolocked(final IMethod method) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
    
    if(methodInfo != null) {
      ensureNotAutolocked(methodInfo);
    }
  }
  
  public void ensureNotAutolocked(final MethodInfo methodInfo) {
    if(isAutolocked(methodInfo)) {
      internalEnsureNotAutolocked(methodInfo);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotAutolocked(final MethodInfo methodInfo) {
    Locks locks = getLocks();
    
    if(locks != null) {
      int    size = locks.sizeOfAutolockArray();
      String expr;
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getAutolockArray(i).getMethodExpression();
        
        if(m_patternHelper.matchesMember(expr, methodInfo)) {
          locks.removeAutolock(i);
        }
      }
      
      testRemoveLocks();
    }
  }

  public void ensureNotAutolocked(final IType type) {
    if(isAutolocked(type)) {
      internalEnsureNotAutolocked(type);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotAutolocked(final IType type) {
    Locks locks = getLocks();
    
    if(locks != null) {
      int    size = locks.sizeOfAutolockArray();
      String expr;
      String typeExpr;
      
      typeExpr = PatternHelper.getExecutionPattern(type);
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getAutolockArray(i).getMethodExpression();
        
        if(typeExpr.equals(expr)) {
          locks.removeAutolock(i);
        }
      }
      
      testRemoveLocks();
    }
  }

  public void ensureNotAutolocked(final IPackageDeclaration packageDecl) {
    if(isAutolocked(packageDecl)) {
      internalEnsureNotAutolocked(packageDecl);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotAutolocked(final IPackageDeclaration packageDecl) {
    Locks locks = getLocks();
      
    if(locks != null) {
      int    size = locks.sizeOfAutolockArray();
      String expr;
      String fragExpr;
      
      fragExpr = PatternHelper.getExecutionPattern(packageDecl);
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getAutolockArray(i).getMethodExpression();
        
        if(fragExpr.equals(expr)) {
          locks.removeAutolock(i);
        }
      }
      
      testRemoveLocks();
    }
  }
  
  public void ensureNotAutolocked(final IPackageFragment fragment) {
    if(isAutolocked(fragment)) {
      internalEnsureNotAutolocked(fragment);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotAutolocked(final IPackageFragment fragment) {
    Locks locks = getLocks();
      
    if(locks != null) {
      int    size = locks.sizeOfAutolockArray();
      String expr;
      String fragExpr;
      
      fragExpr = PatternHelper.getExecutionPattern(fragment);
      
      for(int i = size-1; i >= 0; i--) {
        expr = locks.getAutolockArray(i).getMethodExpression();
        
        if(fragExpr.equals(expr)) {
          locks.removeAutolock(i);
        }
      }
      
      testRemoveLocks();
    }
  }

  public void ensureNotAutolocked(final IJavaProject javaProject) {
    if(javaProject != null && isAutolocked(javaProject)) {
      internalEnsureNotAutolocked(javaProject);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotAutolocked(final IJavaProject javaProject) {
    IPackageFragment[] fragments = getSourceFragments(javaProject);
    
    for(int i = 0; i < fragments.length; i++) {
      if(isAutolocked(fragments[i])) {
        internalEnsureNotAutolocked(fragments[i]);
      }
    }
  }

  public void ensureNotLocked(final IMethod method) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
    
    if(methodInfo != null) {
      ensureNotLocked(methodInfo);
    }
  }

  public void ensureNotLocked(final MethodInfo methodInfo) {
    if(methodInfo != null) {
      boolean persist = false;
      
      if(isAutolocked(methodInfo)) {
        internalEnsureNotAutolocked(methodInfo);
        persist = true;
      }
      
      if(isNameLocked(methodInfo)) {
        internalEnsureNotNameLocked(methodInfo);
        persist = true;
      }
      
      if(persist) {
        ConfigurationEditor editor = getConfigurationEditor();
        if(false && editor != null) {
          editor.updateLocksPanel();
        }
        else {
          persistConfiguration();
        }
      }
    }
  }

  private void internalEnsureNotLocked(final IMethod method) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
    
    if(method != null) {
      internalEnsureNotLocked(methodInfo);
    }
  }

  private void internalEnsureNotLocked(final MethodInfo methodInfo) {
    if(isAutolocked(methodInfo)) {
      internalEnsureNotAutolocked(methodInfo);
    }
    if(isNameLocked(methodInfo)) {
      internalEnsureNotNameLocked(methodInfo);
    }
  }

  public void ensureNotLocked(final IType type) {
    boolean persist = false;
    
    if(isAutolocked(type)) {
      internalEnsureNotAutolocked(type);
      persist = true;
    }
    
    if(isNameLocked(type)) {
      internalEnsureNotNameLocked(type);
      persist = true;
    }
    
    if(persist) {
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotLocked(final IType type) {
    if(isAutolocked(type)) {
      internalEnsureNotAutolocked(type);
    }
    
    if(isNameLocked(type)) {
      internalEnsureNotNameLocked(type);
    }
  }

  public void ensureNotLocked(final IPackageFragment fragment) {
    boolean persist = false;
    
    if(isAutolocked(fragment)) {
      internalEnsureNotAutolocked(fragment);
      persist = true;
    }
    
    if(isNameLocked(fragment)) {
      internalEnsureNotNameLocked(fragment);
      persist = true;
    }
    
    if(persist) {
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotLocked(final IPackageDeclaration packageDecl) {
    if(isAutolocked(packageDecl)) {
      internalEnsureNotAutolocked(packageDecl);
    }
    
    if(isNameLocked(packageDecl)) {
      internalEnsureNotNameLocked(packageDecl);
    }
  }

  private void internalEnsureNotLocked(final IPackageFragment fragment) {
    if(isAutolocked(fragment)) {
      internalEnsureNotAutolocked(fragment);
    }
    
    if(isNameLocked(fragment)) {
      internalEnsureNotNameLocked(fragment);
    }
  }

  public void ensureNotLocked(final IJavaProject javaProject) {
    if(javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      
      for(int i = 0; i < fragments.length; i++) {
        internalEnsureNotLocked(fragments[i]);
      }
      
      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateLocksPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  public boolean isBootJarClass(ICompilationUnit module) {
    return isBootJarClass(module.findPrimaryType());
  }

  public boolean isBootJarClass(IType type) {
    return isBootJarClass(PatternHelper.getFullyQualifiedName(type));
  }
  
  public boolean isBootJarClass(String className) {
    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();
    
    if(classes != null) {
      String[] includes = classes.getIncludeArray();

      for(int i = 0; i < includes.length; i++) {
        if(m_patternHelper.matchesClass(includes[i], className)) {
          return true;
        }
      }
    }

    return false;
  }

  public void ensureBootJarClass(final ICompilationUnit module) {
    ensureBootJarClass(module.findPrimaryType());
  }
  
  public void ensureBootJarClass(final IType type) {
    ensureBootJarClass(PatternHelper.getFullyQualifiedName(type));
  }
  
  private void internalEnsureBootJarClass(final IType type) {
    internalEnsureBootJarClass(PatternHelper.getFullyQualifiedName(type));
  }
  
  public void ensureBootJarClass(final String className) {
    if(!isBootJarClass(className)) {
      internalEnsureBootJarClass(className);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateBootClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureBootJarClass(final String className) {
    if(!isAdaptable(className)) {
      internalEnsureAdaptable(className);
    }
    ensureAdditionalBootJarClasses().addInclude(className);
  }

  public void ensureNotBootJarClass(final ICompilationUnit module) {
    if(module != null) {
      internalEnsureNotBootJarClass(module);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateBootClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotBootJarClass(final ICompilationUnit module) {
    internalEnsureNotBootJarClass(module.findPrimaryType());
  }

  public void ensureNotBootJarClass(final IType type) {
    if(type != null && isBootJarClass(type)) {
      internalEnsureNotBootJarClass(type);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateBootClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }
  
  private void internalEnsureNotBootJarClass(final IType type) {
    internalEnsureNotBootJarClass(PatternHelper.getFullyQualifiedName(type));
  }

  public void ensureNotBootJarClass(final String className) {
    if(isBootJarClass(className)) {
      internalEnsureNotBootJarClass(className);

      ConfigurationEditor editor = getConfigurationEditor();
      if(false && editor != null) {
        editor.updateBootClassesPanel();
      }
      else {
        persistConfiguration();
      }
    }
  }

  private void internalEnsureNotBootJarClass(final String className) {
    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();
    
    if(classes != null) {
      String[] includes = classes.getIncludeArray();

      for(int i = includes.length-1; i >= 0 ; i--) {
        if(m_patternHelper.matchesClass(includes[i], className)) {
          classes.removeInclude(i);
        }
      }
    
      testRemoveAdditionalBootJarClasses();
    }
  }

  /**
   * Are there any bootjar classes specified in the configuration?
   */
  public boolean hasBootJarClasses() {
    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();
    return classes != null && classes.sizeOfIncludeArray() > 0;
  }
  
  // Validation support
  
  public void validateAll() {
    if(getConfig() != null) {
      validateLocks();
      validateRoots();
      validateTransientFields();
      validateInstrumentedClasses();
      validateBootJarClasses();
    }
  }
  
  private static String LOCK_PROBLEM_MARKER = "org.terracotta.dso.LockMethodProblemMarker";
  
  private static String getRawString(XmlString xmlString) {
    String s = xmlString.toString();
    s = s.substring(s.indexOf('>')+1);
    s = s.substring(0, s.indexOf('<'));
    return s;
  }
  
  public void validateLocks() {
    clearConfigProblemMarkersOfType(LOCK_PROBLEM_MARKER);
    
    Locks locks = getLocks();
    
    if(locks != null) {
      Autolock[]  autoLocks  = locks.getAutolockArray();
      NamedLock[] namedLocks = locks.getNamedLockArray();
      
      if(autoLocks != null) {
        for(int i = 0; i < autoLocks.length; i++) {
          validateLockMethodExpression(getRawString(autoLocks[i].xgetMethodExpression()));
        }
      }
      
      if(namedLocks != null) {
        for(int i = 0; i < namedLocks.length; i++) {
          validateLockMethodExpression(getRawString(namedLocks[i].xgetMethodExpression()));
        }
      }
    }
  }
  
  private static String ROOT_PROBLEM_MARKER = "org.terracotta.dso.RootProblemMarker";

  public void validateRoots() {
    clearConfigProblemMarkersOfType(ROOT_PROBLEM_MARKER);
    
    Roots roots = getRoots();
    
    if(roots != null) {
      Root[] rootArray = roots.getRootArray();
    
      for(int i = 0; i < rootArray.length; i++) {
        validateRoot(rootArray[i]);
      }
    }
  }
  
  private static boolean isInterface(IType type) {
    try {
      return type.isInterface();
    } catch(JavaModelException jme) {
      return false;
    }
  }
  
  private static final String[]  PRIMITIVE_NAMES = new String[] {
    "java.lang.String",
    "java.lang.Integer",
    "java.lang.Boolean",
    "java.lang.Double",
    "java.lang.Character",
    "java.lang.Byte"
  };
  
  private static final ArrayList PRIMITIVES = new ArrayList(Arrays.asList(PRIMITIVE_NAMES));
  
  private static boolean isPrimitive(IType type) {
    try {
      String name = type.getFullyQualifiedName();
      return PRIMITIVES.contains(name);
    } catch(Exception e) {
      return false;
    }
  }
  
  private void validateRoot(Root root) {
    String rootName = getRawString(root.xgetFieldName());
    String msg      = validateField(rootName);
    
    if(msg == null) {
      IField field     = getField(root.getFieldName());
      IType  fieldType = getFieldType(field);
      
      if(fieldType != null       &&
         !isInterface(fieldType) &&
         !isPrimitive(fieldType) &&
         !isAdaptable(fieldType))
      {
        String fullName = PatternHelper.getFullyQualifiedName(fieldType);
        msg = "Root type '" + fullName + "' not instrumented";
      }
      else {
        IType declaringType = field.getDeclaringType();
        
        if(declaringType != null       &&
           !isInterface(declaringType) &&
           !isPrimitive(declaringType) &&
           !isAdaptable(declaringType))
        {
          String fullName = PatternHelper.getFullyQualifiedName(declaringType);
          msg = "Declaring type '" + fullName + "' not instrumented";
        }
      }
    }
    
    if(msg != null) {
      reportConfigProblem(rootName, msg, ROOT_PROBLEM_MARKER);
    }
  }

  private static String TRANSIENT_PROBLEM_MARKER = "org.terracotta.dso.TransientProblemMarker";

  public void validateTransientFields() {
    clearConfigProblemMarkersOfType(TRANSIENT_PROBLEM_MARKER);
    
    TransientFields transientFields = getTransientFields();
    
    if(transientFields != null) {
      int    size = transientFields.sizeOfFieldNameArray();
      String field;
    
      for(int i = 0; i < size; i++) {
        field = getRawString(transientFields.xgetFieldNameArray(i));
        validateTransientField(field);
      }
    }
  }
  
  private void validateTransientField(String fieldName) {
    String msg = validateField(fieldName);

    if(msg == null) {
      IField field         = getField(fieldName);
      IType  declaringType = field.getDeclaringType();
      
      if(declaringType != null && !isAdaptable(declaringType)) {
        String fullName = PatternHelper.getFullyQualifiedName(declaringType);
        msg = "Declaring type '" + fullName + "' not instrumented";
      }
    }

    if(msg != null) {
      reportConfigProblem(fieldName, msg, TRANSIENT_PROBLEM_MARKER);
    }
  }

  private static String
    INSTRUMENTED_PROBLEM_MARKER = "org.terracotta.dso.InstrumentedProblemMarker";
  
  public void validateInstrumentedClasses() {
    clearConfigProblemMarkersOfType(INSTRUMENTED_PROBLEM_MARKER);

    InstrumentedClasses instrumentedClasses = getInstrumentedClasses();
    
    if(instrumentedClasses != null) {
      validateIncludes(instrumentedClasses);
      validateExcludes(instrumentedClasses);
    }
  }

  private void validateIncludes(InstrumentedClasses instrumentedClasses) {
    int     size = instrumentedClasses.sizeOfIncludeArray();
    Include include;
    String  expr;
    
    for(int i = 0; i < size; i++) {
      include = instrumentedClasses.getIncludeArray(i);
      expr    = getRawString(include.xgetClassExpression());

      validateInstrumentedTypeExpression(expr);
    }
  }
  
  private void validateExcludes(InstrumentedClasses instrumentedClasses) {
    int    size = instrumentedClasses.sizeOfExcludeArray();
    String exclude;

    for(int i = 0; i < size; i++) {
      exclude = getRawString(instrumentedClasses.xgetExcludeArray(i));
      validateInstrumentedTypeExpression(exclude);
    }
  }
  
  private void validateInstrumentedTypeExpression(String typeExpr) {
    String msg  = null;
    String expr = typeExpr != null ? typeExpr.trim() : null;
    
    if(expr != null && (expr.indexOf('*') != -1 || expr.indexOf('+') != -1)) {
      return;
    }
    
    try {
      if(JdtUtils.findType(m_javaProject, expr) != null) {
        return;
      }
    } catch(JavaModelException jme) {/**/}
    
    if(expr != null && expr.length() > 0) {
      String prefix = findTypeExpressionPrefix(expr);
      
      try {
        IPackageFragment[] fragments = m_javaProject.getPackageFragments();
        IPackageFragment   fragment;
        
        for(int i = 0; i < fragments.length; i++) {
          fragment = fragments[i];
          
          if(fragment.getKind() == IPackageFragmentRoot.K_SOURCE &&
             fragment.getElementName().startsWith(prefix))
          {
            ICompilationUnit[] cus = fragment.getCompilationUnits();
            ICompilationUnit   cu;
            String             cuType;
            
            for(int j = 0; j < cus.length; j++) {
              cu     = cus[j];
              cuType = PatternHelper.getFullyQualifiedName(cu.findPrimaryType());
              
              if(cuType.startsWith(prefix)) {
                IType[] types = cus[j].getAllTypes();
                IType   type;
                
                for(int k = 0; k < types.length; k++) {
                  type = types[k];
                  
                  if(!type.isInterface()) {
                    if(matchesType(expr, type)) {
                      return;
                    }
                  }
                }
              }
            }
          }
        }
        
        for(int i = 0; i < fragments.length; i++) {
          fragment = fragments[i];
          
          if(fragment.getKind() == IPackageFragmentRoot.K_BINARY &&
             fragment.getElementName().startsWith(prefix))
          {
            IClassFile[] classFiles = fragment.getClassFiles();
            IType        type;
            String       typeName;
            int          flags;
            
            for(int j = 0; j < classFiles.length; j++) {
              type     = classFiles[j].getType();
              typeName = PatternHelper.getFullyQualifiedName(type);
              flags    = type.getFlags();
              
              if(typeName.startsWith(prefix)) {
                if(!type.isInterface()       &&
                   !type.isAnonymous()       &&
                   !type.isMember()          &&
                   !type.isEnum()            &&
                   !type.isAnnotation()      &&
                   !Flags.isProtected(flags) &&
                   !Flags.isPrivate(flags)   &&
                   !Flags.isStatic(flags))
                {
                  if(matchesType(expr, type)) {
                    return;
                  }
                }
              }
            }
          }
        }
        
        msg = "No type matches expression '"+expr+"'";
      }
      catch(JavaModelException jme) {
        msg = jme.getMessage();
      }
    }
    else {
      msg = "Empty AspectWerks class expression";
    }
    
    if(msg != null) {
      reportConfigProblem(typeExpr, msg, INSTRUMENTED_PROBLEM_MARKER);
    }
  }

  private String findTypeExpressionPrefix(String typeExpr) {
    String[]  elems    = StringUtils.split(typeExpr, '.');
    char[]    codes    = new char[] {'*', '?', '.', '$'};
    ArrayList list     = new ArrayList();
    String    elem;
    
    for(int i = 0; i < elems.length; i++) {
      elem = elems[i];
      
      if(elem.length() > 0 && StringUtils.containsNone(elems[i], codes)) {
        list.add(elem);
      }
    }
    
    return StringUtils.join(list.toArray(), '.');
  }
  
  private boolean matchesType(String typeExpr, IType type) {
    String name    = PatternHelper.getFullyQualifiedName(type);
    int    nameLen = name.length();
    int    exprLen = typeExpr.length();
    char   ec;

    if(typeExpr.equals(name)) {
      return true;
    }
    
    for(int z = 0; z < exprLen; z++) {
      ec = typeExpr.charAt(z);
      
      if(z == nameLen || name.charAt(z) != ec) {
        if(ec == '.' || ec == '*' || ec == '?') {
          return m_patternHelper.matchesType(typeExpr, type);
        }
        break;
      }
    }

    return false;
  }
  
  private void validateLockMethodExpression(String methodExpr) {
    String msg = validateMethodExpression(methodExpr);
    
    if(msg != null) {
      reportConfigProblem(methodExpr, msg, LOCK_PROBLEM_MARKER);
    }
  }

  private static String
    DISTRIBUTED_METHOD_PROBLEM_MARKER = "org.terracotta.dso.DistributedMethodProblemMarker";

  public void validateDistributedMethods() {
    clearConfigProblemMarkersOfType(DISTRIBUTED_METHOD_PROBLEM_MARKER);
    
    DistributedMethods distributedMethods = getDistributedMethods();

    if(distributedMethods != null) {
      int    size = distributedMethods.sizeOfMethodExpressionArray();
      String methodExpr;
    
      for(int i = 0; i < size; i++) {
        methodExpr = distributedMethods.getMethodExpressionArray(i).getStringValue();
        validateDistributedMethodExpression(methodExpr);
      }
    }
  }
  
  private void validateDistributedMethodExpression(String methodExpr) {
    String msg = validateMethodExpression(methodExpr);
    
    if(msg != null) {
      reportConfigProblem(methodExpr, msg, DISTRIBUTED_METHOD_PROBLEM_MARKER);
    }
  }

  private String validateField(String fullName) {
    String msg = null;
    int    lastDotIndex;

    if(fullName != null) {
      fullName = fullName.trim();
    }
    
    if(fullName != null && 
       (fullName.length() > 0) &&
       (lastDotIndex = fullName.lastIndexOf('.')) != -1)
    {
      String className = fullName.substring(0, lastDotIndex).replace('$', '.');
      String fieldName = fullName.substring(lastDotIndex+1);
          
      try {
        IType  type;
        IField field;
        
        if((type = JdtUtils.findType(m_javaProject, className)) == null) {
          msg = "Class not found: " + className;
        }
        else if((field = type.getField(fieldName)) == null || !field.exists()) {
          msg = "No such field: " + fieldName;
        }
      } catch(JavaModelException jme) {
        msg = jme.getMessage();
      }
    }
    else {
      msg = "Must be a fully-qualified field name";
    }

    return msg;
  }

  private String validateMethodExpression(String methodExpr) {
    String msg = null;

    if(methodExpr != null && methodExpr.length() > 0) {
      try {
        m_patternHelper.testValidateMethodExpression(methodExpr);
      } catch(Exception e) {
        return e.getMessage();
      }
      
      try {
        String prefix = findMethodExpressionPrefix(methodExpr);
        
        if(prefix != null && prefix.length() > 0) {
          try {
            IType type = JdtUtils.findType(m_javaProject, prefix);
            
            if(type != null) {
              IMethod[] methods = type.getMethods();
              
              for(int m = 0; m < methods.length; m++) {
                if(m_patternHelper.matchesMethod(methodExpr, methods[m])) {
                  return null;
                }
              }
            }
          } catch(JavaModelException jme) {/**/}
        }

        IPackageFragment[] fragments = m_javaProject.getPackageFragments();
        IPackageFragment   fragment;

        for(int i = 0; i < fragments.length; i++) {
          fragment = fragments[i];
          
          if(fragment.getKind() == IPackageFragmentRoot.K_SOURCE &&
             fragment.getElementName().startsWith(prefix))
          {
            ICompilationUnit[] cus = fragment.getCompilationUnits();
            ICompilationUnit   cu;
            String             cuType;
            
            for(int j = 0; j < cus.length; j++) {
              cu     = cus[j];
              cuType = PatternHelper.getFullyQualifiedName(cu.findPrimaryType());
              
              if(cuType.startsWith(prefix)) {
                IType[] types = cus[j].getAllTypes();
                IType   type;
                
                for(int k = 0; k < types.length; k++) {
                  type = types[k];
                  
                  if(!type.isInterface() && !type.isAnonymous()) {
                    if(matchesMethod(methodExpr, type)) {
                      return null;
                    }
                  }
                }
              }
            }
          }
        }
        
        for(int i = 0; i < fragments.length; i++) {
          fragment = fragments[i];
          
          if(fragment.getKind() == IPackageFragmentRoot.K_BINARY &&
             fragment.getElementName().startsWith(prefix))
          {
            IClassFile[] classFiles = fragment.getClassFiles();
            IType        type;
            String       typeName;
            int          flags;
            
            for(int j = 0; j < classFiles.length; j++) {
              type     = classFiles[j].getType();
              typeName = PatternHelper.getFullyQualifiedName(type);
              flags    = type.getFlags();
              
              if(typeName.startsWith(prefix)) {
                if(!type.isInterface()       &&
                   !type.isAnonymous()       &&
                   !type.isMember()          &&
                   !type.isEnum()            &&
                   !type.isAnnotation()      &&
                   !Flags.isPrivate(flags)   &&
                   !Flags.isProtected(flags) &&
                   !Flags.isStatic(flags))
                {
                  if(matchesMethod(methodExpr, type)) {
                    return null;
                  }
                }
              }
            }
          }
        }
          
        msg = "No method matching expression '" + methodExpr + "'";
      } catch(JavaModelException jme) {
        msg = jme.getMessage();
      }
    }
    else {
      msg = "Must be a fully-qualified method name";
    }
    
    return msg;
  }

  private String findMethodExpressionPrefix(String methodExpr) {
    String[]  comps    = StringUtils.split(methodExpr);
    String    exprBody = comps.length > 1 ? comps[1] : comps[0];
    String[]  elems    = StringUtils.split(exprBody, '.');
    char[]    codes    = new char[] {'*', '?', '(', ')', '$'};
    ArrayList list     = new ArrayList();
    String    elem;
    
    for(int i = 0; i < elems.length; i++) {
      elem = elems[i];
      
      if(elem.length() > 0 && StringUtils.containsNone(elems[i], codes)) {
        list.add(elem);
      }
      else {
        break;
      }
    }
    
    return StringUtils.join(list.toArray(), '.');
  }
  
  private boolean matchesMethod(String methodExpr, IType type) {
    String[] comps       = StringUtils.split(methodExpr);
    String   exprBody    = comps.length > 1 ? comps[1] : comps[0];
    int      exprBodyLen = exprBody.length();
    String   name        = PatternHelper.getFullyQualifiedName(type);
    int      nameLen     = name.length();
    char     ebc;
    
    for(int z = 0; z < exprBodyLen; z++) {
      ebc = exprBody.charAt(z);
      
      if(z == nameLen || ebc != name.charAt(z)) {
        if(ebc == '.' || ebc == '*' || ebc == '?' || ebc == '(') {
          try {
            IMethod[] methods = type.getMethods();
            IMethod   method;
            
            for(int m = 0; m < methods.length; m++) {
              method = methods[m];
              
              if(m_patternHelper.matchesMethod(methodExpr, method)) {
                return true;
              }
            }
          } catch(JavaModelException jme) {
            return false;
          }
        }

        return false;
      }
    }
    
    // exact match
    return true;
  }
  
  private static String
    BOOT_CLASS_PROBLEM_MARKER = "org.terracotta.dso.BootClassProblemMarker";

  public void validateBootJarClasses() {
    clearConfigProblemMarkersOfType(BOOT_CLASS_PROBLEM_MARKER);

    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();

    if(classes != null) {
      int    size = classes.sizeOfIncludeArray();
      String include;
      
      for(int i = 0; i < size; i++) {
        include = getRawString(classes.xgetIncludeArray(i));
        validateBootJarClass(include);
      }
    }
  }
  
  private void validateBootJarClass(String classExpr) {
    String msg  = null;
    String expr = classExpr;
    
    if(expr != null) {
      expr = expr.trim();
    }
    
    try {
      if(JdtUtils.findType(m_javaProject, expr) == null) {
        msg = "Cannot resolve type '" + expr + "'";
      }
    } catch(JavaModelException jme) {
      msg = "Cannot resolve type '" + expr + "'";
    }
    
    if(msg != null) {
      reportConfigProblem(classExpr, msg, BOOT_CLASS_PROBLEM_MARKER);
    }
  }
  
  private void reportConfigProblem(
    String configText,
    String msg,
    String markerType)
  {
    ConfigurationEditor editor = m_plugin.getConfigurationEditor(m_project);
    
    if(editor != null) {
      editor.applyProblemToText(configText, msg, markerType);
    }
    else {
      IFile       file = m_plugin.getConfigurationFile(m_project);
      InputStream in   = null; 

      try {
        String   text = IOUtils.toString(in = file.getContents());
        Document doc  = new Document(text);
      
        applyProblemToText(doc, configText, msg, markerType);
      } catch(Exception e) {
        m_plugin.openError("Problem reporting config problem", e);
      }
      finally {
        IOUtils.closeQuietly(in);
      }
    }
  }
  
  public void applyProblemToText(
    IDocument doc,
    String    text,
    String    msg,
    String    markerType)
  {
    TcPlugin                   plugin = TcPlugin.getDefault();
    FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(doc);
    IRegion                    region;
   
    try {
      text = "\\Q"+text+"\\E";
      if((region = finder.find(0, text, true, true, false, true)) != null) {
        HashMap map   = new HashMap();
        int     start = region.getOffset();
        int     end   = start + region.getLength();
        int     line  = doc.getLineOfOffset(start)-1;
        
        MarkerUtilities.setMessage(map, msg);
        MarkerUtilities.setLineNumber(map, line);
        MarkerUtilities.setCharStart(map, start);
        MarkerUtilities.setCharEnd(map, end);
          
        map.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));
        map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
        map.put(IMarker.LOCATION, "line " + line);
            
        IFile configFile = plugin.getConfigurationFile(m_project);
        MarkerUtilities.createMarker(configFile, map, markerType);
      }
    } catch(Exception e2) {
      plugin.openError("Problem creating marker '"+markerType+"'", e2);
    }
  }

  private TcConfig getConfig() {
    return m_plugin.getConfiguration(m_project);
  }
  
  private DsoApplication ensureDsoApplication() {
    DsoApplication dsoApp = null;
    TcConfig       config = getConfig();
    
    if(config != null) {
      Application app = config.getApplication();
      
      if(app == null) {
        app = config.addNewApplication();
      }
      
      if((dsoApp = app.getDso()) == null) {
        dsoApp = app.addNewDso();
        dsoApp.addNewInstrumentedClasses();
      }
    }
    
    return dsoApp;
  }

  private Application getApplication() {
    TcConfig config = getConfig();
    return config != null ? config.getApplication() : null;
  }

  private void testRemoveApplication() {
    Application app = getApplication();
    
    if(app != null) {
      if(!app.isSetDso()) {
        getConfig().unsetApplication();
      }
    }
  }
  
  private DsoApplication getDsoApplication() {
    Application app = getApplication();
    return app != null ? app.getDso() : null;
  }
  
  private void testRemoveDsoApplication() {
    DsoApplication dsoApp = getDsoApplication();
    
    if(dsoApp != null) {
      InstrumentedClasses classes    = dsoApp.getInstrumentedClasses();
      boolean             hasClasses = classes.sizeOfExcludeArray() > 0 ||
                                       classes.sizeOfIncludeArray() > 0;
      
      if(!dsoApp.isSetAdditionalBootJarClasses() &&
         !dsoApp.isSetDistributedMethods()       &&
         !hasClasses                             &&
         !dsoApp.isSetLocks()                    &&
         !dsoApp.isSetRoots()                    &&
         !dsoApp.isSetTransientFields())
      {
        getApplication().unsetDso();
        testRemoveApplication();
      }
    }
  }
  
  private Roots getRoots() {
    DsoApplication dsoApp = getDsoApplication();
    return dsoApp != null ? dsoApp.getRoots() : null;
  }
  
  private Roots ensureRoots() {
    DsoApplication dsoApp = ensureDsoApplication();
    Roots          roots  = dsoApp.getRoots();

    return roots != null ? roots : dsoApp.addNewRoots();
  }
  
  private void testRemoveRoots() {
    DsoApplication dsoApp = getDsoApplication();
    
    if(dsoApp != null) {
      Roots roots = dsoApp.getRoots();
    
      if(roots.sizeOfRootArray() == 0) {
        dsoApp.unsetRoots();
        testRemoveDsoApplication();
      }
    }
  }
  
  private DistributedMethods getDistributedMethods() {
    DsoApplication dsoApp = getDsoApplication();
    return dsoApp != null ? dsoApp.getDistributedMethods() : null;
  }
  
  private DistributedMethods ensureDistributedMethods() {
    DsoApplication     dsoApp  = ensureDsoApplication();
    DistributedMethods methods = dsoApp.getDistributedMethods();
    
    if(methods == null) {
      methods = dsoApp.addNewDistributedMethods();
    }
    
    return methods;
  }
  
  private void testRemoveDistributedMethods() {
    DistributedMethods methods = getDistributedMethods();
    
    if(methods != null) {
      if(methods.sizeOfMethodExpressionArray() == 0) {
        getDsoApplication().unsetDistributedMethods();
        testRemoveDsoApplication();
      }
    }
  }
  
  private Locks getLocks() {
    DsoApplication dsoApp = getDsoApplication();
    return dsoApp != null ? dsoApp.getLocks() : null;
  }
 
  private Locks ensureLocks() {
    DsoApplication dsoApp = ensureDsoApplication();
    Locks          locks  = dsoApp.getLocks();
    
    if(locks == null) {
      locks = dsoApp.addNewLocks();
    }
    
    return locks;
  }
  
  private void testRemoveLocks() {
    Locks locks = getLocks();
    
    if(locks != null) {
      if(locks.sizeOfAutolockArray() == 0 &&
         locks.sizeOfNamedLockArray() == 0)
      {
        getDsoApplication().unsetLocks();
        testRemoveDsoApplication();
      }
    }
  }
  
  private InstrumentedClasses getInstrumentedClasses() {
    DsoApplication dsoApp = getDsoApplication();
    return dsoApp != null ? dsoApp.getInstrumentedClasses() : null;
  }

  private InstrumentedClasses ensureInstrumentedClasses() {
    DsoApplication      dsoApp  = ensureDsoApplication();
    InstrumentedClasses classes = dsoApp.getInstrumentedClasses();
    
    if(classes == null) {
      classes = dsoApp.addNewInstrumentedClasses();
    }
    
    return classes;
  }
  
  private AdditionalBootJarClasses getAdditionalBootJarClasses() {
    return ensureDsoApplication().getAdditionalBootJarClasses();
  }
  
  private AdditionalBootJarClasses ensureAdditionalBootJarClasses() {
    DsoApplication           dsoApp = ensureDsoApplication();
    AdditionalBootJarClasses abjc   = dsoApp.getAdditionalBootJarClasses();

    return abjc != null ? abjc : dsoApp.addNewAdditionalBootJarClasses();
  }
  
  private void testRemoveAdditionalBootJarClasses() {
    DsoApplication dsoApp = getDsoApplication();
    
    if(dsoApp != null) {
      AdditionalBootJarClasses abjc = dsoApp.getAdditionalBootJarClasses();
    
      if(abjc.sizeOfIncludeArray() == 0) {
        dsoApp.unsetAdditionalBootJarClasses();
        testRemoveDsoApplication();
      }
    }
  }
  
  public Servers getServers() {
    TcConfig config = getConfig();
    return config != null ? config.getServers() : null;
  }
  
  private static IPackageFragment[] getSourceFragments(IJavaProject javaProject) {
    ArrayList list = new ArrayList();

    try {
      IPackageFragment[] fragments = javaProject.getPackageFragments();
      
      for(int i = 0; i < fragments.length; i++) {
        if(isSourceFragment(fragments[i])) {
          list.add(fragments[i]);
        }
      }
    } catch(JavaModelException jme) {/**/}
    
    return (IPackageFragment[])list.toArray(new IPackageFragment[0]);

  }
  
  private static boolean isSourceFragment(final IPackageFragment fragment) {
    try {
      return fragment.getKind() == IPackageFragmentRoot.K_SOURCE &&
             hasCompilationUnits(fragment);
    } catch(JavaModelException jme) {
      return false;
    }
  }
  
  private static boolean hasCompilationUnits(IPackageFragment fragment) {
    try {
      IResource   resource  = fragment.getResource();
      int         type      = resource.getType();
      IContainer  container = null;

      switch(type) {
        case IResource.PROJECT: {
          IProject project = (IProject)resource;
          String   name    = fragment.getElementName();
          
          if(name.equals(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH)) {
            container = project;
          }
          else {
            String path = fragment.getElementName().replace('.', '/');
            container = project.getFolder(project.getLocation().append(path));
          }
          break;
        }
        case IResource.FOLDER:
          container = (IFolder)resource;
          break;
      }

      if(container != null) {
        IResource[] members = container.members();
        IResource   member;
        
        for(int i = 0; i < members.length; i++) {
          member = members[i];
          
          if(member.getType() == IResource.FILE &&
             member.getFileExtension().equals("java"))
          {
            return true;
          }
        }
      }
    } catch(CoreException ce) {/**/}
    

    return false;
  }
  
  private void openError(String msg, Throwable t) {
    m_plugin.openError(msg, t);
  }
  
  public ConfigurationEditor getConfigurationEditor() {
    return m_plugin.getConfigurationEditor(m_project);
  }
  
  public void persistConfiguration() {
    ConfigurationEditor editor = getConfigurationEditor();
    
    if(false && editor != null) {
      editor._setDirty();
    }
    else {
      m_plugin.saveConfiguration(m_project);
    }
  }    
  
  private void clearConfigProblemMarkersOfType(String markerType) {
    m_plugin.clearConfigProblemMarkersOfType(m_project, markerType);
  }
}
