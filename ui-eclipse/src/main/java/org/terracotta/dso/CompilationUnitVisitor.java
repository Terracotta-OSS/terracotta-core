/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.ui.texteditor.MarkerUtilities;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An Abstract Syntax Tree (AST) visitor that is used for inspecting modules
 * for the purpose of defining Terracotta-specific annotations as for creating
 * type summaries for use in various chooser dialogs.
 * 
 * @see org.eclipse.jdt.core.dom.ASTVisitor
 * @see org.eclipse.jdt.core.ICompilationUnit
 * @see org.terracotta.dso.ConfigurationHelper
 */

public class CompilationUnitVisitor extends ASTVisitor {
  private IProject            m_project;
  private IResource           m_res;
  private CompilationUnit     m_ast;
  private ConfigurationHelper m_configHelper;
  private InspectionAction    m_inspector;

  private static final TcPlugin m_plugin = TcPlugin.getDefault();
  
  public CompilationUnitVisitor() {
    super();
    m_inspector = new InspectionAction();    
  }
  
  protected void setup(ICompilationUnit cu)
    throws JavaModelException,
           CoreException 
  {
    m_res          = cu.getBuffer().getUnderlyingResource();
    m_project      = cu.getJavaProject().getProject();
    m_configHelper = m_plugin.getConfigurationHelper(m_project);
    m_inspector    = new InspectionAction();
  }
  
  public void inspect(final ICompilationUnit cu) {
    try {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();

      if(!workspace.isTreeLocked()) {
        IResourceRuleFactory ruleFactory = workspace.getRuleFactory();      
        ISchedulingRule      rule        = ruleFactory.markerRule(cu.getResource());

        m_inspector.setTarget(cu);
        ResourcesPlugin.getWorkspace().run(m_inspector, rule, IWorkspace.AVOID_UPDATE, null);
      }
    } catch(CoreException ce) {
      m_plugin.openError("Problem inspecting '"+cu.getElementName()+"'", ce);
    }
  }
  
  class InspectionAction implements IWorkspaceRunnable {
    private ICompilationUnit m_target;
    
    void setTarget(ICompilationUnit target) {
      m_target = target;
    }
    
    public void run(IProgressMonitor monitor) throws CoreException {
      setup(m_target);
        
      clearTerracottaMarkers();

      ASTParser parser = ASTParser.newParser(AST.JLS3);
      parser.setSource(m_target);
      parser.setResolveBindings(true);
        
      m_ast = (CompilationUnit)parser.createAST(monitor);
      m_ast.accept(CompilationUnitVisitor.this);
      
      m_configHelper.validateAll();
    }
  }
  
  public boolean visit(CompilationUnit node) {
    return true;
  }

  private static IType typeFromTypeBinding(ITypeBinding typeBinding) {
    if(typeBinding != null) {
      Object o = typeBinding.getJavaElement();
      if(o instanceof IType) {
        return (IType)o;
      }
    }
    return null;
  }
  
  public boolean visit(TypeDeclaration node) {
    ITypeBinding binding = node.resolveBinding();
    if(binding != null) {
      IType type = typeFromTypeBinding(binding);
      
      if(type != null) {
        ClassInfo classInfo = PatternHelper.getHelper().getClassInfo(type);
        if(classInfo instanceof JavaModelClassInfo) {
          JavaModelClassInfo jmci = (JavaModelClassInfo)classInfo;
          jmci.clearAnnotations();
          for(Object o : node.modifiers()) {
            if (o instanceof Annotation) {
              jmci.addAnnotation((Annotation)o);
            }
          }
        }

        String fullName = PatternHelper.getFullyQualifiedName(type);
        if(m_configHelper.isAdaptable(fullName)) {
          addMarker("adaptedTypeMarker", "DSO Adapted Type", node.getName());
        } else if(m_configHelper.isExcluded(fullName)) {
          addMarker("excludedTypeMarker", "DSO Excluded Type", node.getName());
        }
      }
      
      List superInterfaces = node.superInterfaceTypes();
      if (superInterfaces != null) {
        for (Iterator iter = superInterfaces.iterator(); iter.hasNext();) {
          Type t = (Type) iter.next();
          binding = t.resolveBinding();

          if (binding != null && !binding.isPrimitive() && !binding.isTypeVariable()) {
            type = typeFromTypeBinding(binding);

            String fullName = PatternHelper.getFullyQualifiedName(type);
            if (m_plugin.isBootClass(m_project, fullName) || m_configHelper.isBootJarClass(fullName)) {
              addMarker("bootJarTypeMarker", "BootJar Type", t);
            } else if (m_configHelper.isAdaptable(fullName)) {
              addMarker("adaptedTypeReferenceMarker", "DSO Instrumented Type Reference", t);
            } else if (m_configHelper.isExcluded(fullName)) {
              addMarker("excludedTypeMarker", "DSO Excluded Type", t);
            }
          }
        }
      }
      
      Type superType = node.getSuperclassType();
      if(superType != null) {
        binding  = superType.resolveBinding();
        
        if(binding != null && !binding.isPrimitive() && !binding.isTypeVariable()) {
          type = typeFromTypeBinding(binding);
          
          if(type != null) {
            String fullName = PatternHelper.getFullyQualifiedName(type);
        
            if(m_plugin.isBootClass(m_project, fullName) || m_configHelper.isBootJarClass(fullName)) {
              addMarker("bootJarTypeMarker", "BootJar Type", superType);
            } else if(m_configHelper.isAdaptable(fullName)) {
              addMarker("adaptedTypeReferenceMarker", "DSO Instrumented Type Reference", superType);
            } else if(m_configHelper.isExcluded(fullName)) {
              addMarker("excludedTypeMarker", "DSO Excluded Type", superType);
            }
          }
        }
      }
    }
    
    return true;
  }

