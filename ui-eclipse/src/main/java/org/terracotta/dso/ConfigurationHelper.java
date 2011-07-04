/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import org.osgi.framework.BundleException;
import org.terracotta.dso.editors.ConfigurationEditor;

import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.object.NonInstrumentedClasses;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Application;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.Client;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.DistributedMethods.MethodExpression;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Utility singleton for use by the various popup actions in org.terracotta.dso.popup.actions.
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
  private final TcPlugin               m_plugin;
  private final IProject               m_project;
  private final IJavaProject           m_javaProject;
  private final PatternHelper          m_patternHelper;
  private final NonInstrumentedClasses m_nonInstrumentedClasses;

  public ConfigurationHelper(IProject project) {
    m_plugin = TcPlugin.getDefault();
    m_project = project;
    m_javaProject = JavaCore.create(m_project);
    m_patternHelper = PatternHelper.getHelper();
    m_nonInstrumentedClasses = new NonInstrumentedClasses();
  }

  public boolean isInstrumentationNotNeeded(final ICompilationUnit cu) {
    return isInstrumentationNotNeeded(cu.findPrimaryType());
  }

  public boolean isInstrumentationNotNeeded(final IClassFile cf) {
    return isInstrumentationNotNeeded(cf.findPrimaryType());
  }

  public boolean isInstrumentationNotNeeded(final IType type) {
    return isInstrumentationNotNeeded(type.getFullyQualifiedName('$'));
  }

  public boolean isInstrumentationNotNeeded(final String classname) {
    return m_nonInstrumentedClasses.isInstrumentationNotNeeded(classname);
  }

  public boolean isAdaptable(IJavaElement element) {
    if (element instanceof ICompilationUnit) {
      return isAdaptable((ICompilationUnit) element);
    } else if (element instanceof IClassFile) {
      return isAdaptable((IClassFile) element);
    } else if (element instanceof IType) {
      return isAdaptable((IType) element);
    } else if (element instanceof IPackageDeclaration) {
      return isAdaptable((IPackageDeclaration) element);
    } else if (element instanceof IPackageFragment) {
      return isAdaptable((IPackageFragment) element);
    } else if (element instanceof IJavaProject) { return isAdaptable((IJavaProject) element); }

    return false;
  }

  public boolean isAdaptable(ICompilationUnit module) {
    return isAdaptable(module.findPrimaryType());
  }

  public boolean isAdaptable(IClassFile classFile) {
    try {
      return isAdaptable(classFile.getType());
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isAdaptable(IType type) {
    if (type != null) { return m_plugin.isBootClass(m_project, type) || isTypeAdaptable(type); }
    return false;
  }

  private Boolean isTypeAdaptable(IType type, InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      XmlObject[] objects = instrumentedClasses.selectPath("*");

      if (objects != null && objects.length > 0) {
        for (int i = objects.length - 1; i >= 0; i--) {
          XmlObject object = objects[i];

          if (object instanceof Include) {
            String expr = ((Include) object).getClassExpression();
            if (m_patternHelper.matchesType(expr, type)) { return Boolean.TRUE; }
          } else if (object instanceof ClassExpression) {
            String expr = ((ClassExpression) object).getStringValue();
            if (m_patternHelper.matchesType(expr, type)) { return Boolean.FALSE; }
          }
        }
      }
    }
    return null;
  }

  public boolean isTypeAdaptable(IType type) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses instrumentedClasses = getInstrumentedClasses();
      if (instrumentedClasses != null) {
        Boolean adaptable = isTypeAdaptable(type, instrumentedClasses);
        if (adaptable != null) { return adaptable; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        instrumentedClasses = modulesConfig.getApplication().getInstrumentedClasses();
        if (instrumentedClasses != null) {
          Boolean adaptable = isTypeAdaptable(type, instrumentedClasses);
          if (adaptable != null) { return adaptable; }
        }
      }
    }

    return false;
  }

  private Boolean isAdaptable(IPackageDeclaration packageDecl, InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      XmlObject[] objects = instrumentedClasses.selectPath("*");

      if (objects != null && objects.length > 0) {
        for (int i = objects.length - 1; i >= 0; i--) {
          XmlObject object = objects[i];

          if (object instanceof Include) {
            String expr = ((Include) object).getClassExpression();
            if (m_patternHelper.matchesPackageDeclaration(expr, packageDecl)) { return Boolean.TRUE; }
          } else if (object instanceof ClassExpression) {
            String expr = ((ClassExpression) object).getStringValue();
            if (m_patternHelper.matchesPackageDeclaration(expr, packageDecl)) { return Boolean.FALSE; }
          }
        }
      }
    }
    return null;
  }

  public boolean isAdaptable(IPackageDeclaration packageDecl) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses instrumentedClasses = getInstrumentedClasses();

      if (instrumentedClasses != null) {
        Boolean adaptable = isAdaptable(packageDecl, instrumentedClasses);
        if (adaptable != null) { return adaptable; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        instrumentedClasses = modulesConfig.getApplication().getInstrumentedClasses();
        if (instrumentedClasses != null) {
          Boolean adaptable = isAdaptable(packageDecl, instrumentedClasses);
          if (adaptable != null) { return adaptable; }
        }
      }
    }

    return false;
  }

  private Boolean isAdaptable(IPackageFragment fragment, InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      XmlObject[] objects = instrumentedClasses.selectPath("*");

      if (objects != null && objects.length > 0) {
        for (int i = objects.length - 1; i >= 0; i--) {
          XmlObject object = objects[i];

          if (object instanceof Include) {
            String expr = ((Include) object).getClassExpression();
            if (m_patternHelper.matchesPackageFragment(expr, fragment)) { return Boolean.TRUE; }
          } else if (object instanceof ClassExpression) {
            String expr = ((ClassExpression) object).getStringValue();
            if (m_patternHelper.matchesPackageFragment(expr, fragment)) { return Boolean.FALSE; }
          }
        }
      }
    }
    return null;
  }

  public boolean isAdaptable(IPackageFragment fragment) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses instrumentedClasses = getInstrumentedClasses();

      if (instrumentedClasses != null) {
        Boolean adaptable = isAdaptable(fragment, instrumentedClasses);
        if (adaptable != null) { return adaptable; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        instrumentedClasses = modulesConfig.getApplication().getInstrumentedClasses();
        if (instrumentedClasses != null) {
          Boolean adaptable = isAdaptable(fragment, instrumentedClasses);
          if (adaptable != null) { return adaptable; }
        }
      }
    }

    return false;
  }

  public boolean isAdaptable(final IJavaProject javaProject) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);

      if (fragments.length > 0) {
        for (int i = 0; i < fragments.length; i++) {
          if (!isAdaptable(fragments[i])) { return false; }
        }

        return true;
      }
    }

    return false;
  }

  private Boolean isAdaptable(String classExpr, InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      XmlObject[] objects = instrumentedClasses.selectPath("*");

      if (objects != null && objects.length > 0) {
        for (int i = objects.length - 1; i >= 0; i--) {
          XmlObject object = objects[i];

          if (object instanceof Include) {
            String expr = ((Include) object).getClassExpression();
            if (m_patternHelper.matchesClass(expr, classExpr)) { return Boolean.TRUE; }
          } else if (object instanceof ClassExpression) {
            String expr = ((ClassExpression) object).getStringValue();
            if (m_patternHelper.matchesClass(expr, classExpr)) { return Boolean.FALSE; }
          }
        }
      }
    }
    return null;
  }

  public boolean isAdaptable(String classExpr) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses instrumentedClasses = getInstrumentedClasses();

      if (instrumentedClasses != null) {
        Boolean adaptable = isAdaptable(classExpr, instrumentedClasses);
        if (adaptable != null) { return adaptable; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        instrumentedClasses = modulesConfig.getApplication().getInstrumentedClasses();
        if (instrumentedClasses != null) {
          Boolean adaptable = isAdaptable(classExpr, instrumentedClasses);
          if (adaptable != null) { return adaptable; }
        }
      }
    }

    return m_plugin.isBootClass(m_project, classExpr);
  }

  public Include includeRuleFor(String classExpr) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses classes = getInstrumentedClasses();

      if (classes != null) {
        XmlObject[] objects = classes.selectPath("*");

        if (objects != null && objects.length > 0) {
          for (int i = objects.length - 1; i >= 0; i--) {
            XmlObject object = objects[i];

            if (object instanceof Include) {
              String expr = ((Include) object).getClassExpression();
              if (expr != null && expr.equals(classExpr)) { return (Include) object; }
            }
          }
        }
      }
    }

    return null;
  }

  public Include ensureIncludeRuleFor(String classExpr) {
    Include include = includeRuleFor(classExpr);

    if (include == null) {
      include = addIncludeRule(classExpr);
      m_plugin.fireExcludeRulesChanged(m_project);
    }

    return include;
  }

  public boolean declaresRoot(IType type) {
    return declaresRoot(type.getFullyQualifiedName());
  }

  public boolean declaresRoot(String typeName) {
    if (typeName == null) return false;

    Roots roots = getRoots();
    if (roots != null) {
      for (int i = 0; i < roots.sizeOfRootArray(); i++) {
        String rootFieldName = roots.getRootArray(i).getFieldName();

        if (rootFieldName != null && rootFieldName.length() > 0) {
          int dotIndex = rootFieldName.lastIndexOf('.');

          if (dotIndex != -1) {
            String rootTypeName = rootFieldName.substring(0, dotIndex);
            if (typeName.equals(rootTypeName)) { return true; }
          }
        }
      }
    }

    return false;
  }

  public void ensureAdaptable(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(element, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(IJavaElement element, MultiChangeSignaller signaller) {
    if (element instanceof ICompilationUnit) {
      ensureAdaptable((ICompilationUnit) element, signaller);
    } else if (element instanceof IClassFile) {
      ensureAdaptable((IClassFile) element, signaller);
    } else if (element instanceof IType) {
      ensureAdaptable((IType) element, signaller);
    } else if (element instanceof IPackageDeclaration) {
      ensureAdaptable((IPackageDeclaration) element, signaller);
    } else if (element instanceof IPackageFragment) {
      ensureAdaptable((IPackageFragment) element, signaller);
    } else if (element instanceof IJavaProject) {
      ensureAdaptable((IJavaProject) element, signaller);
    }
  }

  public void ensureAdaptable(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(module, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(ICompilationUnit module, MultiChangeSignaller signaller) {
    ensureAdaptable(module.findPrimaryType(), signaller);
  }

  public void ensureAdaptable(IClassFile classFile) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(classFile, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(IClassFile classFile, MultiChangeSignaller signaller) {
    try {
      ensureAdaptable(classFile.getType(), signaller);
    } catch (Exception e) {
      openError("Error ensuring '" + classFile.getElementName() + "' instrumented", e);
    }
  }

  public void ensureAdaptable(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(IType type, MultiChangeSignaller signaller) {
    if (isInterface(type)) {
      internalEnsureAdaptable(type, signaller);
      return;
    }

    while (type != null) {
      if (!isInterface(type)) {
        if (!isAdaptable(type)) {
          internalEnsureAdaptable(type, signaller);
        }
      } else {
        break;
      }

      IType parentType = type;
      while (true) {
        try {
          String superTypeSig = parentType.getSuperclassTypeSignature();

          if (superTypeSig == null) {
            break;
          }

          String superTypeName = JdtUtils.getResolvedTypeName(superTypeSig, type);
          if (superTypeName == null || superTypeName.equals("java.lang.Object")) {
            break;
          } else {
            IType superType = JdtUtils.findType(m_javaProject, superTypeName);

            if (superType == null) {
              break;
            } else if (!isInterface(superType)) {
              if (!isAdaptable(superType)) {
                internalEnsureAdaptable(superType, signaller);
              } else {
                break;
              }
            }
            parentType = superType;
          }
        } catch (JavaModelException jme) {
          break;
        }
      }

      type = type.getDeclaringType();
    }
  }

  public void internalEnsureAdaptable(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAdaptable(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAdaptable(IType type, MultiChangeSignaller signaller) {
    String postscript = isInterface(type) ? "+" : "";
    internalEnsureAdaptable(PatternHelper.getFullyQualifiedName(type) + postscript, signaller);

    if (!isBootJarClass(type)) {
      int filter = IJavaSearchScope.SYSTEM_LIBRARIES;
      IJavaElement[] elements = new IJavaElement[] { m_javaProject };
      IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements, filter);

      if (scope.encloses(type)) {
        internalEnsureBootJarClass(type, signaller);
      }
    }
  }

  public void ensureAdaptable(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    if (packageDecl != null && !isAdaptable(packageDecl)) {
      internalEnsureAdaptable(packageDecl, signaller);
    }
  }

  public void internalEnsureAdaptable(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAdaptable(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAdaptable(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    internalEnsureAdaptable(PatternHelper.getWithinPattern(packageDecl), signaller);
  }

  public void ensureAdaptable(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (fragment != null && !isAdaptable(fragment)) {
      internalEnsureAdaptable(fragment, signaller);
    }
  }

  public void internalEnsureAdaptable(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAdaptable(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAdaptable(IPackageFragment fragment, MultiChangeSignaller signaller) {
    internalEnsureAdaptable(PatternHelper.getWithinPattern(fragment), signaller);
  }

  public void ensureAdaptable(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null && !isAdaptable(javaProject)) {
      internalEnsureAdaptable(javaProject, signaller);
    }
  }

  public void internalEnsureAdaptable(IJavaProject javaProject, MultiChangeSignaller signaller) {
    IPackageFragment[] fragments = getSourceFragments(javaProject);

    for (int i = 0; i < fragments.length; i++) {
      if (!isAdaptable(fragments[i])) {
        internalEnsureAdaptable(fragments[i], signaller);
      }
    }
  }

  public void ensureAdaptable(String classExpr) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAdaptable(classExpr, signaller);
    signaller.signal(m_project);
  }

  public void ensureAdaptable(String classExpr, MultiChangeSignaller signaller) {
    if (isInstrumentationNotNeeded(classExpr)) return;
    if (isAdaptable(classExpr)) {
      internalEnsureAdaptable(classExpr, signaller);
    }
  }

  public Include addIncludeRule(String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    Include include = classes.addNewInclude();
    include.setClassExpression(classExpr);
    return include;
  }

  public void internalEnsureAdaptable(String classExpr) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAdaptable(classExpr, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAdaptable(String classExpr, MultiChangeSignaller signaller) {
    if (isInstrumentationNotNeeded(classExpr)) return;
    addIncludeRule(classExpr);
    signaller.includeRulesChanged = true;
  }

  public void ensureNotAdaptable(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAdaptable(element, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAdaptable(IJavaElement element, MultiChangeSignaller signaller) {
    if (element instanceof ICompilationUnit) {
      ensureNotAdaptable((ICompilationUnit) element, signaller);
    } else if (element instanceof IType) {
      ensureNotAdaptable((IType) element, signaller);
    } else if (element instanceof IPackageDeclaration) {
      ensureNotAdaptable((IPackageDeclaration) element, signaller);
    } else if (element instanceof IPackageFragment) {
      ensureNotAdaptable((IPackageFragment) element, signaller);
    } else if (element instanceof IJavaProject) {
      ensureNotAdaptable((IJavaProject) element, signaller);
    }
  }

  public void ensureNotAdaptable(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAdaptable(module, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAdaptable(ICompilationUnit module, MultiChangeSignaller signaller) {
    if (module != null) {
      internalEnsureNotAdaptable(module, signaller);
    }
  }

  public void internalEnsureNotAdaptable(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAdaptable(module, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAdaptable(ICompilationUnit module, MultiChangeSignaller signaller) {
    IType primaryType = module.findPrimaryType();
    if (primaryType != null) {
      internalEnsureNotAdaptable(primaryType, signaller);
    }
  }

  public void ensureNotAdaptable(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAdaptable(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAdaptable(IType type, MultiChangeSignaller signaller) {
    if (isAdaptable(type)) {
      baseEnsureNotAdaptable(type, signaller);
    }
  }

  public void baseEnsureNotAdaptable(IType type, MultiChangeSignaller signaller) {
    internalEnsureNotAdaptable(type, signaller);
  }

  public void internalEnsureNotAdaptable(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAdaptable(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAdaptable(IType type, MultiChangeSignaller signaller) {
    internalEnsureNotLocked(type, signaller);
    internalEnsureNotBootJarClass(type, signaller);
    internalEnsureNotAdaptable(PatternHelper.getFullyQualifiedName(type), signaller);

    try {
      IField[] fields = type.getFields();

      if (fields != null) {
        for (int i = 0; i < fields.length; i++) {
          internalEnsureNotRoot(fields[i], signaller);
        }
      }

      IType[] childTypes = type.getTypes();

      if (childTypes != null) {
        for (int i = 0; i < childTypes.length; i++) {
          internalEnsureNotAdaptable(childTypes[i], signaller);
          internalEnsureNotBootJarClass(childTypes[i], signaller);
        }
      }

      IMethod[] methods = type.getMethods();

      if (methods != null) {
        for (int i = 0; i < methods.length; i++) {
          internalEnsureNotLocked(methods[i], signaller);
          internalEnsureLocalMethod(methods[i], signaller);
        }
      }
    } catch (JavaModelException jme) {/**/
    }

    testRemoveInstrumentedClasses();
  }

  public void ensureNotAdaptable(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAdaptable(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAdaptable(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    if (isAdaptable(packageDecl)) {
      internalEnsureNotAdaptable(packageDecl, signaller);
    }
  }

  public void internalEnsureNotAdaptable(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAdaptable(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAdaptable(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    internalEnsureNotLocked(packageDecl, signaller);
    internalEnsureNotAdaptable(PatternHelper.getWithinPattern(packageDecl), signaller);
  }

  public void ensureNotAdaptable(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAdaptable(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAdaptable(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (isAdaptable(fragment)) {
      internalEnsureNotAdaptable(fragment, signaller);
    }
  }

  public void internalEnsureNotAdaptable(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAdaptable(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAdaptable(IPackageFragment fragment, MultiChangeSignaller signaller) {
    internalEnsureNotLocked(fragment, signaller);

    try {
      ICompilationUnit[] cus = fragment.getCompilationUnits();

      if (cus != null) {
        for (int i = 0; i < cus.length; i++) {
          internalEnsureNotAdaptable(cus[i], signaller);
        }
      }
    } catch (JavaModelException jme) {
      internalEnsureNotAdaptable(PatternHelper.getWithinPattern(fragment), signaller);
    }
  }

  public void ensureNotAdaptable(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAdaptable(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAdaptable(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      for (int i = 0; i < fragments.length; i++) {
        internalEnsureNotAdaptable(fragments[i], signaller);
      }
    }
  }

  public void ensureNotAdaptable(String classExpr) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAdaptable(classExpr, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAdaptable(String classExpr, MultiChangeSignaller signaller) {
    if (isAdaptable(classExpr)) {
      internalEnsureNotAdaptable(classExpr, signaller);
    }
  }

  public void internalEnsureNotAdaptable(String classExpr, MultiChangeSignaller signaller) {
    InstrumentedClasses classes = getInstrumentedClasses();

    if (classes != null) {
      int size = classes.sizeOfIncludeArray();

      for (int i = size - 1; i >= 0; i--) {
        String expr = classes.getIncludeArray(i).getClassExpression();

        if (m_patternHelper.matchesClass(expr, classExpr)) {
          classes.removeInclude(i);
          signaller.includeRulesChanged = true;
        }
      }
    }
  }

  public boolean isExcluded(IJavaElement element) {
    if (element instanceof ICompilationUnit) {
      return isExcluded((ICompilationUnit) element);
    } else if (element instanceof IType) {
      return isExcluded((IType) element);
    } else if (element instanceof IPackageDeclaration) {
      return isExcluded(element.getElementName());
    } else if (element instanceof IPackageFragment) {
      return isExcluded((IPackageFragment) element);
    } else if (element instanceof IJavaProject) { return isExcluded((IJavaProject) element); }

    return false;
  }

  public boolean isExcluded(ICompilationUnit module) {
    return isExcluded(module.findPrimaryType());
  }

  public boolean isExcluded(IType type) {
    return type != null && isExcluded(PatternHelper.getFullyQualifiedName(type));
  }

  private Boolean isExcluded(IPackageFragment fragment, InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      XmlObject[] objects = instrumentedClasses.selectPath("*");

      if (objects != null && objects.length > 0) {
        for (int i = objects.length - 1; i >= 0; i--) {
          XmlObject object = objects[i];

          if (object instanceof Include) {
            String expr = ((Include) object).getClassExpression();
            if (m_patternHelper.matchesPackageFragment(expr, fragment)) { return Boolean.FALSE; }
          } else if (object instanceof ClassExpression) {
            String expr = ((ClassExpression) object).getStringValue();
            if (m_patternHelper.matchesPackageFragment(expr, fragment)) { return Boolean.TRUE; }
          }
        }
      }
    }
    return null;
  }

  public boolean isExcluded(IPackageFragment fragment) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses instrumentedClasses = getInstrumentedClasses();

      if (instrumentedClasses != null) {
        Boolean excluded = isExcluded(fragment, instrumentedClasses);
        if (excluded != null) { return excluded; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        instrumentedClasses = modulesConfig.getApplication().getInstrumentedClasses();
        if (instrumentedClasses != null) {
          Boolean excluded = isExcluded(fragment, instrumentedClasses);
          if (excluded != null) { return excluded; }
        }
      }
    }

    return false;
  }

  public boolean isExcluded(IJavaProject javaProject) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);

      if (fragments.length > 0) {
        for (int i = 0; i < fragments.length; i++) {
          if (!isExcluded(fragments[i])) { return false; }
        }

        return true;
      }
    }

    return false;
  }

  public Boolean isExcluded(String classExpr, InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      XmlObject[] objects = instrumentedClasses.selectPath("*");

      if (objects != null && objects.length > 0) {
        for (int i = objects.length - 1; i >= 0; i--) {
          XmlObject object = objects[i];

          if (object instanceof Include) {
            String expr = ((Include) object).getClassExpression();
            if (m_patternHelper.matchesClass(expr, classExpr)) { return Boolean.FALSE; }
          } else if (object instanceof ClassExpression) {
            String expr = ((ClassExpression) object).getStringValue();
            if (m_patternHelper.matchesClass(expr, classExpr)) { return Boolean.TRUE; }
          }
        }
      }
    }
    return null;
  }

  public boolean isExcluded(String classExpr) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses instrumentedClasses = getInstrumentedClasses();

      if (instrumentedClasses != null) {
        Boolean excluded = isExcluded(classExpr, instrumentedClasses);
        if (excluded != null) { return excluded; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        instrumentedClasses = modulesConfig.getApplication().getInstrumentedClasses();
        if (instrumentedClasses != null) {
          Boolean excluded = isExcluded(classExpr, instrumentedClasses);
          if (excluded != null) { return excluded; }
        }
      }
    }

    return false;
  }

  public void ensureExcluded(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureExcluded(element, signaller);
    signaller.signal(m_project);
  }

  public void ensureExcluded(IJavaElement element, MultiChangeSignaller signaller) {
    if (element instanceof ICompilationUnit) {
      ensureExcluded((ICompilationUnit) element, signaller);
    } else if (element instanceof IType) {
      ensureExcluded((IType) element, signaller);
    } else if (element instanceof IPackageDeclaration) {
      ensureExcluded(element.getElementName(), signaller);
    } else if (element instanceof IPackageFragment) {
      ensureExcluded((IPackageFragment) element, signaller);
    } else if (element instanceof IJavaProject) {
      ensureExcluded((IJavaProject) element, signaller);
    }
  }

  public void ensureExcluded(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureExcluded(module, signaller);
    signaller.signal(m_project);
  }

  public void ensureExcluded(ICompilationUnit module, MultiChangeSignaller signaller) {
    if (module != null) {
      internalEnsureExcluded(module, signaller);
    }
  }

  public void internalEnsureExcluded(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureExcluded(module, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureExcluded(ICompilationUnit module, MultiChangeSignaller signaller) {
    internalEnsureExcluded(module.findPrimaryType(), signaller);
  }

  public void ensureExcluded(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureExcluded(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureExcluded(IType type, MultiChangeSignaller signaller) {
    if (type != null && !isExcluded(type)) {
      internalEnsureExcluded(type, signaller);
    }
  }

  public void internalEnsureExcluded(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureExcluded(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureExcluded(IType type, MultiChangeSignaller signaller) {
    internalEnsureExcluded(PatternHelper.getFullyQualifiedName(type), signaller);
  }

  public void ensureExcluded(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureExcluded(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureExcluded(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (fragment != null && !isExcluded(fragment)) {
      internalEnsureExcluded(fragment, signaller);
    }
  }

  public void internalEnsureExcluded(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureExcluded(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureExcluded(IPackageFragment fragment, MultiChangeSignaller signaller) {
    internalEnsureExcluded(PatternHelper.getWithinPattern(fragment), signaller);
  }

  public void ensureExcluded(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureExcluded(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureExcluded(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      for (int i = 0; i < fragments.length; i++) {
        internalEnsureExcluded(fragments[i], signaller);
      }
    }
  }

  public void ensureExcluded(String className) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureExcluded(className, signaller);
    signaller.signal(m_project);
  }

  public void ensureExcluded(String className, MultiChangeSignaller signaller) {
    if (className != null && !isExcluded(className)) {
      internalEnsureExcluded(className, signaller);
    }
  }

  public void internalEnsureExcluded(String className, MultiChangeSignaller signaller) {
    ensureInstrumentedClasses().addExclude(className);
    signaller.excludeRulesChanged = true;
  }

  public void ensureNotExcluded(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotExcluded(element, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotExcluded(IJavaElement element, MultiChangeSignaller signaller) {
    if (element instanceof ICompilationUnit) {
      ensureNotExcluded((ICompilationUnit) element, signaller);
    } else if (element instanceof IType) {
      ensureNotExcluded((IType) element, signaller);
    } else if (element instanceof IPackageFragment) {
      ensureNotExcluded((IPackageFragment) element, signaller);
    } else if (element instanceof IJavaProject) {
      ensureNotExcluded((IJavaProject) element, signaller);
    }
  }

  public void ensureNotExcluded(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotExcluded(module, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotExcluded(ICompilationUnit module, MultiChangeSignaller signaller) {
    if (module != null) {
      internalEnsureNotExcluded(module, signaller);
    }
  }

  public void internalEnsureNotExcluded(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotExcluded(module, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotExcluded(ICompilationUnit module, MultiChangeSignaller signaller) {
    internalEnsureNotExcluded(module.findPrimaryType(), signaller);
  }

  public void ensureNotExcluded(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotExcluded(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotExcluded(IType type, MultiChangeSignaller signaller) {
    if (type != null && isExcluded(type)) {
      baseEnsureNotExcluded(PatternHelper.getFullyQualifiedName(type), signaller);
    }
  }

  public void baseEnsureNotExcluded(IType type, MultiChangeSignaller signaller) {
    internalEnsureNotExcluded(type, signaller);
  }

  public void internalEnsureNotExcluded(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotExcluded(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotExcluded(IType type, MultiChangeSignaller signaller) {
    internalEnsureNotExcluded(PatternHelper.getFullyQualifiedName(type), signaller);
  }

  public void ensureNotExcluded(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotExcluded(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotExcluded(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (fragment != null) {
      ensureNotExcluded(PatternHelper.getWithinPattern(fragment), signaller);
    }
  }

  public void internalEnsureNotExcluded(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotExcluded(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotExcluded(IPackageFragment fragment, MultiChangeSignaller signaller) {
    internalEnsureNotExcluded(PatternHelper.getWithinPattern(fragment), signaller);
  }

  public void ensureNotExcluded(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotExcluded(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotExcluded(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);
      for (int i = 0; i < fragments.length; i++) {
        internalEnsureNotExcluded(fragments[i], signaller);
      }

    }
  }

  public void ensureNotExcluded(String classExpr) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotExcluded(classExpr, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotExcluded(String classExpr, MultiChangeSignaller signaller) {
    if (isExcluded(classExpr)) {
      baseEnsureNotExcluded(classExpr, signaller);
    }
  }

  public void baseEnsureNotExcluded(String classExpr, MultiChangeSignaller signaller) {
    internalEnsureNotExcluded(classExpr, signaller);
  }

  public void internalEnsureNotExcluded(String classExpr) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotExcluded(classExpr, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotExcluded(String classExpr, MultiChangeSignaller signaller) {
    InstrumentedClasses classes = getInstrumentedClasses();

    if (classes != null) {
      int size = classes.sizeOfExcludeArray();

      for (int i = size - 1; i >= 0; i--) {
        String expr = classes.getExcludeArray(i);

        if (m_patternHelper.matchesClass(expr, classExpr)) {
          classes.removeExclude(i);
          signaller.excludeRulesChanged = true;
        }
      }
    }
  }

  public static String getFullName(IField field) {
    IType type = field.getDeclaringType();
    String parentType = PatternHelper.getFullyQualifiedName(type);
    String fieldName = field.getElementName();

    return parentType + "." + fieldName;
  }

  public Boolean isRoot(IField field, Roots roots) {
    if (roots != null) {
      for (int i = 0; i < roots.sizeOfRootArray(); i++) {
        if (m_patternHelper.matchesField(roots.getRootArray(i), field)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isRoot(IField field) {
    if (field == null) return false;

    TcConfig config = getConfig();
    if (config != null) {
      Roots roots = getRoots();

      if (roots != null) {
        Boolean isRoot = isRoot(field, roots);
        if (isRoot != null) { return isRoot; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        roots = modulesConfig.getApplication().getRoots();
        if (roots != null) {
          Boolean isRoot = isRoot(field, roots);
          if (isRoot != null) { return isRoot; }
        }
      }
    }

    return false;
  }

  public boolean isRoot(String className, String fieldName) {
    return isRoot(className + "." + fieldName);
  }

  public boolean isRoot(String fieldName) {
    return isRoot(getField(fieldName));
  }

  public void ensureRoot(IField field) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureRoot(field, signaller);
    signaller.signal(m_project);
  }

  public void ensureRoot(IField field, MultiChangeSignaller signaller) {
    if (!isRoot(field)) {
      IType fieldType = getFieldType(field);

      if (fieldType != null && !isInterface(fieldType) && !isAdaptable(fieldType)) {
        internalEnsureAdaptable(fieldType, signaller);
      }

      if (isTransient(field)) {
        ensureNotTransient(field, signaller);
      }

      internalEnsureRoot(getFullName(field), signaller);
    }
  }

  public IType getFieldType(IField field) {
    try {
      String sig = field.getTypeSignature();
      IType declaringType = field.getDeclaringType();
      String typeName = JdtUtils.getResolvedTypeName(sig, declaringType);

      if (typeName != null) { return JdtUtils.findType(m_javaProject, typeName); }
    } catch (JavaModelException jme) {/**/
    }

    return null;
  }

  public IField getField(String fieldName) {
    int lastDot = fieldName.lastIndexOf('.');

    if (lastDot != -1) {
      String declaringTypeName = fieldName.substring(0, lastDot);

      try {
        IType declaringType = JdtUtils.findType(m_javaProject, declaringTypeName);

        if (declaringType != null) { return declaringType.getField(fieldName.substring(lastDot + 1)); }
      } catch (JavaModelException jme) {/**/
      }
    }

    return null;
  }

  public IType getFieldType(String fieldName) {
    IField field = getField(fieldName);

    if (field != null) {
      try {
        String sig = field.getTypeSignature();
        IType declaringType = field.getDeclaringType();
        String typeName = JdtUtils.getResolvedTypeName(sig, declaringType);

        return JdtUtils.findType(m_javaProject, typeName);
      } catch (JavaModelException jme) {/**/
      }

    }

    return null;
  }

  public void ensureRoot(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureRoot(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void ensureRoot(String fieldName, MultiChangeSignaller signaller) {
    if (!isRoot(fieldName)) {
      IType fieldType = getFieldType(fieldName);

      if (fieldType != null && !isAdaptable(fieldType)) {
        ensureAdaptable(fieldType, signaller);
      }

      if (isTransient(fieldName)) {
        ensureNotTransient(fieldName, signaller);
      }

      internalEnsureRoot(fieldName, signaller);
    }
  }

  public void internalEnsureRoot(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureRoot(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureRoot(String fieldName, MultiChangeSignaller signaller) {
    ensureRoots().addNewRoot().setFieldName(fieldName);
    signaller.rootsChanged = true;
  }

  public void ensureNotRoot(IField field) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotRoot(field, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotRoot(IField field, MultiChangeSignaller signaller) {
    if (field != null && isRoot(field)) {
      baseEnsureNotRoot(field, signaller);
    }
  }

  public void baseEnsureNotRoot(IField field, MultiChangeSignaller signaller) {
    internalEnsureNotRoot(field, signaller);
  }

  public void internalEnsureNotRoot(IField field) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotRoot(field, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotRoot(IField field, MultiChangeSignaller signaller) {
    internalEnsureNotRoot(getFullName(field), signaller);
  }

  public void ensureNotRoot(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotRoot(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotRoot(String fieldName, MultiChangeSignaller signaller) {
    if (isRoot(fieldName)) {
      baseEnsureNotRoot(fieldName, signaller);
    }
  }

  public void baseEnsureNotRoot(String fieldName, MultiChangeSignaller signaller) {
    internalEnsureNotRoot(fieldName, signaller);
  }

  public void internalEnsureNotRoot(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotRoot(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotRoot(String fieldName, MultiChangeSignaller signaller) {
    Roots roots = getRoots();

    if (roots != null) {
      int size = roots.sizeOfRootArray();

      for (int i = size - 1; i >= 0; i--) {
        if (fieldName.equals(roots.getRootArray(i).getFieldName())) {
          roots.removeRoot(i);
          signaller.rootsChanged = true;
        }
      }

      testRemoveRoots();
    }
  }

  public void renameRoot(String fieldName, String newFieldName) {
    if (isRoot(fieldName)) {
      internalRenameRoot(fieldName, newFieldName);
      m_plugin.fireRootsChanged(m_project);
    }
  }

  public void internalRenameRoot(String fieldName, String newFieldName) {
    Roots roots = getRoots();

    if (roots != null) {
      for (int i = 0; i < roots.sizeOfRootArray(); i++) {
        Root root = roots.getRootArray(i);

        if (fieldName.equals(root.getFieldName())) {
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
    return isTransient(className + "." + fieldName);
  }

  private Boolean isTransient(String fieldName, TransientFields transientFields) {
    if (transientFields != null) {
      for (int i = 0; i < transientFields.sizeOfFieldNameArray(); i++) {
        if (fieldName.equals(transientFields.getFieldNameArray(i))) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isTransient(String fieldName) {
    TcConfig config = getConfig();

    if (config != null) {
      TransientFields transientFields = getTransientFields();

      if (transientFields != null) {
        Boolean isTransient = isTransient(fieldName, transientFields);
        if (isTransient != null) { return isTransient; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        transientFields = modulesConfig.getApplication().getTransientFields();
        if (transientFields != null) {
          Boolean isTransient = isTransient(fieldName, transientFields);
          if (isTransient != null) { return isTransient; }
        }
      }
    }

    return false;
  }

  public void ensureTransient(IField field) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureTransient(field, signaller);
    signaller.signal(m_project);
  }

  public void ensureTransient(IField field, MultiChangeSignaller signaller) {
    if (field != null && !isTransient(field)) {
      if (isRoot(field)) {
        internalEnsureNotRoot(field, signaller);
      }

      IType declaringType = field.getDeclaringType();
      if (!isAdaptable(declaringType)) {
        internalEnsureAdaptable(declaringType, signaller);
      }

      internalEnsureTransient(getFullName(field), signaller);
    }
  }

  public void ensureTransient(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureTransient(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void ensureTransient(String fieldName, MultiChangeSignaller signaller) {
    if (!isTransient(fieldName)) {
      IField field = getField(fieldName);
      if (field != null) {
        IType fieldType = field.getDeclaringType();
        if (!isAdaptable(fieldType)) {
          ensureAdaptable(fieldType, signaller);
        }
      }

      if (isRoot(fieldName)) {
        ensureNotRoot(fieldName, signaller);
      }

      internalEnsureTransient(fieldName, signaller);
    }
  }

  public void internalEnsureTransient(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureTransient(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureTransient(String fieldName, MultiChangeSignaller signaller) {
    ensureTransientFields().addFieldName(fieldName);
    signaller.transientFieldsChanged = true;
  }

  public void ensureNotTransient(IField field) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotTransient(field, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotTransient(IField field, MultiChangeSignaller signaller) {
    if (field != null) {
      ensureNotTransient(getFullName(field), signaller);
    }
  }

  public void ensureNotTransient(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotTransient(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotTransient(String fieldName, MultiChangeSignaller signaller) {
    if (isTransient(fieldName)) {
      internalEnsureNotTransient(fieldName, signaller);
    }
  }

  public void internalEnsureNotTransient(String fieldName) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotTransient(fieldName, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotTransient(String fieldName, MultiChangeSignaller signaller) {
    TransientFields transients = getTransientFields();

    if (transients != null) {
      for (int i = transients.sizeOfFieldNameArray() - 1; i >= 0; i--) {
        if (fieldName.equals(transients.getFieldNameArray(i))) {
          transients.removeFieldName(i);
          signaller.transientFieldsChanged = true;
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
    DsoApplication dsoApp = ensureDsoApplication();
    TransientFields transients = dsoApp.getTransientFields();

    if (transients == null) {
      transients = dsoApp.addNewTransientFields();
    }

    return transients;
  }

  private void testRemoveTransientFields() {
    DsoApplication dsoApp = getDsoApplication();

    if (dsoApp != null) {
      TransientFields transients = dsoApp.getTransientFields();

      if (transients != null) {
        if (transients.sizeOfFieldNameArray() == 0) {
          dsoApp.unsetTransientFields();
          testRemoveDsoApplication();
        }
      }
    }
  }

  public boolean matches(String expression, MemberInfo methodInfo) {
    return m_patternHelper.matchesMember(expression, methodInfo);
  }

  public boolean matches(String expression, IMethod method) {
    return m_patternHelper.matchesMethod(expression, method);
  }

  public boolean isDistributedMethod(MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();

    if (binding != null && !binding.getDeclaringClass().isInterface()) { return isDistributedMethod(PatternHelper
        .methodDecl2IMethod(methodDecl)); }

    return false;
  }

  public boolean isDistributedMethod(IMethod method) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
    return methodInfo != null && isDistributedMethod(methodInfo);
  }

  private Boolean isDistributedMethod(MethodInfo methodInfo, DistributedMethods distributedMethods) {
    if (distributedMethods != null) {
      for (int i = 0; i < distributedMethods.sizeOfMethodExpressionArray(); i++) {
        String expr = distributedMethods.getMethodExpressionArray(i).getStringValue();
        if (m_patternHelper.matchesMember(expr, methodInfo)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isDistributedMethod(MethodInfo methodInfo) {
    TcConfig config = getConfig();

    if (config != null) {
      DistributedMethods distributedMethods = getDistributedMethods();

      if (distributedMethods != null) {
        Boolean isDistributedMethod = isDistributedMethod(methodInfo, distributedMethods);
        if (isDistributedMethod != null) { return isDistributedMethod; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        distributedMethods = modulesConfig.getApplication().getDistributedMethods();
        if (distributedMethods != null) {
          Boolean isDistributedMethod = isDistributedMethod(methodInfo, distributedMethods);
          if (isDistributedMethod != null) { return isDistributedMethod; }
        }
      }
    }

    return false;
  }

  public void ensureDistributedMethod(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureDistributedMethod(method, signaller);
    signaller.signal(m_project);
  }

  public void ensureDistributedMethod(IMethod method, MultiChangeSignaller signaller) {
    if (!isDistributedMethod(method)) {
      internalEnsureDistributedMethod(method, signaller);
    }
  }

  public void internalEnsureDistributedMethod(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureDistributedMethod(method, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureDistributedMethod(IMethod method, MultiChangeSignaller signaller) {
    IType declaringType = method.getDeclaringType();

    if (!isAdaptable(declaringType)) {
      internalEnsureAdaptable(declaringType, signaller);
    }

    DistributedMethods methods = ensureDistributedMethods();
    MethodExpression methodExpression = methods.addNewMethodExpression();

    try {
      methodExpression.setStringValue(PatternHelper.getJavadocSignature(method));
    } catch (JavaModelException jme) {
      openError("Error ensuring method '" + method.getElementName() + "' distributed", jme);
      return;
    }

    signaller.distributedMethodsChanged = true;
  }

  public void ensureLocalMethod(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureLocalMethod(method, signaller);
    signaller.signal(m_project);
  }

  public void ensureLocalMethod(IMethod method, MultiChangeSignaller signaller) {
    if (isDistributedMethod(method)) {
      internalEnsureLocalMethod(method, signaller);
    }
  }

  public void internalEnsureLocalMethod(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureLocalMethod(method, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureLocalMethod(IMethod method, MultiChangeSignaller signaller) {
    DistributedMethods methods = getDistributedMethods();

    if (methods != null) {
      for (int i = methods.sizeOfMethodExpressionArray() - 1; i >= 0; i--) {
        String expr = methods.getMethodExpressionArray(i).getStringValue();

        if (m_patternHelper.matchesMethod(expr, method)) {
          methods.removeMethodExpression(i);
          signaller.distributedMethodsChanged = true;
        }
      }

      testRemoveDistributedMethods();
    }
  }

  public boolean isLocked(IMethod method) {
    try {
      if (!method.getDeclaringType().isInterface()) { return isAutolocked(method) || isNameLocked(method); }
    } catch (JavaModelException jme) {/**/
    }

    return false;
  }

  public boolean isLocked(MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();

    if (binding != null && !binding.getDeclaringClass().isInterface()) { return isLocked(PatternHelper
        .methodDecl2IMethod(methodDecl)); }

    return false;
  }

  public boolean isAutolocked(IJavaElement element) {
    if (element instanceof IMethod) {
      return isAutolocked((IMethod) element);
    } else if (element instanceof IType) {
      return isAutolocked((IType) element);
    } else if (element instanceof ICompilationUnit) {
      return isAutolocked((ICompilationUnit) element);
    } else if (element instanceof IPackageDeclaration) {
      return isAutolocked((IPackageDeclaration) element);
    } else if (element instanceof IPackageFragment) {
      return isAutolocked((IPackageFragment) element);
    } else if (element instanceof IJavaProject) { return isAutolocked((IJavaProject) element); }

    return false;
  }

  public XmlObject getLock(IMethod method) {
    TcConfig config = getConfig();

    if (config != null) {
      MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
      Locks locks = getLocks();

      if (locks != null) {
        for (int i = locks.sizeOfAutolockArray() - 1; i >= 0; i--) {
          Autolock autolock = locks.getAutolockArray(i);
          String expr = autolock.getMethodExpression();

          if (m_patternHelper.matchesMember(expr, methodInfo)) { return autolock; }
        }

        for (int i = locks.sizeOfNamedLockArray() - 1; i >= 0; i--) {
          NamedLock namedLock = locks.getNamedLockArray(i);
          String expr = namedLock.getMethodExpression();

          if (m_patternHelper.matchesMember(expr, methodInfo)) { return namedLock; }
        }
      }
    }

    return null;
  }

  public boolean isAutolocked(IMethod method) {
    try {
      if (!method.getDeclaringType().isInterface()) {
        MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
        return methodInfo != null && isAutolocked(methodInfo);
      }
    } catch (JavaModelException jme) {/**/
    }

    return false;
  }

  private Boolean isAutolocked(MethodInfo methodInfo, Locks locks) {
    if (locks != null) {
      for (int i = 0; i < locks.sizeOfAutolockArray(); i++) {
        Autolock autolock = locks.getAutolockArray(i);
        String expr = autolock.getMethodExpression();

        if (m_patternHelper.matchesMember(expr, methodInfo)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isAutolocked(MethodInfo methodInfo) {
    TcConfig config = getConfig();

    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        Boolean isAutolocked = isAutolocked(methodInfo, locks);
        if (isAutolocked != null) { return isAutolocked; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        locks = modulesConfig.getApplication().getLocks();
        if (locks != null) {
          Boolean isAutolocked = isAutolocked(methodInfo, locks);
          if (isAutolocked != null) { return isAutolocked; }
        }
      }
    }

    return false;
  }

  public boolean isAutolocked(MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();

    if (binding != null && !binding.getDeclaringClass().isInterface()) { return isAutolocked(PatternHelper
        .methodDecl2IMethod(methodDecl)); }

    return false;
  }

  // TODO: use m_patternHelper.matchesType(expr, type) here?
  public Boolean isAutolocked(IType type, Locks locks) {
    if (locks != null) {
      String typeExpr = PatternHelper.getExecutionPattern(type);

      for (int i = 0; i < locks.sizeOfAutolockArray(); i++) {
        Autolock autolock = locks.getAutolockArray(i);
        String expr = autolock.getMethodExpression();

        if (typeExpr.equals(expr)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isAutolocked(IType type) {
    try {
      if (type.isInterface()) { return false; }
    } catch (JavaModelException jme) {/**/
    }

    TcConfig config = getConfig();
    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        Boolean isAutolocked = isAutolocked(type, locks);
        if (isAutolocked != null) { return isAutolocked; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        locks = modulesConfig.getApplication().getLocks();
        if (locks != null) {
          Boolean isAutolocked = isAutolocked(type, locks);
          if (isAutolocked != null) { return isAutolocked; }
        }
      }
    }

    return false;
  }

  public boolean isAutolocked(ICompilationUnit cu) {
    IType primaryType = cu.findPrimaryType();
    return primaryType != null ? isAutolocked(primaryType) : false;
  }

  public boolean isAutolocked(IPackageDeclaration packageDecl) {
    TcConfig config = getConfig();

    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        String fragExpr = PatternHelper.getExecutionPattern(packageDecl);

        for (int i = 0; i < locks.sizeOfAutolockArray(); i++) {
          Autolock autolock = locks.getAutolockArray(i);
          String expr = autolock.getMethodExpression();

          if (fragExpr.equals(expr)) { return true; }
        }
      }
    }

    return false;
  }

  public boolean isAutolocked(IPackageFragment fragment) {
    TcConfig config = getConfig();

    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        String fragExpr = PatternHelper.getExecutionPattern(fragment);

        for (int i = 0; i < locks.sizeOfAutolockArray(); i++) {
          Autolock autolock = locks.getAutolockArray(i);
          String expr = autolock.getMethodExpression();

          if (fragExpr.equals(expr)) { return true; }
        }
      }
    }

    return false;
  }

  public boolean isAutolocked(IJavaProject javaProject) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);

      if (fragments.length > 0) {
        for (int i = 0; i < fragments.length; i++) {
          if (!isAutolocked(fragments[i])) { return false; }
        }

        return true;
      }

    }

    return false;
  }

  public boolean isNameLocked(IJavaElement element) {
    if (element instanceof IMethod) {
      return isNameLocked((IMethod) element);
    } else if (element instanceof IType) {
      return isNameLocked((IType) element);
    } else if (element instanceof IPackageDeclaration) {
      return isNameLocked((IPackageDeclaration) element);
    } else if (element instanceof IPackageFragment) {
      return isNameLocked((IPackageFragment) element);
    } else if (element instanceof IJavaProject) { return isNameLocked((IJavaProject) element); }

    return false;
  }

  public boolean isNameLocked(IMethod method) {
    try {
      if (!method.getDeclaringType().isInterface()) {
        MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
        return methodInfo != null && isNameLocked(methodInfo);
      }
    } catch (JavaModelException jme) {/**/
    }

    return false;
  }

  private Boolean isNameLocked(MethodInfo methodInfo, Locks locks) {
    if (locks != null) {
      for (int i = 0; i < locks.sizeOfNamedLockArray(); i++) {
        NamedLock namedLock = locks.getNamedLockArray(i);
        String expr = namedLock.getMethodExpression();

        if (m_patternHelper.matchesMember(expr, methodInfo)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isNameLocked(MethodInfo methodInfo) {
    TcConfig config = getConfig();

    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        Boolean isNameLocked = isNameLocked(methodInfo, locks);
        if (isNameLocked != null) { return isNameLocked; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        locks = modulesConfig.getApplication().getLocks();
        if (locks != null) {
          Boolean isNameLocked = isNameLocked(methodInfo, locks);
          if (isNameLocked != null) { return isNameLocked; }
        }
      }
    }

    return false;
  }

  public boolean isNameLocked(MethodDeclaration methodDecl) {
    IMethodBinding binding = methodDecl.resolveBinding();

    if (binding != null && !binding.getDeclaringClass().isInterface()) { return isNameLocked(PatternHelper
        .methodDecl2IMethod(methodDecl)); }

    return false;
  }

  private Boolean isNameLocked(IType type, Locks locks) {
    if (locks != null) {
      String typeExpr = PatternHelper.getExecutionPattern(type);

      for (int i = 0; i < locks.sizeOfNamedLockArray(); i++) {
        NamedLock namedLock = locks.getNamedLockArray(i);
        String expr = namedLock.getMethodExpression();

        if (typeExpr.equals(expr)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isNameLocked(IType type) {
    try {
      if (type.isInterface()) { return false; }
    } catch (JavaModelException jme) {/**/
    }

    TcConfig config = getConfig();
    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        Boolean isNameLocked = isNameLocked(type, locks);
        if (isNameLocked != null) { return isNameLocked; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        locks = modulesConfig.getApplication().getLocks();
        if (locks != null) {
          Boolean isNameLocked = isNameLocked(type, locks);
          if (isNameLocked != null) { return isNameLocked; }
        }
      }
    }

    return false;
  }

  private Boolean isNameLocked(IPackageDeclaration packageDecl, Locks locks) {
    String fragExpr = PatternHelper.getExecutionPattern(packageDecl);

    for (int i = 0; i < locks.sizeOfNamedLockArray(); i++) {
      NamedLock namedLock = locks.getNamedLockArray(i);
      String expr = namedLock.getMethodExpression();

      if (fragExpr.equals(expr)) { return Boolean.TRUE; }
    }
    return null;
  }

  public boolean isNameLocked(IPackageDeclaration packageDecl) {
    TcConfig config = getConfig();

    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        Boolean isNameLocked = isNameLocked(packageDecl, locks);
        if (isNameLocked != null) { return isNameLocked; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        locks = modulesConfig.getApplication().getLocks();
        if (locks != null) {
          Boolean isNameLocked = isNameLocked(packageDecl, locks);
          if (isNameLocked != null) { return isNameLocked; }
        }
      }
    }

    return false;
  }

  private Boolean isNameLocked(IPackageFragment fragment, Locks locks) {
    if (locks != null) {
      String fragExpr = PatternHelper.getExecutionPattern(fragment);

      for (int i = 0; i < locks.sizeOfNamedLockArray(); i++) {
        NamedLock namedLock = locks.getNamedLockArray(i);
        String expr = namedLock.getMethodExpression();

        if (fragExpr.equals(expr)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isNameLocked(IPackageFragment fragment) {
    TcConfig config = getConfig();

    if (config != null) {
      Locks locks = getLocks();

      if (locks != null) {
        Boolean isNameLocked = isNameLocked(fragment, locks);
        if (isNameLocked != null) { return isNameLocked; }
      }

      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
      if (modulesConfig != null) {
        locks = modulesConfig.getApplication().getLocks();
        if (locks != null) {
          Boolean isNameLocked = isNameLocked(fragment, locks);
          if (isNameLocked != null) { return isNameLocked; }
        }
      }
    }

    return false;
  }

  public boolean isNameLocked(IJavaProject javaProject) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);

      if (fragments.length > 0) {
        for (int i = 0; i < fragments.length; i++) {
          if (!isNameLocked(fragments[i])) { return false; }
        }

        return true;
      }
    }

    return false;
  }

  public void ensureNameLocked(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNameLocked(element, element.getElementName(), LockLevel.WRITE, signaller);
    signaller.signal(m_project);
  }

  public void ensureNameLocked(IJavaElement element, MultiChangeSignaller signaller) {
    ensureNameLocked(element, element.getElementName(), LockLevel.WRITE, signaller);
  }

  public void ensureNameLocked(IJavaElement element, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNameLocked(element, name, level, signaller);
    signaller.signal(m_project);
  }

  public void ensureNameLocked(IJavaElement element, String name, LockLevel.Enum level, MultiChangeSignaller signaller) {
    if (element instanceof IMethod) {
      ensureNameLocked((IMethod) element, name, level, signaller);
    } else if (element instanceof IType) {
      ensureNameLocked((IType) element, name, level, signaller);
    } else if (element instanceof IPackageDeclaration) {
      ensureNameLocked((IPackageDeclaration) element, name, level, signaller);
    } else if (element instanceof IPackageFragment) {
      ensureNameLocked((IPackageFragment) element, name, level, signaller);
    } else if (element instanceof IJavaProject) {
      ensureNameLocked((IJavaProject) element, name, level, signaller);
    }
  }

  public void ensureNameLocked(IMethod method, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNameLocked(method, name, level, signaller);
    signaller.signal(m_project);
  }

  public void ensureNameLocked(IMethod method, String name, LockLevel.Enum level, MultiChangeSignaller signaller) {
    if (!isNameLocked(method)) {
      internalEnsureNameLocked(method, name, level, signaller);
    }
  }

  public NamedLock addNewNamedLock(final String name, final IMethod method, final LockLevel.Enum level,
                                   MultiChangeSignaller signaller) throws JavaModelException {
    return addNewNamedLock(name, PatternHelper.getJavadocSignature(method), level, signaller);
  }

  public NamedLock addNewNamedLock(final String name, final String expr, final LockLevel.Enum level,
                                   MultiChangeSignaller signaller) {
    Locks locks = ensureLocks();
    NamedLock lock = locks.addNewNamedLock();

    lock.setMethodExpression(expr);
    lock.setLockLevel(level);
    lock.setLockName(name);
    signaller.namedLocksChanged = true;

    for (int i = locks.sizeOfAutolockArray() - 1; i >= 0; i--) {
      Autolock autoLock = locks.getAutolockArray(i);
      if (expr.equals(autoLock.getMethodExpression())) {
        locks.removeAutolock(i);
        signaller.autolocksChanged = true;
      }
    }

    return lock;
  }

  public void internalEnsureNameLocked(IMethod method, String name, LockLevel.Enum level, MultiChangeSignaller signaller) {
    IType declaringType = method.getDeclaringType();

    if (!isAdaptable(declaringType)) {
      internalEnsureAdaptable(declaringType);
      signaller.includeRulesChanged = true;
    }

    try {
      addNewNamedLock(name, PatternHelper.getJavadocSignature(method), level, signaller);
    } catch (JavaModelException jme) {
      openError("Error ensuring method '" + method.getElementName() + "' name-locked", jme);
      return;
    }
  }

  public void ensureNameLocked(IType type, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNameLocked(type, name, level, signaller);
    signaller.signal(m_project);
  }

  public void ensureNameLocked(IType type, String name, LockLevel.Enum level, MultiChangeSignaller signaller) {
    if (!isNameLocked(type)) {
      internalEnsureNameLocked(type, name, level, signaller);
    }
  }

  public void internalEnsureNameLocked(IType type, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNameLocked(type, name, level, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNameLocked(IType type, String name, LockLevel.Enum level, MultiChangeSignaller signaller) {
    if (!isAdaptable(type)) {
      internalEnsureAdaptable(type, signaller);
    }

    addNewNamedLock(name, PatternHelper.getExecutionPattern(type), level, signaller);
  }

  public void ensureNameLocked(IPackageDeclaration packageDecl, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNameLocked(packageDecl, name, level, signaller);
    signaller.signal(m_project);
  }

  public void ensureNameLocked(IPackageDeclaration packageDecl, String name, LockLevel.Enum level,
                               MultiChangeSignaller signaller) {
    if (!isNameLocked(packageDecl)) {
      internalEnsureNameLocked(packageDecl, name, level, signaller);
    }
  }

  public void internalEnsureNameLocked(IPackageDeclaration packageDecl, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNameLocked(packageDecl, name, level, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNameLocked(IPackageDeclaration packageDecl, String name, LockLevel.Enum level,
                                       MultiChangeSignaller signaller) {
    if (!isAdaptable(packageDecl)) {
      internalEnsureAdaptable(packageDecl, signaller);
    }

    addNewNamedLock(name, PatternHelper.getExecutionPattern(packageDecl), level, signaller);
  }

  public void ensureNameLocked(IPackageFragment fragment, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNameLocked(fragment, name, level, signaller);
    signaller.signal(m_project);
  }

  public void ensureNameLocked(IPackageFragment fragment, String name, LockLevel.Enum level,
                               MultiChangeSignaller signaller) {
    if (!isNameLocked(fragment)) {
      internalEnsureNameLocked(fragment, name, level, signaller);
    }
  }

  public void internalEnsureNameLocked(IPackageFragment fragment, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNameLocked(fragment, name, level, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNameLocked(IPackageFragment fragment, String name, LockLevel.Enum level,
                                       MultiChangeSignaller signaller) {
    if (!isAdaptable(fragment)) {
      internalEnsureAdaptable(fragment, signaller);
    }

    addNewNamedLock(name, PatternHelper.getExecutionPattern(fragment), level, signaller);
  }

  public void ensureNameLocked(IJavaProject javaProject, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNameLocked(javaProject, name, level, signaller);
    signaller.signal(m_project);
  }

  public void ensureNameLocked(IJavaProject javaProject, String name, LockLevel.Enum level,
                               MultiChangeSignaller signaller) {
    if (javaProject != null && !isNameLocked(javaProject)) {
      internalEnsureNameLocked(javaProject, name, level, signaller);
    }
  }

  public void internalEnsureNameLocked(IJavaProject javaProject, String name, LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNameLocked(javaProject, name, level, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNameLocked(IJavaProject javaProject, String name, LockLevel.Enum level,
                                       MultiChangeSignaller signaller) {
    IPackageFragment[] fragments = getSourceFragments(javaProject);

    for (int i = 0; i < fragments.length; i++) {
      if (!isNameLocked(fragments[i])) {
        internalEnsureNameLocked(fragments[i], name, level, signaller);
      }
    }
  }

  public void ensureAutolocked(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAutolocked(element, signaller);
    signaller.signal(m_project);
  }

  public void ensureAutolocked(IJavaElement element, MultiChangeSignaller signaller) {
    if (element instanceof IMethod) {
      ensureAutolocked((IMethod) element, signaller);
    } else if (element instanceof IType) {
      ensureAutolocked((IType) element, signaller);
    } else if (element instanceof ICompilationUnit) {
      ensureAutolocked((ICompilationUnit) element, signaller);
    } else if (element instanceof IPackageDeclaration) {
      ensureAutolocked((IPackageDeclaration) element, signaller);
    } else if (element instanceof IPackageFragment) {
      ensureAutolocked((IPackageFragment) element, signaller);
    } else if (element instanceof IJavaProject) {
      ensureAutolocked((IJavaProject) element, signaller);
    }
  }

  public void ensureAutolocked(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAutolocked(method, signaller);
    signaller.signal(m_project);
  }

  public void ensureAutolocked(IMethod method, MultiChangeSignaller signaller) {
    if (!isAutolocked(method)) {
      internalEnsureAutolocked(method, LockLevel.WRITE, signaller);
    }
  }

  public void ensureAutolocked(IMethod method, LockLevel.Enum level, MultiChangeSignaller signaller) {
    if (!isAutolocked(method)) {
      internalEnsureAutolocked(method, level, signaller);
    }
  }

  public void internalEnsureAutolocked(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAutolocked(method, LockLevel.WRITE, signaller);
    signaller.signal(m_project);
  }

  public Autolock addNewAutolock(final IMethod method, final LockLevel.Enum level, MultiChangeSignaller signaller)
      throws JavaModelException {
    return addNewAutolock(PatternHelper.getJavadocSignature(method), level, signaller);
  }

  public Autolock addNewAutolock(final String expr, final LockLevel.Enum level) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    Autolock lock = addNewAutolock(expr, level, signaller);
    signaller.signal(m_project);
    return lock;
  }

  public Autolock addNewAutolock(final String expr, final LockLevel.Enum level, MultiChangeSignaller signaller) {
    Locks locks = ensureLocks();
    Autolock lock = locks.addNewAutolock();
    lock.setMethodExpression(expr);
    lock.setLockLevel(level);
    signaller.autolocksChanged = true;

    for (int i = locks.sizeOfNamedLockArray() - 1; i >= 0; i--) {
      NamedLock namedLock = locks.getNamedLockArray(i);
      if (expr.equals(namedLock.getMethodExpression())) {
        locks.removeNamedLock(i);
        signaller.namedLocksChanged = true;
      }
    }

    return lock;
  }

  public void internalEnsureAutolocked(IMethod method, final LockLevel.Enum level, MultiChangeSignaller signaller) {
    IType declaringType = method.getDeclaringType();

    if (!isAdaptable(declaringType)) {
      internalEnsureAdaptable(declaringType, signaller);
    }

    try {
      addNewAutolock(PatternHelper.getJavadocSignature(method), level, signaller);
    } catch (JavaModelException jme) {
      openError("Error ensuring method '" + method.getElementName() + "' auto-locked", jme);
      return;
    }
  }

  public void ensureAutolocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAutolocked(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureAutolocked(IType type, MultiChangeSignaller signaller) {
    if (!isAutolocked(type)) {
      internalEnsureAutolocked(type, signaller);
    }
  }

  public void internalEnsureAutolocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAutolocked(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAutolocked(IType type, MultiChangeSignaller signaller) {
    if (!isAdaptable(type)) {
      internalEnsureAdaptable(type, signaller);
    }

    addNewAutolock(PatternHelper.getExecutionPattern(type), LockLevel.WRITE, signaller);
  }

  public void ensureAutolocked(ICompilationUnit cu, MultiChangeSignaller signaller) {
    IType primaryType = cu.findPrimaryType();
    if (primaryType != null) {
      ensureAutolocked(primaryType, signaller);
    }
  }

  public void ensureAutolocked(ICompilationUnit cu) {
    IType primaryType = cu.findPrimaryType();
    if (primaryType != null) {
      ensureAutolocked(primaryType);
    }
  }

  public void ensureAutolocked(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAutolocked(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void ensureAutolocked(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    if (!isAutolocked(packageDecl)) {
      internalEnsureAutolocked(packageDecl, signaller);
    }
  }

  public void internalEnsureAutolocked(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAutolocked(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAutolocked(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    if (!isAdaptable(packageDecl)) {
      internalEnsureAdaptable(packageDecl, signaller);
    }
    addNewAutolock(PatternHelper.getExecutionPattern(packageDecl), LockLevel.WRITE, signaller);
  }

  public void ensureAutolocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAutolocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureAutolocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (!isAutolocked(fragment)) {
      internalEnsureAutolocked(fragment, signaller);
    }
  }

  public void internalEnsureAutolocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAutolocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAutolocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (!isAdaptable(fragment)) {
      internalEnsureAdaptable(fragment, signaller);
    }
    addNewAutolock(PatternHelper.getExecutionPattern(fragment), LockLevel.WRITE, signaller);
  }

  public void ensureAutolocked(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureAutolocked(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureAutolocked(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null && !isAutolocked(javaProject)) {
      internalEnsureAutolocked(javaProject, signaller);
    }
  }

  public void internalEnsureAutolocked(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureAutolocked(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureAutolocked(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);

      for (int i = 0; i < fragments.length; i++) {
        if (!isAutolocked(fragments[i])) {
          internalEnsureAutolocked(fragments[i], signaller);
        }
      }
    }
  }

  public void ensureNotNameLocked(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotNameLocked(element, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotNameLocked(IJavaElement element, MultiChangeSignaller signaller) {
    if (element instanceof IMethod) {
      ensureNotNameLocked((IMethod) element, signaller);
    } else if (element instanceof IType) {
      ensureNotNameLocked((IType) element, signaller);
    } else if (element instanceof IPackageFragment) {
      ensureNotNameLocked((IPackageFragment) element, signaller);
    } else if (element instanceof IJavaProject) {
      ensureNotNameLocked((IJavaProject) element, signaller);
    }
  }

  public void ensureNotNameLocked(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotNameLocked(method, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotNameLocked(IMethod method, MultiChangeSignaller signaller) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);
    if (methodInfo != null) {
      ensureNotNameLocked(methodInfo, signaller);
    }
  }

  public void ensureNotNameLocked(MethodInfo methodInfo) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotNameLocked(methodInfo, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotNameLocked(MethodInfo methodInfo, MultiChangeSignaller signaller) {
    if (isNameLocked(methodInfo)) {
      internalEnsureNotNameLocked(methodInfo, signaller);
    }
  }

  public void internalEnsureNotNameLocked(MethodInfo methodInfo) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotNameLocked(methodInfo, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotNameLocked(MethodInfo methodInfo, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      for (int i = locks.sizeOfNamedLockArray() - 1; i >= 0; i--) {
        String expr = locks.getNamedLockArray(i).getMethodExpression();

        if (m_patternHelper.matchesMember(expr, methodInfo)) {
          locks.removeNamedLock(i);
          signaller.namedLocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotNameLocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotNameLocked(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotNameLocked(IType type, MultiChangeSignaller signaller) {
    if (isNameLocked(type)) {
      internalEnsureNotNameLocked(type, signaller);
    }
  }

  public void internalEnsureNotNameLocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotNameLocked(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotNameLocked(IType type, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      String typeExpr = PatternHelper.getExecutionPattern(type);

      for (int i = locks.sizeOfNamedLockArray() - 1; i >= 0; i--) {
        String expr = locks.getNamedLockArray(i).getMethodExpression();

        if (typeExpr.equals(expr)) {
          locks.removeNamedLock(i);
          signaller.namedLocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotNameLocked(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotNameLocked(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotNameLocked(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    if (isNameLocked(packageDecl)) {
      internalEnsureNotNameLocked(packageDecl, signaller);
    }
  }

  public void internalEnsureNotNameLocked(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotNameLocked(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotNameLocked(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      String fragExpr = PatternHelper.getExecutionPattern(packageDecl);

      for (int i = locks.sizeOfNamedLockArray() - 1; i >= 0; i--) {
        String expr = locks.getNamedLockArray(i).getMethodExpression();

        if (fragExpr.equals(expr)) {
          locks.removeNamedLock(i);
          signaller.namedLocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotNameLocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotNameLocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotNameLocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (isNameLocked(fragment)) {
      internalEnsureNotNameLocked(fragment, signaller);
    }
  }

  public void internalEnsureNotNameLocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotNameLocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotNameLocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      String fragExpr = PatternHelper.getExecutionPattern(fragment);

      for (int i = locks.sizeOfNamedLockArray() - 1; i >= 0; i--) {
        String expr = locks.getNamedLockArray(i).getMethodExpression();

        if (fragExpr.equals(expr)) {
          locks.removeNamedLock(i);
          signaller.namedLocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotNameLocked(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotNameLocked(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotNameLocked(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null && isNameLocked(javaProject)) {
      internalEnsureNotNameLocked(javaProject, signaller);
    }
  }

  public void internalEnsureNotNameLocked(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotNameLocked(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotNameLocked(IJavaProject javaProject, MultiChangeSignaller signaller) {
    IPackageFragment[] fragments = getSourceFragments(javaProject);

    for (int i = 0; i < fragments.length; i++) {
      if (isNameLocked(fragments[i])) {
        internalEnsureNotNameLocked(fragments[i], signaller);
      }
    }
  }

  public void ensureNotAutolocked(IJavaElement element) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAutolocked(element, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAutolocked(IJavaElement element, MultiChangeSignaller signaller) {
    if (element instanceof IMethod) {
      ensureNotAutolocked((IMethod) element, signaller);
    } else if (element instanceof IType) {
      ensureNotAutolocked((IType) element, signaller);
    } else if (element instanceof ICompilationUnit) {
      ensureNotAutolocked((ICompilationUnit) element, signaller);
    } else if (element instanceof IPackageFragment) {
      ensureNotAutolocked((IPackageFragment) element, signaller);
    } else if (element instanceof IJavaProject) {
      ensureNotAutolocked((IJavaProject) element, signaller);
    }
  }

  public void ensureNotAutolocked(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAutolocked(method, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAutolocked(IMethod method, MultiChangeSignaller signaller) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);

    if (methodInfo != null) {
      ensureNotAutolocked(methodInfo, signaller);
    }
  }

  public void ensureNotAutolocked(MethodInfo methodInfo) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAutolocked(methodInfo, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAutolocked(MethodInfo methodInfo, MultiChangeSignaller signaller) {
    if (isAutolocked(methodInfo)) {
      internalEnsureNotAutolocked(methodInfo, signaller);
    }
  }

  public void internalEnsureNotAutolocked(MethodInfo methodInfo) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAutolocked(methodInfo, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAutolocked(MethodInfo methodInfo, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      for (int i = locks.sizeOfAutolockArray() - 1; i >= 0; i--) {
        String expr = locks.getAutolockArray(i).getMethodExpression();

        if (m_patternHelper.matchesMember(expr, methodInfo)) {
          locks.removeAutolock(i);
          signaller.autolocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotAutolocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAutolocked(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAutolocked(IType type, MultiChangeSignaller signaller) {
    if (isAutolocked(type)) {
      internalEnsureNotAutolocked(type, signaller);
    }
  }

  public void internalEnsureNotAutolocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAutolocked(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAutolocked(IType type, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      String typeExpr = PatternHelper.getExecutionPattern(type);

      for (int i = locks.sizeOfAutolockArray() - 1; i >= 0; i--) {
        String expr = locks.getAutolockArray(i).getMethodExpression();

        if (typeExpr.equals(expr)) {
          locks.removeAutolock(i);
          signaller.autolocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotAutolocked(ICompilationUnit cu) {
    IType primaryType = cu.findPrimaryType();
    if (primaryType != null) {
      ensureNotAutolocked(primaryType);
    }
  }

  public void ensureNotAutolocked(ICompilationUnit cu, MultiChangeSignaller signaller) {
    IType primaryType = cu.findPrimaryType();
    if (primaryType != null) {
      ensureNotAutolocked(primaryType, signaller);
    }
  }

  public void ensureNotAutolocked(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAutolocked(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAutolocked(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    if (isAutolocked(packageDecl)) {
      internalEnsureNotAutolocked(packageDecl, signaller);
    }
  }

  public void internalEnsureNotAutolocked(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAutolocked(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAutolocked(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      String fragExpr = PatternHelper.getExecutionPattern(packageDecl);

      for (int i = locks.sizeOfAutolockArray() - 1; i >= 0; i--) {
        String expr = locks.getAutolockArray(i).getMethodExpression();

        if (fragExpr.equals(expr)) {
          locks.removeAutolock(i);
          signaller.autolocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotAutolocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAutolocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAutolocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (isAutolocked(fragment)) {
      internalEnsureNotAutolocked(fragment, signaller);
    }
  }

  public void internalEnsureNotAutolocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAutolocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAutolocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    Locks locks = getLocks();

    if (locks != null) {
      String fragExpr = PatternHelper.getExecutionPattern(fragment);

      for (int i = locks.sizeOfAutolockArray() - 1; i >= 0; i--) {
        String expr = locks.getAutolockArray(i).getMethodExpression();

        if (fragExpr.equals(expr)) {
          locks.removeAutolock(i);
          signaller.autolocksChanged = true;
        }
      }

      testRemoveLocks();
    }
  }

  public void ensureNotAutolocked(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotAutolocked(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotAutolocked(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null && isAutolocked(javaProject)) {
      internalEnsureNotAutolocked(javaProject, signaller);
    }
  }

  public void internalEnsureNotAutolocked(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotAutolocked(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotAutolocked(IJavaProject javaProject, MultiChangeSignaller signaller) {
    IPackageFragment[] fragments = getSourceFragments(javaProject);

    for (int i = 0; i < fragments.length; i++) {
      if (isAutolocked(fragments[i])) {
        internalEnsureNotAutolocked(fragments[i], signaller);
      }
    }
  }

  public void ensureNotLocked(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotLocked(method, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotLocked(IMethod method, MultiChangeSignaller signaller) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);

    if (methodInfo != null) {
      ensureNotLocked(methodInfo, signaller);
    }
  }

  public void ensureNotLocked(MethodInfo methodInfo) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotLocked(methodInfo, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotLocked(MethodInfo methodInfo, MultiChangeSignaller signaller) {
    if (methodInfo != null) {
      if (isAutolocked(methodInfo)) {
        internalEnsureNotAutolocked(methodInfo, signaller);
      }
      if (isNameLocked(methodInfo)) {
        internalEnsureNotNameLocked(methodInfo, signaller);
      }
    }
  }

  public void internalEnsureNotLocked(IMethod method) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotLocked(method, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotLocked(IMethod method, MultiChangeSignaller signaller) {
    MethodInfo methodInfo = m_patternHelper.getMethodInfo(method);

    if (method != null) {
      internalEnsureNotLocked(methodInfo, signaller);
    }
  }

  public void internalEnsureNotLocked(MethodInfo methodInfo) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotLocked(methodInfo, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotLocked(MethodInfo methodInfo, MultiChangeSignaller signaller) {
    if (isAutolocked(methodInfo)) {
      internalEnsureNotAutolocked(methodInfo, signaller);
    }
    if (isNameLocked(methodInfo)) {
      internalEnsureNotNameLocked(methodInfo, signaller);
    }
  }

  public void ensureNotLocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotLocked(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotLocked(IType type, MultiChangeSignaller signaller) {
    if (isAutolocked(type)) {
      internalEnsureNotAutolocked(type, signaller);
    }
    if (isNameLocked(type)) {
      internalEnsureNotNameLocked(type, signaller);
    }
  }

  public void internalEnsureNotLocked(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotLocked(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotLocked(IType type, MultiChangeSignaller signaller) {
    if (isAutolocked(type)) {
      internalEnsureNotAutolocked(type, signaller);
    }

    if (isNameLocked(type)) {
      internalEnsureNotNameLocked(type, signaller);
    }
  }

  public void ensureNotLocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotLocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotLocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (isAutolocked(fragment)) {
      internalEnsureNotAutolocked(fragment, signaller);
    }
    if (isNameLocked(fragment)) {
      internalEnsureNotNameLocked(fragment, signaller);
    }
  }

  public void internalEnsureNotLocked(IPackageDeclaration packageDecl) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotLocked(packageDecl, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotLocked(IPackageDeclaration packageDecl, MultiChangeSignaller signaller) {
    if (isAutolocked(packageDecl)) {
      internalEnsureNotAutolocked(packageDecl, signaller);
    }

    if (isNameLocked(packageDecl)) {
      internalEnsureNotNameLocked(packageDecl, signaller);
    }
  }

  public void internalEnsureNotLocked(IPackageFragment fragment) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotLocked(fragment, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotLocked(IPackageFragment fragment, MultiChangeSignaller signaller) {
    if (isAutolocked(fragment)) {
      internalEnsureNotAutolocked(fragment, signaller);
    }

    if (isNameLocked(fragment)) {
      internalEnsureNotNameLocked(fragment, signaller);
    }
  }

  public void ensureNotLocked(IJavaProject javaProject) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotLocked(javaProject, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotLocked(IJavaProject javaProject, MultiChangeSignaller signaller) {
    if (javaProject != null) {
      IPackageFragment[] fragments = getSourceFragments(javaProject);

      for (int i = 0; i < fragments.length; i++) {
        if (isAutolocked(fragments[i])) {
          internalEnsureNotAutolocked(fragments[i], signaller);
        }
        if (isNameLocked(fragments[i])) {
          internalEnsureNotNameLocked(fragments[i], signaller);
        }
      }
    }
  }

  public boolean isBootJarClass(ICompilationUnit module) {
    return isBootJarClass(module.findPrimaryType());
  }

  public boolean isBootJarClass(IType type) {
    return isBootJarClass(PatternHelper.getFullyQualifiedName(type));
  }

  private Boolean isBootJarClass(String className, AdditionalBootJarClasses abjc) {
    if (abjc != null) {
      String[] includes = abjc.getIncludeArray();

      for (int i = 0; i < includes.length; i++) {
        if (m_patternHelper.matchesClass(includes[i], className)) { return Boolean.TRUE; }
      }
    }
    return null;
  }

  public boolean isBootJarClass(String className) {
    AdditionalBootJarClasses abjc = getAdditionalBootJarClasses();

    if (abjc != null) {
      Boolean isBootJarClass = isBootJarClass(className, abjc);
      if (isBootJarClass != null) { return isBootJarClass; }
    }

    ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);
    if (modulesConfig != null) {
      abjc = modulesConfig.getApplication().getAdditionalBootJarClasses();
      if (abjc != null) {
        Boolean isBootJarClass = isBootJarClass(className, abjc);
        if (isBootJarClass != null) { return isBootJarClass; }
      }
    }

    return false;
  }

  public void ensureBootJarClass(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureBootJarClass(module, signaller);
    signaller.signal(m_project);
  }

  public void ensureBootJarClass(ICompilationUnit module, MultiChangeSignaller signaller) {
    ensureBootJarClass(module.findPrimaryType(), signaller);
  }

  public void ensureBootJarClass(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureBootJarClass(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureBootJarClass(IType type, MultiChangeSignaller signaller) {
    ensureBootJarClass(PatternHelper.getFullyQualifiedName(type), signaller);
  }

  public void internalEnsureBootJarClass(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureBootJarClass(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureBootJarClass(IType type, MultiChangeSignaller signaller) {
    internalEnsureBootJarClass(PatternHelper.getFullyQualifiedName(type), signaller);
  }

  public void ensureBootJarClass(String className) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureBootJarClass(className, signaller);
    signaller.signal(m_project);
  }

  public void ensureBootJarClass(String className, MultiChangeSignaller signaller) {
    if (isInstrumentationNotNeeded(className)) return;
    if (!isBootJarClass(className)) {
      internalEnsureBootJarClass(className, signaller);
    }
  }

  public void internalEnsureBootJarClass(String className) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureBootJarClass(className, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureBootJarClass(String className, MultiChangeSignaller signaller) {
    if (isInstrumentationNotNeeded(className)) return;
    ensureAdditionalBootJarClasses().addInclude(className);
    signaller.bootClassesChanged = true;
  }

  public void ensureNotBootJarClass(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotBootJarClass(module, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotBootJarClass(ICompilationUnit module, MultiChangeSignaller signaller) {
    if (module != null) {
      internalEnsureNotBootJarClass(module, signaller);
    }
  }

  public void internalEnsureNotBootJarClass(ICompilationUnit module) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotBootJarClass(module, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotBootJarClass(ICompilationUnit module, MultiChangeSignaller signaller) {
    internalEnsureNotBootJarClass(module.findPrimaryType(), signaller);
  }

  public void ensureNotBootJarClass(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotBootJarClass(type, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotBootJarClass(IType type, MultiChangeSignaller signaller) {
    if (type != null && isBootJarClass(type)) {
      internalEnsureNotBootJarClass(type, signaller);
    }
  }

  public void internalEnsureNotBootJarClass(IType type) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotBootJarClass(type, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotBootJarClass(IType type, MultiChangeSignaller signaller) {
    internalEnsureNotBootJarClass(PatternHelper.getFullyQualifiedName(type), signaller);
  }

  public void ensureNotBootJarClass(String className) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    ensureNotBootJarClass(className, signaller);
    signaller.signal(m_project);
  }

  public void ensureNotBootJarClass(String className, MultiChangeSignaller signaller) {
    if (isBootJarClass(className)) {
      internalEnsureNotBootJarClass(className, signaller);
    }
  }

  public void internalEnsureNotBootJarClass(String className) {
    MultiChangeSignaller signaller = new MultiChangeSignaller();
    internalEnsureNotBootJarClass(className, signaller);
    signaller.signal(m_project);
  }

  public void internalEnsureNotBootJarClass(String className, MultiChangeSignaller signaller) {
    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();

    if (classes != null) {
      String[] includes = classes.getIncludeArray();

      for (int i = includes.length - 1; i >= 0; i--) {
        if (m_patternHelper.matchesClass(includes[i], className)) {
          classes.removeInclude(i);
          signaller.includeRulesChanged = true;
        }
      }

      testRemoveAdditionalBootJarClasses();
    }
  }

  public boolean hasBootJarClasses() {
    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();
    return classes != null && classes.sizeOfIncludeArray() > 0;
  }

  public boolean hasModules() {
    Modules modules = getModules();
    return modules != null && modules.sizeOfModuleArray() > 0;
  }

  // Validation support

  public void validateAll() {
    if (getConfig() != null) {
      // DISABLING VALIDATING FOR ANYTHING THAT CAN TAKE A PATTERN
      // validateLocks();
      // validateInstrumentedClasses();
      validateRoots();
      validateTransientFields();
      validateBootJarClasses();
      validateModules();
    }
  }

  private static String LOCK_PROBLEM_MARKER = "org.terracotta.dso.LockMethodProblemMarker";

  private static String getRawString(XmlString xmlString) {
    if (xmlString == null) return null;
    String s = xmlString.toString();
    if (s != null) {
      s = s.substring(s.indexOf('>') + 1);
      int end = s.indexOf('<');
      if (end != -1) {
        s = s.substring(0, end);
      }
    }
    return s;
  }

  public void validateLocks() {
    clearConfigProblemMarkersOfType(LOCK_PROBLEM_MARKER);

    Locks locks = getLocks();

    if (locks != null) {
      Autolock[] autoLocks = locks.getAutolockArray();
      NamedLock[] namedLocks = locks.getNamedLockArray();

      if (autoLocks != null) {
        for (int i = 0; i < autoLocks.length; i++) {
          validateLockMethodExpression(getRawString(autoLocks[i].xgetMethodExpression()));
        }
      }

      if (namedLocks != null) {
        for (int i = 0; i < namedLocks.length; i++) {
          validateLockMethodExpression(getRawString(namedLocks[i].xgetMethodExpression()));
        }
      }
    }
  }

  private static String ROOT_PROBLEM_MARKER = "org.terracotta.dso.RootProblemMarker";

  public void validateRoots() {
    clearConfigProblemMarkersOfType(ROOT_PROBLEM_MARKER);

    Roots roots = getRoots();

    if (roots != null) {
      Root[] rootArray = roots.getRootArray();

      for (int i = 0; i < rootArray.length; i++) {
        validateRoot(rootArray[i]);
      }
    }
  }

  private static boolean isInterface(IType type) {
    try {
      return type.isInterface();
    } catch (JavaModelException jme) {
      return false;
    }
  }

  private static String[]          PRIMITIVE_NAMES = new String[] { "java.lang.String", "java.lang.Integer",
      "java.lang.Boolean", "java.lang.Double", "java.lang.Character", "java.lang.Byte" };

  private static ArrayList<String> PRIMITIVES      = new ArrayList<String>(Arrays.asList(PRIMITIVE_NAMES));

  private static boolean isPrimitive(IType type) {
    try {
      String name = type.getFullyQualifiedName();
      return PRIMITIVES.contains(name);
    } catch (Exception e) {
      return false;
    }
  }

  private void validateRoot(Root root) {
    String rootName = getRawString(root.xgetFieldName());
    String msg = validateField(rootName);

    if (msg == null) {
      IField field = getField(root.getFieldName());
      IType fieldType = getFieldType(field);

      if (fieldType != null && !isInterface(fieldType) && !isPrimitive(fieldType) && !isAdaptable(fieldType)
          && !isBootJarClass(fieldType) && !m_plugin.isBootClass(m_project, fieldType)
          && !isInstrumentationNotNeeded(fieldType)) {
        String fullName = PatternHelper.getFullyQualifiedName(fieldType);
        msg = "Root type '" + fullName + "' not instrumented";
      }
    }

    if (msg != null) {
      reportConfigProblem(rootName, msg, ROOT_PROBLEM_MARKER);
    }
  }

  private static String TRANSIENT_PROBLEM_MARKER = "org.terracotta.dso.TransientProblemMarker";

  public void validateTransientFields() {
    clearConfigProblemMarkersOfType(TRANSIENT_PROBLEM_MARKER);

    TransientFields transientFields = getTransientFields();

    if (transientFields != null) {
      for (int i = 0; i < transientFields.sizeOfFieldNameArray(); i++) {
        String field = getRawString(transientFields.xgetFieldNameArray(i));
        validateTransientField(field);
      }
    }
  }

  private void validateTransientField(String fieldName) {
    String msg = validateField(fieldName);

    if (msg == null) {
      IField field = getField(fieldName);
      IType declaringType = field.getDeclaringType();

      if (declaringType != null && !isAdaptable(declaringType) && !isBootJarClass(declaringType)) {
        String fullName = PatternHelper.getFullyQualifiedName(declaringType);
        msg = "Declaring type '" + fullName + "' not instrumented";
      }
    }

    if (msg != null) {
      reportConfigProblem(fieldName, msg, TRANSIENT_PROBLEM_MARKER);
    }
  }

  private static String INSTRUMENTED_PROBLEM_MARKER = "org.terracotta.dso.InstrumentedProblemMarker";

  public void validateInstrumentedClasses() {
    clearConfigProblemMarkersOfType(INSTRUMENTED_PROBLEM_MARKER);

    InstrumentedClasses instrumentedClasses = getInstrumentedClasses();

    if (instrumentedClasses != null) {
      validateIncludes(instrumentedClasses);
      validateExcludes(instrumentedClasses);
    }
  }

  private void validateIncludes(InstrumentedClasses instrumentedClasses) {
    for (int i = 0; i < instrumentedClasses.sizeOfIncludeArray(); i++) {
      Include include = instrumentedClasses.getIncludeArray(i);
      String expr = getRawString(include.xgetClassExpression());

      validateInstrumentedTypeExpression(expr);
    }
  }

  private void validateExcludes(InstrumentedClasses instrumentedClasses) {
    for (int i = 0; i < instrumentedClasses.sizeOfExcludeArray(); i++) {
      String exclude = getRawString(instrumentedClasses.xgetExcludeArray(i));
      validateInstrumentedTypeExpression(exclude);
    }
  }

  private void validateInstrumentedTypeExpression(String typeExpr) {
    String msg = null;
    String expr = typeExpr != null ? typeExpr.trim() : null;

    if (expr != null && (expr.indexOf('*') != -1 || expr.indexOf('+') != -1)) { return; }

    try {
      if (expr != null && JdtUtils.findType(m_javaProject, expr) != null) { return; }
    } catch (JavaModelException jme) {/**/
    }

    if (expr != null && expr.length() > 0) {
      String prefix = findTypeExpressionPrefix(expr);

      try {
        IPackageFragment[] fragments = m_javaProject.getPackageFragments();

        for (int i = 0; i < fragments.length; i++) {
          IPackageFragment fragment = fragments[i];

          if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE && fragment.getElementName().startsWith(prefix)) {
            ICompilationUnit[] cus = fragment.getCompilationUnits();

            for (int j = 0; j < cus.length; j++) {
              ICompilationUnit cu = cus[j];
              String cuType = PatternHelper.getFullyQualifiedName(cu.findPrimaryType());

              if (cuType.startsWith(prefix)) {
                IType[] types = cus[j].getAllTypes();

                for (int k = 0; k < types.length; k++) {
                  IType type = types[k];

                  if (!type.isInterface()) {
                    if (matchesType(expr, type)) { return; }
                  }
                }
              }
            }
          }
        }

        for (int i = 0; i < fragments.length; i++) {
          IPackageFragment fragment = fragments[i];

          if (fragment.getKind() == IPackageFragmentRoot.K_BINARY && fragment.getElementName().startsWith(prefix)) {
            IClassFile[] classFiles = fragment.getClassFiles();

            for (int j = 0; j < classFiles.length; j++) {
              IType type = classFiles[j].getType();
              String typeName = PatternHelper.getFullyQualifiedName(type);
              int flags = type.getFlags();

              if (typeName.startsWith(prefix)) {
                if (!type.isInterface() && !type.isAnonymous() && !type.isMember() && !type.isEnum()
                    && !type.isAnnotation() && !Flags.isProtected(flags) && !Flags.isPrivate(flags)
                    && !Flags.isStatic(flags)) {
                  if (matchesType(expr, type)) { return; }
                }
              }
            }
          }
        }

        msg = "No type matches expression '" + expr + "'";
      } catch (JavaModelException jme) {
        msg = jme.getMessage();
      }
    } else {
      msg = "Empty type expression";
    }

    if (msg != null) {
      reportConfigProblem(typeExpr, msg, INSTRUMENTED_PROBLEM_MARKER);
    }
  }

  private String findTypeExpressionPrefix(String typeExpr) {
    String[] elems = StringUtils.split(typeExpr, '.');
    char[] codes = new char[] { '*', '?', '.', '$' };
    ArrayList<String> list = new ArrayList<String>();

    for (int i = 0; i < elems.length; i++) {
      String elem = elems[i];

      if (elem.length() > 0 && StringUtils.containsNone(elems[i], codes)) {
        list.add(elem);
      }
    }

    return StringUtils.join(list.toArray(), '.');
  }

  private boolean matchesType(String typeExpr, IType type) {
    String name = PatternHelper.getFullyQualifiedName(type);
    int nameLen = name.length();
    int exprLen = typeExpr.length();

    if (typeExpr.equals(name)) { return true; }

    for (int z = 0; z < exprLen; z++) {
      char ec = typeExpr.charAt(z);

      if (z == nameLen || name.charAt(z) != ec) {
        if (ec == '.' || ec == '*' || ec == '?') { return m_patternHelper.matchesType(typeExpr, type); }
        break;
      }
    }

    return false;
  }

  private void validateLockMethodExpression(String methodExpr) {
    String msg = validateMethodExpression(methodExpr);

    if (msg != null) {
      reportConfigProblem(methodExpr, msg, LOCK_PROBLEM_MARKER);
    }
  }

  private static String DISTRIBUTED_METHOD_PROBLEM_MARKER = "org.terracotta.dso.DistributedMethodProblemMarker";

  public void validateDistributedMethods() {
    clearConfigProblemMarkersOfType(DISTRIBUTED_METHOD_PROBLEM_MARKER);

    DistributedMethods distributedMethods = getDistributedMethods();

    if (distributedMethods != null) {
      for (int i = 0; i < distributedMethods.sizeOfMethodExpressionArray(); i++) {
        String methodExpr = distributedMethods.getMethodExpressionArray(i).getStringValue();
        validateDistributedMethodExpression(methodExpr);
      }
    }
  }

  private void validateDistributedMethodExpression(String methodExpr) {
    String msg = validateMethodExpression(methodExpr);

    if (msg != null) {
      reportConfigProblem(methodExpr, msg, DISTRIBUTED_METHOD_PROBLEM_MARKER);
    }
  }

  private String validateField(String fullName) {
    String msg = null;
    int lastDotIndex;

    if (fullName != null) {
      fullName = fullName.trim();
    }

    if (fullName != null && (fullName.length() > 0) && (lastDotIndex = fullName.lastIndexOf('.')) != -1) {
      String className = fullName.substring(0, lastDotIndex).replace('$', '.');
      String fieldName = fullName.substring(lastDotIndex + 1);

      try {
        IType type;
        IField field;

        if ((type = JdtUtils.findType(m_javaProject, className)) == null) {
          msg = "Class not found: " + className;
        } else if ((field = type.getField(fieldName)) == null || !field.exists()) {
          msg = "No such field: " + fieldName;
        }
      } catch (JavaModelException jme) {
        msg = jme.getMessage();
      }
    } else {
      msg = "Must be a fully-qualified field name";
    }

    return msg;
  }

  private String validateMethodExpression(String methodExpr) {
    String msg = null;

    if (methodExpr != null && methodExpr.length() > 0) {
      try {
        m_patternHelper.testValidateMethodExpression(methodExpr);
      } catch (Exception e) {
        return e.getMessage();
      }

      try {
        String prefix = findMethodExpressionPrefix(methodExpr);

        if (prefix != null && prefix.length() > 0) {
          try {
            IType type = JdtUtils.findType(m_javaProject, prefix);

            if (type != null) {
              IMethod[] methods = type.getMethods();

              for (int m = 0; m < methods.length; m++) {
                if (m_patternHelper.matchesMethod(methodExpr, methods[m])) { return null; }
              }
            }
          } catch (JavaModelException jme) {/**/
          }
        }

        IPackageFragment[] fragments = m_javaProject.getPackageFragments();

        for (int i = 0; i < fragments.length; i++) {
          IPackageFragment fragment = fragments[i];

          if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE && fragment.getElementName().startsWith(prefix)) {
            ICompilationUnit[] cus = fragment.getCompilationUnits();

            for (int j = 0; j < cus.length; j++) {
              ICompilationUnit cu = cus[j];
              String cuType = PatternHelper.getFullyQualifiedName(cu.findPrimaryType());

              if (cuType.startsWith(prefix)) {
                IType[] types = cus[j].getAllTypes();

                for (int k = 0; k < types.length; k++) {
                  IType type = types[k];

                  if (!type.isInterface() && !type.isAnonymous()) {
                    if (matchesMethod(methodExpr, type)) { return null; }
                  }
                }
              }
            }
          }
        }

        for (int i = 0; i < fragments.length; i++) {
          IPackageFragment fragment = fragments[i];

          if (fragment.getKind() == IPackageFragmentRoot.K_BINARY && fragment.getElementName().startsWith(prefix)) {
            IClassFile[] classFiles = fragment.getClassFiles();

            for (int j = 0; j < classFiles.length; j++) {
              IType type = classFiles[j].getType();
              String typeName = PatternHelper.getFullyQualifiedName(type);
              int flags = type.getFlags();

              if (typeName.startsWith(prefix)) {
                if (!type.isInterface() && !type.isAnonymous() && !type.isMember() && !type.isEnum()
                    && !type.isAnnotation() && !Flags.isPrivate(flags) && !Flags.isProtected(flags)
                    && !Flags.isStatic(flags)) {
                  if (matchesMethod(methodExpr, type)) { return null; }
                }
              }
            }
          }
        }

        msg = "No method matching expression '" + methodExpr + "'";
      } catch (JavaModelException jme) {
        msg = jme.getMessage();
      }
    } else {
      msg = "Must be a fully-qualified method name";
    }

    return msg;
  }

  private String findMethodExpressionPrefix(String methodExpr) {
    String[] comps = StringUtils.split(methodExpr);
    String exprBody = comps.length > 1 ? comps[1] : comps[0];
    String[] elems = StringUtils.split(exprBody, '.');
    char[] codes = new char[] { '*', '?', '(', ')', '$' };
    ArrayList<String> list = new ArrayList<String>();

    for (int i = 0; i < elems.length; i++) {
      String elem = elems[i];

      if (elem.length() > 0 && StringUtils.containsNone(elems[i], codes)) {
        list.add(elem);
      } else {
        break;
      }
    }

    return StringUtils.join(list.toArray(), '.');
  }

  private boolean matchesMethod(String methodExpr, IType type) {
    String[] comps = StringUtils.split(methodExpr);
    String exprBody = comps.length > 1 ? comps[1] : comps[0];
    int exprBodyLen = exprBody.length();
    String name = PatternHelper.getFullyQualifiedName(type);
    int nameLen = name.length();

    for (int z = 0; z < exprBodyLen; z++) {
      char ebc = exprBody.charAt(z);

      if (z == nameLen || ebc != name.charAt(z)) {
        if (ebc == '.' || ebc == '*' || ebc == '?' || ebc == '(') {
          try {
            IMethod[] methods = type.getMethods();

            for (int m = 0; m < methods.length; m++) {
              IMethod method = methods[m];

              if (m_patternHelper.matchesMethod(methodExpr, method)) { return true; }
            }
          } catch (JavaModelException jme) {
            return false;
          }
        }

        return false;
      }
    }

    // exact match
    return true;
  }

  private static String BOOT_CLASS_PROBLEM_MARKER = "org.terracotta.dso.BootClassProblemMarker";

  public void validateBootJarClasses() {
    clearConfigProblemMarkersOfType(BOOT_CLASS_PROBLEM_MARKER);

    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();

    if (classes != null) {
      for (int i = 0; i < classes.sizeOfIncludeArray(); i++) {
        String include = getRawString(classes.xgetIncludeArray(i));
        validateBootJarClass(include);
      }
    }
  }

  private void validateBootJarClass(String classname) {
    String msg = null;
    String expr = classname;

    if (expr != null) {
      expr = expr.trim();
    }

    if (isInstrumentationNotNeeded(classname)) {
      msg = "Type '" + classname + "' should never be instrumented";
    } else {
      BootClassHelper bch = m_plugin.getBootClassHelper(m_project);

      if (bch != null && bch.isAdaptable(classname)) {
        msg = "Type '" + classname + "' is a pre-instrumented (default) boot type";
      } else {
        try {
          if (expr != null && JdtUtils.findType(m_javaProject, expr) == null) {
            msg = "Cannot resolve type '" + expr + "'";
          }
        } catch (JavaModelException jme) {
          msg = "Cannot resolve type '" + expr + "'";
        }
      }
    }

    if (msg != null) {
      reportConfigProblem(classname, msg, BOOT_CLASS_PROBLEM_MARKER);
    }
  }

  private static String MODULE_PROBLEM_MARKER      = "org.terracotta.dso.ModuleProblemMarker";
  private static String MODULE_REPO_PROBLEM_MARKER = "org.terracotta.dso.ModuleRepoProblemMarker";

  private void validateModules() {
    clearConfigProblemMarkersOfType(MODULE_PROBLEM_MARKER);
    clearConfigProblemMarkersOfType(MODULE_REPO_PROBLEM_MARKER);

    Modules modules = getModules();
    if (modules != null) {
      ModulesConfiguration modulesConfig = m_plugin.getModulesConfiguration(m_project);

      for (String repo : modules.getRepositoryArray()) {
        if (repo.startsWith("file:")) {
          reportConfigProblem(repo, "File URLs have been deprecated - use file path instead",
                              MODULE_REPO_PROBLEM_MARKER);
        } else {
          File file = new File(ParameterSubstituter.substitute(repo));
          if (!file.exists()) {
            reportConfigProblem(repo, "Repository does not exist", MODULE_REPO_PROBLEM_MARKER);
          } else if (!file.isDirectory()) {
            reportConfigProblem(repo, "Repository is not a directory", MODULE_REPO_PROBLEM_MARKER);
          }
        }
      }

      for (Module module : modules.getModuleArray()) {
        ModuleInfo moduleInfo = modulesConfig.getModuleInfo(module);
        if (moduleInfo != null) {
          BundleException error = moduleInfo.getError();
          if (error != null) {
            String text = module.getName();
            reportConfigProblem(text, error.getMessage(), MODULE_PROBLEM_MARKER);
          }
        }
      }
    }
  }

  private void reportConfigProblem(String configText, String msg, String markerType) {
    ConfigurationEditor editor = m_plugin.getConfigurationEditor(m_project);

    if (editor != null) {
      editor.applyProblemToText(configText, msg, markerType);
    } else {
      IFile file = m_plugin.getConfigurationFile(m_project);
      InputStream in = null;

      try {
        String text = IOUtils.toString(in = file.getContents());
        Document doc = new Document(text);

        applyProblemToText(doc, configText, msg, markerType);
      } catch (Exception e) {
        m_plugin.openError("Problem reporting config problem", e);
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
  }

  public void applyProblemToText(IDocument doc, String text, String msg, String markerType) {
    TcPlugin plugin = TcPlugin.getDefault();
    FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(doc);
    IRegion region;

    try {
      text = "\\Q" + text + "\\E";
      if ((region = finder.find(0, text, true, true, false, true)) != null) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        int start = region.getOffset();
        int end = start + region.getLength();
        int line = doc.getLineOfOffset(start) - 1;

        MarkerUtilities.setMessage(map, msg);
        MarkerUtilities.setLineNumber(map, line);
        MarkerUtilities.setCharStart(map, start);
        MarkerUtilities.setCharEnd(map, end);

        map.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_HIGH));
        map.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_WARNING));
        map.put(IMarker.LOCATION, "line " + line);

        IFile configFile = plugin.getConfigurationFile(m_project);
        MarkerUtilities.createMarker(configFile, map, markerType);
      }
    } catch (Exception e2) {
      plugin.openError("Problem creating marker '" + markerType + "'", e2);
    }
  }

  public TcConfig getConfig() {
    return m_plugin.getConfiguration(m_project);
  }

  private DsoApplication ensureDsoApplication() {
    DsoApplication dsoApp = null;
    TcConfig config = getConfig();

    if (config != null) {
      Application app = config.getApplication();

      if (app == null) {
        app = config.addNewApplication();
      }

      if ((dsoApp = app.getDso()) == null) {
        dsoApp = app.addNewDso();
        dsoApp.addNewInstrumentedClasses();
      }
    }

    return dsoApp;
  }

  private Client getClient() {
    TcConfig config = getConfig();
    return config != null ? config.getClients() : null;
  }

  private Application getApplication() {
    TcConfig config = getConfig();
    return config != null ? config.getApplication() : null;
  }

  private void testRemoveApplication() {
    Application app = getApplication();

    if (app != null) {
      if (!app.isSetDso()) {
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

    if (dsoApp != null) {
      InstrumentedClasses classes = dsoApp.getInstrumentedClasses();
      boolean hasClasses = classes != null && (classes.sizeOfExcludeArray() > 0 || classes.sizeOfIncludeArray() > 0);

      if (!dsoApp.isSetAdditionalBootJarClasses() && !dsoApp.isSetDistributedMethods() && !hasClasses
          && !dsoApp.isSetLocks() && !dsoApp.isSetRoots() && !dsoApp.isSetTransientFields()) {
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
    Roots roots = dsoApp.getRoots();

    return roots != null ? roots : dsoApp.addNewRoots();
  }

  private void testRemoveRoots() {
    DsoApplication dsoApp = getDsoApplication();

    if (dsoApp != null) {
      Roots roots = dsoApp.getRoots();

      if (roots.sizeOfRootArray() == 0) {
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
    DsoApplication dsoApp = ensureDsoApplication();
    DistributedMethods methods = dsoApp.getDistributedMethods();

    if (methods == null) {
      methods = dsoApp.addNewDistributedMethods();
    }

    return methods;
  }

  private void testRemoveDistributedMethods() {
    DistributedMethods methods = getDistributedMethods();

    if (methods != null) {
      if (methods.sizeOfMethodExpressionArray() == 0) {
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
    Locks locks = dsoApp.getLocks();

    if (locks == null) {
      locks = dsoApp.addNewLocks();
    }

    return locks;
  }

  private void testRemoveLocks() {
    Locks locks = getLocks();

    if (locks != null) {
      if (locks.sizeOfAutolockArray() == 0 && locks.sizeOfNamedLockArray() == 0) {
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
    DsoApplication dsoApp = ensureDsoApplication();
    InstrumentedClasses classes = dsoApp.getInstrumentedClasses();

    if (classes == null) {
      classes = dsoApp.addNewInstrumentedClasses();
    }

    return classes;
  }

  private void testRemoveInstrumentedClasses() {
    InstrumentedClasses ic = getInstrumentedClasses();
    if (ic != null) {
      if (ic.sizeOfExcludeArray() == 0 && ic.sizeOfIncludeArray() == 0) {
        getDsoApplication().unsetInstrumentedClasses();
        testRemoveDsoApplication();
      }
    }
  }

  private Modules getModules() {
    Client client = getClient();
    return client != null ? client.getModules() : null;
  }

  private AdditionalBootJarClasses getAdditionalBootJarClasses() {
    return ensureDsoApplication().getAdditionalBootJarClasses();
  }

  private AdditionalBootJarClasses ensureAdditionalBootJarClasses() {
    DsoApplication dsoApp = ensureDsoApplication();
    AdditionalBootJarClasses abjc = dsoApp.getAdditionalBootJarClasses();

    return abjc != null ? abjc : dsoApp.addNewAdditionalBootJarClasses();
  }

  private void testRemoveAdditionalBootJarClasses() {
    DsoApplication dsoApp = getDsoApplication();

    if (dsoApp != null) {
      AdditionalBootJarClasses abjc = dsoApp.getAdditionalBootJarClasses();

      if (abjc.sizeOfIncludeArray() == 0) {
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
    ArrayList<IPackageFragment> list = new ArrayList<IPackageFragment>();

    try {
      IPackageFragment[] fragments = javaProject.getPackageFragments();

      for (int i = 0; i < fragments.length; i++) {
        if (isSourceFragment(fragments[i])) {
          list.add(fragments[i]);
        }
      }
    } catch (JavaModelException jme) {/**/
    }

    return list.toArray(new IPackageFragment[0]);

  }

  private static boolean isSourceFragment(IPackageFragment fragment) {
    try {
      return fragment.getKind() == IPackageFragmentRoot.K_SOURCE && hasCompilationUnits(fragment);
    } catch (JavaModelException jme) {
      return false;
    }
  }

  private static boolean hasCompilationUnits(IPackageFragment fragment) {
    try {
      IResource resource = fragment.getResource();
      int type = resource.getType();
      IContainer container = null;

      switch (type) {
        case IResource.PROJECT: {
          IProject project = (IProject) resource;
          String name = fragment.getElementName();

          if (name.equals(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH)) {
            container = project;
          } else {
            String path = fragment.getElementName().replace('.', '/');
            container = project.getFolder(project.getLocation().append(path));
          }
          break;
        }
        case IResource.FOLDER:
          container = (IFolder) resource;
          break;
      }

      if (container != null) {
        IResource[] members = container.members();

        for (int i = 0; i < members.length; i++) {
          IResource member = members[i];

          if (member.getType() == IResource.FILE && member.getFileExtension().equals("java")) { return true; }
        }
      }
    } catch (CoreException ce) {/**/
    }

    return false;
  }

  private void openError(String msg, Throwable t) {
    m_plugin.openError(msg, t);
  }

  public ConfigurationEditor getConfigurationEditor() {
    return m_plugin.getConfigurationEditor(m_project);
  }

  private void clearConfigProblemMarkersOfType(String markerType) {
    m_plugin.clearConfigProblemMarkersOfType(m_project, markerType);
  }
}
