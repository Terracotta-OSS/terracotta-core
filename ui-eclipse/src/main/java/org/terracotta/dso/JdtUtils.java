/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;

public class JdtUtils {
  private static final IJavaElement[] EMPTY_RESULT= new IJavaElement[0];

  /** 
   * Finds a type by its qualified type name (dot separated).
   * @param jproject The java project to search in
   * @param fullyQualifiedName The fully qualified name (type name with enclosing type names and package (all separated by dots))
   * @return The type found, or null if not existing
   */ 
  public static IType findType(IJavaProject jproject, String fullyQualifiedName) throws JavaModelException {
    //workaround for bug 22883
    fullyQualifiedName = fullyQualifiedName.replace('$', '.');
    IType type= jproject.findType(fullyQualifiedName, (IProgressMonitor)null);
    if (type != null)
      return type;
    IPackageFragmentRoot[] roots= jproject.getPackageFragmentRoots();
    for (int i= 0; i < roots.length; i++) {
      IPackageFragmentRoot root= roots[i];
      type= findType(root, fullyQualifiedName);
      if (type != null && type.exists())
        return type;
    } 
    return null;
  }

  private static IType findType(IPackageFragmentRoot root, String fullyQualifiedName) throws JavaModelException{
    IJavaElement[] children= root.getChildren();
    for (int i= 0; i < children.length; i++) {
      IJavaElement element= children[i];
      if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT){
        IPackageFragment pack= (IPackageFragment)element;
        if (! fullyQualifiedName.startsWith(pack.getElementName()))
          continue;
        IType type= findType(pack, fullyQualifiedName);
        if (type != null && type.exists())
          return type;
      }
    }   
    return null;
  }
  
  private static IType findType(IPackageFragment pack, String fullyQualifiedName) throws JavaModelException{
    ICompilationUnit[] cus= pack.getCompilationUnits();
    for (int i= 0; i < cus.length; i++) {
      ICompilationUnit unit= cus[i];
      IType type= findType(unit, fullyQualifiedName);
      if (type != null && type.exists())
        return type;
    }
    return null;
  }
  
  public static IMethod[] findMethods(IType type, String name) throws JavaModelException {
    ArrayList list = new ArrayList();
    IMethod[] methods = type.getMethods();
    for (int i= 0; i < methods.length; i++) {
      if(methods[i].getElementName().equals(name)) list.add(methods[i]);
    }
    return (IMethod[])list.toArray(new IMethod[0]);
  }
  
  private static IType findType(ICompilationUnit cu, String fullyQualifiedName) throws JavaModelException{
    IType[] types= cu.getAllTypes();
    for (int i= 0; i < types.length; i++) {
      IType type= types[i];
      if (getFullyQualifiedName(type).equals(fullyQualifiedName))
        return type;
    }
    return null;
  }

  /**
   * Returns the fully qualified name of the given type using '.' as separators.
   * This is a replace for IType.getFullyQualifiedTypeName
   * which uses '$' as separators. As '$' is also a valid character in an id
   * this is ambiguous. JavaCore PR: 1GCFUNT
   */
  public static String getFullyQualifiedName(IType type) {
    try {
      if (type.isBinary() && !type.isAnonymous()) {
        IType declaringType= type.getDeclaringType();
        if (declaringType != null) {
          return getFullyQualifiedName(declaringType) + '.' + type.getElementName();
        }
      }
    } catch (JavaModelException e) {
      // ignore
    }   
    return type.getFullyQualifiedName('.');
  }

  /**
   * Force a reconcile of a compilation unit.
   * @param unit
   */
  public static void reconcile(ICompilationUnit unit) throws JavaModelException {
    unit.reconcile(
        ICompilationUnit.NO_AST, 
        false /* don't force problem detection */, 
        null /* use primary owner */, 
        null /* no progress monitor */);
  }

  /**
   * Returns the package fragment root of <code>IJavaElement</code>. If the given
   * element is already a package fragment root, the element itself is returned.
   */
  public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
    return (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
  }

  /**
   * Resolves a type name in the context of the declaring type.
   * 
   * @param refTypeSig the type name in signature notation (for example 'QVector') this can also be an array type, but dimensions will be ignored.
   * @param declaringType the context for resolving (type where the reference was made in)
   * @return returns the fully qualified type name or build-in-type name. if a unresolved type couldn't be resolved null is returned
   */
  public static String getResolvedTypeName(String refTypeSig, IType declaringType) throws JavaModelException {
    int arrayCount= Signature.getArrayCount(refTypeSig);
    char type= refTypeSig.charAt(arrayCount);
    if (type == Signature.C_UNRESOLVED) {
      String name= ""; //$NON-NLS-1$
      int bracket= refTypeSig.indexOf(Signature.C_GENERIC_START, arrayCount + 1);
      if (bracket > 0)
        name= refTypeSig.substring(arrayCount + 1, bracket);
      else {
        int semi= refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
        if (semi == -1) {
          throw new IllegalArgumentException();
        }
        name= refTypeSig.substring(arrayCount + 1, semi);
      }
      String[][] resolvedNames= declaringType.resolveType(name);
      if (resolvedNames != null && resolvedNames.length > 0) {
        return concatenateName(resolvedNames[0][0], resolvedNames[0][1]);
      }
      return null;
    } else {
      return Signature.toString(refTypeSig.substring(arrayCount));
    }
  }

  public static String getResolvedTypeFileName(String refTypeSig, IType declaringType) throws JavaModelException {
    int arrayCount= Signature.getArrayCount(refTypeSig);
    char type= refTypeSig.charAt(arrayCount);
    if (type == Signature.C_UNRESOLVED) {
      String name= ""; //$NON-NLS-1$
      int bracket= refTypeSig.indexOf(Signature.C_GENERIC_START, arrayCount + 1);
      if (bracket > 0)
        name= refTypeSig.substring(arrayCount + 1, bracket);
      else {
        int semi= refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
        if (semi == -1) {
          throw new IllegalArgumentException();
        }
        name= refTypeSig.substring(arrayCount + 1, semi);
      }
      String[][] resolvedNames= declaringType.resolveType(name);
      if (resolvedNames != null && resolvedNames.length > 0) {
        return concatenateName(resolvedNames[0][0], resolvedNames[0][1].replace('.', '$'));
      }
      return "*";
    } else {
      return Signature.toString(refTypeSig.substring(arrayCount));
    }
  }

  /**
   * Determine if refTypeSig is unresolved.
   */
  public static boolean isTypeNameUnresolved(String refTypeSig) {
    return refTypeSig.charAt(Signature.getArrayCount(refTypeSig)) == Signature.C_UNRESOLVED;
  } 
    
  /**
   * Resolve refTypeSig in context of declaringType.
   */
  public static String resolveTypeName(String refTypeSig, IType declaringType) {
    int arrayCount= Signature.getArrayCount(refTypeSig);
    char type= refTypeSig.charAt(arrayCount);
    if (type == Signature.C_UNRESOLVED) {
      String name= ""; //$NON-NLS-1$
      int bracket= refTypeSig.indexOf(Signature.C_GENERIC_START, arrayCount + 1);
      if (bracket > 0)
        name= refTypeSig.substring(arrayCount + 1, bracket);
      else {
        int semi= refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
        if (semi == -1) {
          throw new IllegalArgumentException();
        }
        name= refTypeSig.substring(arrayCount + 1, semi);
      }
      try {
        String[][] resolvedNames= declaringType.resolveType(name);
        if (resolvedNames != null && resolvedNames.length > 0) {
          return JdtUtils.concatenateName(resolvedNames[0][0], resolvedNames[0][1]);
        }
        else {
          return "java.lang.Object";
        }
      } catch(JavaModelException jme) {
        return name;
      }
    }

    return refTypeSig;
  }
  
  /**
   * Resolve refTypeSig in context of declaringType into sb.
   */
  public static void resolveTypeName(String refTypeSig, IType declaringType, StringBuffer sb) {
    if(isTypeNameUnresolved(refTypeSig)) {
      sb.append(refTypeSig.substring(0, refTypeSig.indexOf(Signature.C_UNRESOLVED)));
      sb.append('L');
      sb.append(resolveTypeName(refTypeSig, declaringType));
      sb.append(';');
    }
    else {
      sb.append(refTypeSig);
    }
  }
  
  /**
   * Concatenates two names. Uses a dot for separation.
   * Both strings can be empty or <code>null</code>.
   */
  public static String concatenateName(String name1, String name2) {
    StringBuffer buf= new StringBuffer();
    if (name1 != null && name1.length() > 0) {
      buf.append(name1);
    }
    if (name2 != null && name2.length() > 0) {
      if (buf.length() > 0) {
        buf.append('.');
      }
      buf.append(name2);
    }   
    return buf.toString();
  }
  
  public static IJavaElement[] codeResolve(ITextEditor editor) throws JavaModelException {
    return codeResolve(editor, true);
  }
    
  /**
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   * @since 3.2
   */
  public static IJavaElement[] codeResolve(ITextEditor editor, boolean primaryOnly) throws JavaModelException {
    return codeResolve(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
  }

  public static IJavaElement getInput(IEditorPart editor) {
    return getInput(editor, true);
  }
  
  /**
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   * @since 3.2
   */
  private static IJavaElement getInput(IEditorPart editor, boolean primaryOnly) {
    if (editor == null)
      return null;
    return getEditorInputJavaElement(editor, primaryOnly);
  }
  
  public static ICompilationUnit getInputAsCompilationUnit(ITextEditor editor) {
    Object editorInput= getInput(editor);
    if (editorInput instanceof ICompilationUnit)
      return (ICompilationUnit)editorInput;
    return null;
  }
  
  /**
   * Returns the given editor's input as Java element.
   *
   * @param editor the editor
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   * @return the given editor's input as Java element or <code>null</code> if none
   * @since 3.2
   */
  public static IJavaElement getEditorInputJavaElement(IEditorPart editor, boolean primaryOnly) {
    Assert.isNotNull(editor);
    IEditorInput editorInput= editor.getEditorInput();
    if (editorInput == null)
      return null;
    
    IJavaElement je= getEditorInputJavaElement(editorInput);
    if (je != null || primaryOnly)
      return je;

    return  JavaUI.getWorkingCopyManager().getWorkingCopy(editorInput);
  }

  /**
   * Returns the Java element wrapped by the given editor input.
   *
   * @param editorInput the editor input
   * @return the Java element wrapped by <code>editorInput</code> or <code>null</code> if none
   * @since 3.2
   */
  public static IJavaElement getEditorInputJavaElement(IEditorInput editorInput) {
    // Performance: check working copy manager first: this is faster
    IJavaElement je= JavaUI.getWorkingCopyManager().getWorkingCopy(editorInput);
    if (je != null)
      return je;
    
    return (IJavaElement)editorInput.getAdapter(IJavaElement.class);
  }
  
  public static IJavaElement[] codeResolve(IJavaElement input, ITextSelection selection) throws JavaModelException {
    if (input instanceof ICodeAssist) {
      if (input instanceof ICompilationUnit) {
        reconcile((ICompilationUnit) input);
      }
      IJavaElement[] elements= ((ICodeAssist)input).codeSelect(selection.getOffset(), selection.getLength());
      if (elements != null && elements.length > 0)
        return elements;
    }
    return EMPTY_RESULT;
  }
  
  public static IJavaElement getElementAtOffset(ITextEditor editor) throws JavaModelException {
    return getElementAtOffset(editor, true);
  }
  
  /**
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   * @since 3.2
   */
  private static IJavaElement getElementAtOffset(ITextEditor editor, boolean primaryOnly) throws JavaModelException {
    return getElementAtOffset(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
  }
  
  public static IJavaElement getElementAtOffset(IJavaElement input, ITextSelection selection) throws JavaModelException {
    if (input instanceof ICompilationUnit) {
      ICompilationUnit cunit= (ICompilationUnit) input;
      reconcile(cunit);
      IJavaElement ref= cunit.getElementAt(selection.getOffset());
      if (ref == null)
        return input;
      else
        return ref;
    } else if (input instanceof IClassFile) {
      IJavaElement ref= ((IClassFile)input).getElementAt(selection.getOffset());
      if (ref == null)
        return input;
      else
        return ref;
    }
    return null;
  }
}