  public boolean visit(FieldDeclaration node) {
    Type         typeNode = node.getType();
    ITypeBinding binding  = typeNode.resolveBinding();
    
    if (binding != null && (binding.isPrimitive() || binding.isClass() || binding.isInterface())) {
      IType type = typeFromTypeBinding(binding);
      
      List fragments = node.fragments();
      Iterator<VariableDeclarationFragment> fragIter = fragments.iterator();
      while (fragIter.hasNext()) {
        VariableDeclarationFragment vdf = fragIter.next();
        IField field = (IField) vdf.resolveBinding().getJavaElement();
        FieldInfo fieldInfo = PatternHelper.getHelper().getFieldInfo(field);
        if (fieldInfo instanceof JavaModelFieldInfo) {
          JavaModelFieldInfo jmfi = (JavaModelFieldInfo) fieldInfo;
          jmfi.clearAnnotations();
          for (Object o : node.modifiers()) {
            if (o instanceof Annotation) {
              jmfi.addAnnotation((Annotation) o);
            }
          }
        }
      }
      
      if(type != null) {
        String fullName = PatternHelper.getFullyQualifiedName(type);
  
        if(m_plugin.isBootClass(m_project, fullName) || m_configHelper.isBootJarClass(fullName)) {
          addMarker("bootJarTypeMarker", "BootJar Type", typeNode);
        } else if(m_configHelper.isAdaptable(fullName)) {
          addMarker("adaptedTypeReferenceMarker", "DSO Instrumented Type Reference", typeNode);
        } else if(m_configHelper.isExcluded(fullName)) {
          addMarker("excludedTypeMarker", "DSO Excluded Type", typeNode);
        }
      }
    }
    
    return true;
  }

  public boolean visit(MethodDeclaration node) {
    IMethodBinding methodBinding = node.resolveBinding();
    if(methodBinding != null) {
      IMethod method = (IMethod)methodBinding.getJavaElement();
      MethodInfo methodInfo = PatternHelper.getHelper().getMethodInfo(method);
      if(methodInfo instanceof JavaModelMethodInfo) {
        JavaModelMethodInfo jmmi = (JavaModelMethodInfo)methodInfo;
        jmmi.clearAnnotations();
        for(Object o : node.modifiers()) {
          if (o instanceof Annotation) {
            jmmi.addAnnotation((Annotation)o);
          }
        }
      }
    }
    
    if(isDeclaringTypeAdaptable(node)) {
      if(m_configHelper.isAutolocked(node) && hasSynchronization(node)) {
        addMarker("autolockedMarker", "DSO Auto-locked Method", node.getName());
      } else if(m_configHelper.isNameLocked(node)) {
        addMarker("nameLockedMarker", "DSO Name-locked Method", node.getName());
      }
    }

    if(m_configHelper.isDistributedMethod(node)) {
      addMarker("distributedMethodMarker", "DSO Distributed Method", node.getName());

      if(!isDeclaringTypeAdaptable(node)) {
        addProblemMarker("DeclaringTypeNotInstrumentedMarker", "Declaring type not instrumented", node.getName());
      }
    }
    
    return true;
  }
  
  public boolean visit(MethodInvocation node) {
    IMethodBinding binding = node.resolveMethodBinding();

    if(binding != null && binding.isConstructor()) {
      ITypeBinding declaringType = binding.getDeclaringClass();      
      String       fullName      = declaringType.getQualifiedName();

      if(m_plugin.isBootClass(m_project, fullName) || m_configHelper.isBootJarClass(fullName)) {
        addMarker("bootJarTypeMarker", "BootJar Type", node.getName());
      } else if(m_configHelper.isAdaptable(fullName)) {
        addMarker("adaptedTypeReferenceMarker", "DSO Instrumented Type Reference", node.getName());
      }
    }

    return true;
  }

  public boolean visit(ClassInstanceCreation node) {
    Type         type    = node.getType(); 
    ITypeBinding binding = type.resolveBinding();
    
    if(binding != null) {
      String fullName = binding.getQualifiedName();
  
      if(m_plugin.isBootClass(m_project, fullName) || m_configHelper.isBootJarClass(fullName)) {
        addMarker("bootJarTypeMarker", "BootJar Type", type);
      } else if(m_configHelper.isAdaptable(fullName)) {
        addMarker("adaptedTypeReferenceMarker", "DSO Instrumented Type Reference", type);
      }
    }
    
    return true;
  }

  public boolean visit(Block node) {return true;}
  public boolean visit(SynchronizedStatement node) {return true;}
  public boolean visit(WhileStatement node) {return true;}
  public boolean visit(TryStatement node) {return true;}
  public boolean visit(ForStatement node) {return true;}
  public boolean visit(IfStatement node) {return true;}
  public boolean visit(SwitchStatement node) {return true;}
  public boolean visit(LabeledStatement node) {return true;}
  public boolean visit(InstanceofExpression node) {return true;}
  public boolean visit(Initializer node) {return true;}

  public boolean visit(SingleVariableDeclaration node) {
    IVariableBinding binding = node.resolveBinding();
    
    if(binding != null) {
      String fullName = binding.getType().getQualifiedName();
      if(m_plugin.isBootClass(m_project, fullName) || m_configHelper.isBootJarClass(fullName)) {
        addMarker("bootJarTypeMarker", "BootJar Type", node.getType());
      }
    }
    
    return true;
  }
  
  private static String getQualifiedName(ITypeBinding binding) {
    String qname = binding.getQualifiedName();
    
    if(binding.isMember()) {
      int lastDot = qname.lastIndexOf('.');
      qname = qname.substring(0, lastDot) + '$' + qname.substring(lastDot+1);
    }
    
    return qname;
  }
  
  public boolean visit(VariableDeclarationFragment node) {
    IVariableBinding binding = node.resolveBinding();
    
    if(binding != null) {
      if(binding.isField()) {
        String parentClass = getQualifiedName(binding.getDeclaringClass());
        String fieldName = parentClass+"."+binding.getName();
        IField field = (IField)binding.getJavaElement();

        FieldInfo fieldInfo = PatternHelper.getHelper().getFieldInfo(field);
        if (fieldInfo instanceof JavaModelFieldInfo) {
          JavaModelFieldInfo jmfi = (JavaModelFieldInfo) fieldInfo;
          ASTNode parent = node.getParent();
          if(parent instanceof BodyDeclaration) {
            BodyDeclaration body = (BodyDeclaration)parent;
            jmfi.clearAnnotations();
            for (Object o : body.modifiers()) {
              if (o instanceof Annotation) {
                jmfi.addAnnotation((Annotation) o);
              }
            }
          }
        }
        
        if(m_configHelper.isRoot(field)) {
          addMarker("rootMarker", "DSO Root Field", node.getName());
        } else if(m_configHelper.isTransient(fieldName)) {
          addMarker("transientFieldMarker", "DSO Transient Field", node.getName());
          
          if(!m_configHelper.isAdaptable(parentClass) && !m_configHelper.declaresRoot(parentClass)) {
            addProblemMarker("DeclaringTypeNotInstrumentedMarker", "Declaring type not instrumented", node);
          }
        }
      }

      String typeName = binding.getType().getQualifiedName();
      
      if(m_plugin.isBootClass(m_project, typeName) || m_configHelper.isBootJarClass(typeName)) {
        addMarker("bootJarTypeMarker", "BootJar Type", getType(node));
      } else if(m_configHelper.isAdaptable(typeName)) {
        addMarker("adaptedTypeReferenceMarker", "DSO Instrumented Type Reference", getType(node));
      }
    }
    
    return true;
  }
  
  private Type getType(VariableDeclarationFragment node) {
    ASTNode parentNode = node.getParent();
    
    if(parentNode instanceof FieldDeclaration) {
      return ((FieldDeclaration)node.getParent()).getType();
    } else if(parentNode instanceof VariableDeclarationStatement) {
      return ((VariableDeclarationStatement)node.getParent()).getType();
    } else if(parentNode instanceof VariableDeclarationExpression) {
      return ((VariableDeclarationExpression)node.getParent()).getType();
    }
    
    return null;
  }

  private static boolean hasSynchronization(MethodDeclaration node) {
    if(Modifier.isSynchronized(node.getModifiers())) {
      return true;
    }
    
    Block body = node.getBody();
    if(body != null) {
      return hasSynchronization(body);
    }
    
    return false;
  }

  private static boolean hasSynchronization(Block block) {
    List statements = block.statements();
    
    if(statements != null) {
      Iterator iter = statements.iterator();
      
      while(iter.hasNext()) {
        if(hasSynchronization((Statement)iter.next())) {
          return true;
        }
      }
    }

    return false;
  }
  
  private static boolean hasSynchronization(Statement statement) {
    if(statement instanceof SynchronizedStatement) {
      return true;
    } else if(statement instanceof Block) {
      return hasSynchronization((Block)statement);
    } else if(statement instanceof ForStatement) {
      return hasSynchronization(((ForStatement)statement).getBody());
    } else if(statement instanceof EnhancedForStatement) {
      return hasSynchronization(((EnhancedForStatement)statement).getBody());
    } else if(statement instanceof IfStatement) {
      return hasSynchronization((IfStatement)statement);
    } else if(statement instanceof WhileStatement) {
      return hasSynchronization((WhileStatement)statement);
    } else if(statement instanceof DoStatement) {
      return hasSynchronization((DoStatement)statement);
    } else if(statement instanceof TryStatement) {
      return hasSynchronization((TryStatement)statement);
    } else if(statement instanceof SwitchStatement) {
      return hasSynchronization((SwitchStatement)statement);
    }

    return false;
  }
  
  private static boolean hasSynchronization(IfStatement statement) {
    Statement thenStatement = statement.getThenStatement();
    Statement elseStatement = statement.getElseStatement();

    return hasSynchronization(thenStatement) ||
      ((elseStatement != null) && hasSynchronization(elseStatement));
  }

  private static boolean hasSynchronization(WhileStatement statement) {
    return hasSynchronization(statement.getBody());
  }

  private static boolean hasSynchronization(DoStatement statement) {
    return hasSynchronization(statement.getBody());
  }

  // TODO: handle catchClauses and finallyBlock
  private static boolean hasSynchronization(TryStatement statement) {
    return hasSynchronization(statement.getBody());
  }

  private static boolean hasSynchronization(SwitchStatement statement) {
    List statements = statement.statements();
    
    if(statements != null) {
      Iterator iter = statements.iterator();
      
      while(iter.hasNext()) {
        if(hasSynchronization((Statement)iter.next())) {
          return true;
        }
      }
    }

    return false;
  }

  public boolean isDeclaringTypeAdaptable(MethodDeclaration node) {
    IMethodBinding binding = node.resolveBinding();
    
    if(binding != null) {
      IMethod method = (IMethod)binding.getJavaElement();
      
      if(method != null) {
        IType type = method.getDeclaringType();
        return m_configHelper.isAdaptable(type) ||
               m_configHelper.declaresRoot(type);
      }
    }
    
    return false;
  }

  private void addMarker(String markerID, String msg, ASTNode node) {
    if(node == null) return;
    
    HashMap map      = new HashMap();
    int     startPos = node.getStartPosition();
    int     endPos   = startPos + node.getLength();
    int     line     = m_ast.lineNumber(startPos);
      
    MarkerUtilities.setMessage(map, msg);
    MarkerUtilities.setCharStart(map, startPos);
    MarkerUtilities.setCharEnd(map, endPos);
    MarkerUtilities.setLineNumber(map, line);
    map.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_INFO));

    createMarker(m_res, map, markerID);
  }
  
  private void addProblemMarker(String markerID, String msg, ASTNode node) {
    HashMap<String, Integer> map      = new HashMap<String, Integer>();
    int                      startPos = node.getStartPosition();
    int                      endPos   = startPos + node.getLength();
    int                      line     = m_ast.lineNumber(startPos);
      
    MarkerUtilities.setMessage(map, msg);
    MarkerUtilities.setCharStart(map, startPos);
    MarkerUtilities.setCharEnd(map, endPos);
    MarkerUtilities.setLineNumber(map, line);
    map.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_WARNING));
    
    createMarker(m_res, map, markerID);
  }

  private void createMarker(IResource resource, Map attributes, String markerType) {
    try {
      IMarker marker = resource.createMarker("org.terracotta.dso."+markerType);
      marker.setAttributes(attributes);
    } catch(CoreException ce) {
      m_plugin.openError("Creating marker", ce);
    }
  }
  
  protected void clearTerracottaMarkers() {
    if (m_res.exists() && m_res.getProject().isOpen()) {
      try {
        m_res.deleteMarkers("org.terracotta.dso.baseMarker", true, IResource.DEPTH_ZERO);
      } catch (Exception e) {/**/
      }

      try {
        m_res.deleteMarkers("org.terracotta.dso.ConfigProblemMarker", true, IResource.DEPTH_ZERO);
      } catch (Exception e) {/**/
      }
    }
  }

  public boolean visit(AnnotationTypeDeclaration node) {return false;}
  public boolean visit(AnnotationTypeMemberDeclaration node) {return false;}
  public boolean visit(AnonymousClassDeclaration node) {return false;}
  public boolean visit(ArrayAccess node) {return false;}
  public boolean visit(ArrayCreation node) {return false;}
  public boolean visit(ArrayInitializer node) {return false;}
  public boolean visit(ArrayType node) {return false;}
  public boolean visit(AssertStatement node) {return false;}
  public boolean visit(Assignment node) {return true;}
  public boolean visit(BlockComment node) {return false;}
  public boolean visit(BooleanLiteral node) {return false;}
  public boolean visit(BreakStatement node) {return false;}
  public boolean visit(CastExpression node) {return true;}
  public boolean visit(CatchClause node) {return false;}
  public boolean visit(CharacterLiteral node) {return false;}
  public boolean visit(ConditionalExpression node) {return false;}
  public boolean visit(ConstructorInvocation node) {return true;}
  public boolean visit(ContinueStatement node) {return false;}
  public boolean visit(DoStatement node) {return true;}
  public boolean visit(EmptyStatement node) {return false;}
  public boolean visit(EnhancedForStatement node) {return true;}
  public boolean visit(EnumConstantDeclaration node) {return false;}
  public boolean visit(EnumDeclaration node) {return false;}
  public boolean visit(ExpressionStatement node) {return true;}
  public boolean visit(FieldAccess node) {return false;}
  public boolean visit(ImportDeclaration node) {return false;}
  public boolean visit(InfixExpression node) {return false;}
  public boolean visit(Javadoc node) {return false;}
  public boolean visit(LineComment node) {return false;}
  public boolean visit(MarkerAnnotation node) {return false;}
  public boolean visit(MemberRef node) {return false;}
  public boolean visit(MemberValuePair node) {return false;}
  public boolean visit(MethodRef node) {return false;}
  public boolean visit(MethodRefParameter node) {return false;}
  public boolean visit(Modifier node) {return false;}
  public boolean visit(NormalAnnotation node) {return false;}
  public boolean visit(NullLiteral node) {return false;}
  public boolean visit(NumberLiteral node) {return false;}
  public boolean visit(PackageDeclaration node) {return false;}
  public boolean visit(ParameterizedType node) {return false;}
  public boolean visit(ParenthesizedExpression node) {return false;}
  public boolean visit(PostfixExpression node) {return false;}
  public boolean visit(PrefixExpression node) {return false;}
  public boolean visit(PrimitiveType node) {return false;}
  public boolean visit(QualifiedName node) {return false;}
  public boolean visit(QualifiedType node) {return false;}
  public boolean visit(ReturnStatement node) {return false;}
  public boolean visit(SimpleName node) {return false;}
  public boolean visit(SimpleType node) {return false;}
  public boolean visit(SingleMemberAnnotation node) {return false;}
  public boolean visit(StringLiteral node) {return false;}
  public boolean visit(SuperConstructorInvocation node) {return false;}
  public boolean visit(SuperFieldAccess node) {return false;}
  public boolean visit(SuperMethodInvocation node) {return false;}
  public boolean visit(SwitchCase node) {return false;}
  public boolean visit(TagElement node) {return false;}
  public boolean visit(TextElement node) {return false;}
  public boolean visit(ThisExpression node) {return false;}
  public boolean visit(ThrowStatement node) {return false;}
  public boolean visit(TypeLiteral node) {return false;}
  public boolean visit(WildcardType node) {return false;}
}
