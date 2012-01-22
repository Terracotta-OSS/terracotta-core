/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.expression;


import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ReflectionInfo;
import com.tc.aspectwerkz.reflect.StaticInitializationInfo;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.aspectwerkz.expression.ExpressionInfo;

import com.tc.aspectwerkz.expression.ast.*;
import com.tc.aspectwerkz.expression.regexp.Pattern;
import com.tc.aspectwerkz.expression.regexp.TypePattern;
import com.tc.aspectwerkz.util.Util;

/**
 * The expression visitor.
 * If a runtime residual is required (target => instance of check sometimes), Undeterministic matching is used.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur </a>
 * @author Michael Nascimento
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public class ExpressionVisitor implements ExpressionParserVisitor {

  protected Node m_root;
  protected String m_expression;
  protected String m_namespace;

  /**
   * The expressionInfo this visitor is built on
   */
  protected ExpressionInfo m_expressionInfo;

  /**
   * Creates a new expression.
   *
   * @param expressionInfo the expressionInfo this visitor is built on for expression with signature
   * @param expression     the expression as a string
   * @param namespace      the namespace
   * @param root           the AST root
   */
  public ExpressionVisitor(final ExpressionInfo expressionInfo,
                           final String expression,
                           final String namespace,
                           final Node root) {
    m_expressionInfo = expressionInfo;
    m_expression = expression;
    m_namespace = namespace;
    m_root = root;
  }

  /**
   * Matches the expression context.
   * If undetermined, assume true.
   * Do not use for poincut reference - see matchUndeterministic
   *
   * @param context
   * @return
   */
  public boolean match(final com.tc.aspectwerkz.expression.ExpressionContext context) {
    Boolean match = ((Boolean) visit(m_root, context));
    // undeterministic is assumed to be "true" at this stage
    // since it won't be composed anymore with a NOT (unless
    // thru pointcut reference ie a new visitor)
    return (match != null) ? match.booleanValue() : true;
  }

  protected Boolean matchUndeterministic(final ExpressionContext context) {
    Boolean match = ((Boolean) visit(m_root, context));
    return match;
  }

  // ============ Boot strap =============
  public Object visit(Node node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  public Object visit(SimpleNode node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  public Object visit(ASTRoot node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  public Object visit(ASTExpression node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  // ============ Logical operators =============
  public Object visit(ASTOr node, Object data) {
    // the AND and OR can have more than 2 nodes [see jjt grammar]
    Boolean matchL = (Boolean) node.jjtGetChild(0).jjtAccept(this, data);
    Boolean matchR = (Boolean) node.jjtGetChild(1).jjtAccept(this, data);
    Boolean intermediate = Undeterministic.or(matchL, matchR);
    for (int i = 2; i < node.jjtGetNumChildren(); i++) {
      Boolean matchNext = (Boolean) node.jjtGetChild(i).jjtAccept(this, data);
      intermediate = Undeterministic.or(intermediate, matchNext);
    }
    return intermediate;
  }

  public Object visit(ASTAnd node, Object data) {
    // the AND and OR can have more than 2 nodes [see jjt grammar]
    Boolean matchL = (Boolean) node.jjtGetChild(0).jjtAccept(this, data);
    Boolean matchR = (Boolean) node.jjtGetChild(1).jjtAccept(this, data);
    Boolean intermediate = Undeterministic.and(matchL, matchR);
    for (int i = 2; i < node.jjtGetNumChildren(); i++) {
      Boolean matchNext = (Boolean) node.jjtGetChild(i).jjtAccept(this, data);
      intermediate = Undeterministic.and(intermediate, matchNext);
    }
    return intermediate;
  }

  public Object visit(ASTNot node, Object data) {
    Boolean match = (Boolean) node.jjtGetChild(0).jjtAccept(this, data);
    return Undeterministic.not(match);
  }

  // ============ Pointcut types =============
  public Object visit(ASTPointcutReference node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    ExpressionNamespace namespace = ExpressionNamespace.getNamespace(m_namespace);
    ExpressionVisitor expression = namespace.getExpression(node.getName());
    return expression.matchUndeterministic(context);
  }

  public Object visit(ASTExecution node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    if (context.hasExecutionPointcut() && (context.hasMethodInfo() || context.hasConstructorInfo())) {
      return visitAnnotatedNode(node, context.getReflectionInfo());
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTCall node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    if (context.hasCallPointcut() && (context.hasMethodInfo() || context.hasConstructorInfo())) {
      return visitAnnotatedNode(node, context.getReflectionInfo());
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTSet node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    if (context.hasSetPointcut() && context.hasFieldInfo()) {
      return visitAnnotatedNode(node, context.getReflectionInfo());
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTGet node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    if (context.hasGetPointcut() && context.hasFieldInfo()) {
      return visitAnnotatedNode(node, context.getReflectionInfo());
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTHandler node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    if (context.hasHandlerPointcut() && context.hasClassInfo()) {
      return node.jjtGetChild(0).jjtAccept(this, context.getReflectionInfo());
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTStaticInitialization node, Object data) {
    ExpressionContext context = (ExpressionContext) data;

    if (context.hasStaticInitializationPointcut() && context.hasReflectionInfo()) {
      ReflectionInfo reflectInfo = context.getReflectionInfo();

      if (reflectInfo instanceof StaticInitializationInfo) {
        ClassInfo declaringClassInfo = ((StaticInitializationInfo) reflectInfo).getDeclaringType();

        // In an annotated subtree, only the last child node may represent the pattern
        Node patternNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
        if (!(patternNode instanceof ASTAttribute)) {
          Boolean matchPattern = (Boolean) patternNode.jjtAccept(this, reflectInfo);
          if (Boolean.FALSE.equals(matchPattern)) {
            return Boolean.FALSE;
          }
        }

        // match on annotation if no pattern node or matched already
        boolean matchedAnnotations = visitAttributes(node, declaringClassInfo);
        if (!matchedAnnotations) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else {
        return Boolean.FALSE;
      }
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTIf node, Object data) {
    // TODO implent some day
    return Boolean.TRUE;
  }

  public Object visit(ASTWithin node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    if (context.hasWithinReflectionInfo()) {
      ReflectionInfo reflectInfo = context.getWithinReflectionInfo();
      ReflectionInfo withinInfo = null;

      if (reflectInfo instanceof MemberInfo) {
        withinInfo = ((MemberInfo) reflectInfo).getDeclaringType();
      } else if (reflectInfo instanceof ClassInfo) {
        withinInfo = reflectInfo;
      } else {
        return Boolean.FALSE;
      }
      return visitAnnotatedNode(
              node,
              withinInfo);
    } else {
      return null;
    }
  }

  public Object visit(ASTWithinCode node, Object data) {
    ExpressionContext context = (ExpressionContext) data;

    if (!context.hasWithinReflectionInfo()) {
      return null;
    }

    ReflectionInfo reflectInfo = context.getWithinReflectionInfo();

    if (node.isStaticInitializer()) {
      if (reflectInfo instanceof StaticInitializationInfo) {
        // Ignore the ASTStaticInitialization node in this context
        SimpleNode staticClinitNode = (SimpleNode) node.jjtGetChild(0);
        ClassInfo declaringClassInfo = ((StaticInitializationInfo) reflectInfo).getDeclaringType();

        boolean matchedAnnotations = visitAttributes(staticClinitNode, declaringClassInfo);
        if (!matchedAnnotations) {
          return Boolean.FALSE;
        }

        // In an annotated subtree, the last child node represents the pattern
        Node lastNode = staticClinitNode.jjtGetChild(staticClinitNode.jjtGetNumChildren() - 1);
        if (lastNode instanceof ASTAttribute) {
          return Boolean.TRUE;
        } else {
          return lastNode.jjtAccept(this, reflectInfo);
        }
      } else {
        return Boolean.FALSE;
      }
    } else {
      return visitAnnotatedNode(
              node,
              reflectInfo
      );
    }
  }


  public Object visit(ASTHasMethod node, Object data) {
    ExpressionContext context = (ExpressionContext) data;

    // we are matching on the CALLER info
    // for execution() pointcut, this is equals to CALLEE info
    ReflectionInfo info = context.getWithinReflectionInfo();
    ClassInfo classInfo = info instanceof MemberInfo
            ? ((MemberInfo) info).getDeclaringType()
            : (ClassInfo) info;

    Node patternNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
    boolean hasPatternNode = !(patternNode instanceof ASTAttribute);

    MethodInfo[] methodInfos = classInfo.getMethods();
    for (int i = 0; i < methodInfos.length; i++) {
      if (hasPatternNode) {
        if (Boolean.FALSE.equals(patternNode.jjtAccept(this, methodInfos[i]))) {
          continue;
        }
      }

      boolean matchAnnotations = visitAttributes(node, methodInfos[i]);
      if (matchAnnotations) {
        return Boolean.TRUE;
      }
    }

    ConstructorInfo[] constructorInfos = classInfo.getConstructors();
    for (int i = 0; i < constructorInfos.length; i++) {
      if (hasPatternNode) {
        if (Boolean.FALSE.equals(patternNode.jjtAccept(this, constructorInfos[i]))) {
          continue;
        }
      }

      boolean matchAnnotations = visitAttributes(node, constructorInfos[i]);
      if (matchAnnotations) {
        return Boolean.TRUE;
      }
    }

    return Boolean.FALSE;
  }

  public Object visit(ASTHasField node, Object data) {
    ExpressionContext context = (ExpressionContext) data;

    // we are matching on the CALLER info
    // for execution() pointcut, this is equals to CALLEE info
    ReflectionInfo info = context.getWithinReflectionInfo();
    ClassInfo classInfo = (info instanceof MemberInfo) ?
            ((MemberInfo) info).getDeclaringType() : (ClassInfo) info;

    Node patternNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
    boolean hasPatternNode = !(patternNode instanceof ASTAttribute);

    FieldInfo[] fieldInfos = classInfo.getFields();
    for (int i = 0; i < fieldInfos.length; i++) {
      if (hasPatternNode) {
        if (Boolean.FALSE.equals(patternNode.jjtAccept(this, fieldInfos[i]))) {
          continue;
        }
      }

      boolean matchAnnotations = visitAttributes(node, fieldInfos[i]);
      if (matchAnnotations) {
        return Boolean.TRUE;
      }
    }

    return Boolean.FALSE;
  }

  public Object visit(ASTTarget node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    ReflectionInfo info = context.getReflectionInfo();

//        //TODO - seems to be the case for AJ - not intuitive
//        if (info instanceof ConstructorInfo) {
//            // target(..) does not match for constructors
//            return Boolean.FALSE;
//        }
    ClassInfo declaringType = null;
    if (info instanceof MemberInfo) {
      // if method/field is static, target(..) is evaluated to false
      if (Modifier.isStatic(((MemberInfo) info).getModifiers())) {
        return Boolean.FALSE;
      }

      declaringType = ((MemberInfo) info).getDeclaringType();
    } else if (info instanceof ClassInfo) {
      declaringType = (ClassInfo) info;
    } else {
      return Boolean.FALSE;
    }

    String boundedTypeName = node.getBoundedType(m_expressionInfo);
    // check if the context we match is an interface call, while the bounded type of target(..) is not an
    // interface. In such a case we will need a runtime check
    if (declaringType.isInterface()) {
      // if we are a instanceof (subinterface) of the bounded type, then we don't need a runtime check
      if (ClassInfoHelper.instanceOf(declaringType, boundedTypeName)) {
        return Boolean.TRUE;
      } else {
        //System.out.println("*** RT check for "  + boundedTypeName + " when I am " + declaringType.getName());
        // a runtime check with instance of will be required
        return null;
      }
    } else {
      return Util.booleanValueOf(ClassInfoHelper.instanceOf(declaringType, boundedTypeName));
    }
  }

  public Object visit(ASTThis node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    // for execution pointcut, this(..) is used to match the callee info
    // and we are assuming here that withinInfo is properly set to reflectionInfo
    if (context.hasWithinReflectionInfo()) {
      ReflectionInfo withinInfo = context.getWithinReflectionInfo();
      if (withinInfo instanceof MemberInfo) {
        // if method is static (callee for execution or caller for call/getDefault/set), this(..) is evaluated to false
        if (Modifier.isStatic(((MemberInfo) withinInfo).getModifiers())) {
          return Boolean.FALSE;
        }
        return Util.booleanValueOf(
                ClassInfoHelper.instanceOf(
                        ((MemberInfo) withinInfo).getDeclaringType(),
                        node.getBoundedType(m_expressionInfo)
                )
        );
      } else if (withinInfo instanceof ClassInfo) {
        return Util.booleanValueOf(
                ClassInfoHelper.instanceOf((ClassInfo) withinInfo, node.getBoundedType(m_expressionInfo))
        );
      }
    }
    return Boolean.FALSE;
  }

  public Object visit(ASTCflow node, Object data) {
    return null;
  }

  public Object visit(ASTCflowBelow node, Object data) {
    return null;
  }

  // ============ Patterns =============
  public Object visit(ASTClassPattern node, Object data) {
    if (data instanceof ClassInfo) {
      ClassInfo classInfo = (ClassInfo) data;
      TypePattern typePattern = node.getTypePattern();

      if (typePattern.matchType(classInfo)
              && visitModifiers(node, classInfo)) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    } else if (data instanceof StaticInitializationInfo) {
      ClassInfo classInfo = ((StaticInitializationInfo) data).getDeclaringType();

      if (node.getTypePattern().matchType(classInfo)) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }

//          return new Boolean(node.getTypePattern().matchType(classInfo));
    }

    return Boolean.FALSE;
  }

  public Object visit(ASTMethodPattern node, Object data) {
    if (data instanceof MethodInfo) {
      MethodInfo methodInfo = (MethodInfo) data;
      if (node.getMethodNamePattern().matches(methodInfo.getName())
              && node.getDeclaringTypePattern().matchType(methodInfo.getDeclaringType())
              && node.getReturnTypePattern().matchType(methodInfo.getReturnType())
              && visitModifiers(node, methodInfo)
              && visitParameters(node, methodInfo.getParameterTypes())) {
        return Boolean.TRUE;
      }
    }

    return Boolean.FALSE;
  }

  public Object visit(ASTConstructorPattern node, Object data) {
    if (data instanceof ConstructorInfo) {
      ConstructorInfo constructorMetaData = (ConstructorInfo) data;
      if (node.getDeclaringTypePattern().matchType(constructorMetaData.getDeclaringType())
              && visitModifiers(node, constructorMetaData)
              && visitParameters(node, constructorMetaData.getParameterTypes())) {
        return Boolean.TRUE;
      }
    }
    return Boolean.FALSE;
  }

  public Object visit(ASTFieldPattern node, Object data) {
    if (data instanceof FieldInfo) {
      FieldInfo fieldInfo = (FieldInfo) data;
      if (node.getFieldNamePattern().matches(fieldInfo.getName())
              && node.getDeclaringTypePattern().matchType(fieldInfo.getDeclaringType())
              && node.getFieldTypePattern().matchType(fieldInfo.getType())
              && visitModifiers(node, fieldInfo)) {
        return Boolean.TRUE;
      }
    }
    return Boolean.FALSE;
  }

  public Object visit(ASTParameter node, Object data) {
    ClassInfo parameterType = (ClassInfo) data;
    if (node.getDeclaringClassPattern().matchType(parameterType)) {
      return Boolean.TRUE;
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTArgs node, Object data) {
    ExpressionContext ctx = (ExpressionContext) data;
    if (node.jjtGetNumChildren() <= 0) {
      // args(EMPTY)
      return (getParametersCount(ctx) == 0) ? Boolean.TRUE : Boolean.FALSE;
    } else {
      // check for ".." as first node
      int expressionParameterCount = node.jjtGetNumChildren();// the number of node minus eager one.
      boolean isFirstArgEager = ((ASTArgParameter) node.jjtGetChild(0)).getTypePattern().isEagerWildCard();
      boolean isLastArgEager = ((ASTArgParameter) node.jjtGetChild(node.jjtGetNumChildren() - 1))
              .getTypePattern().isEagerWildCard();
      // args(..)
      if (isFirstArgEager && expressionParameterCount == 1) {
        return Boolean.TRUE;
      }
      int contextParametersCount = getParametersCount(ctx);
      if (isFirstArgEager && isLastArgEager) {
        expressionParameterCount -= 2;
        if (expressionParameterCount == 0) {
          // expression is "args(.., ..)"
          return Boolean.TRUE;
        }
        // we need to find a starting position - args(..,int, bar, ..)
        // foo(int) //int is ok
        // foo(bar,int,bar) //int is ok
        // foo(bar,int,foo,int,bar) // int is ok, but then we fail, so move on to next..
        int matchCount = 0;
        int ictx = 0;
        for (int iexp = 0; iexp < expressionParameterCount; iexp++) {
          if (ictx >= contextParametersCount) {
            // too many args in args()
            matchCount = -1;
            break;
          }
          ctx.setCurrentTargetArgsIndex(ictx);
          // do we have an eager wildcard in the middle ?
          boolean isEager = ((ASTArgParameter) node.jjtGetChild(iexp + 1)).getTypePattern().isEagerWildCard();
          if (isEager) {
            // TODO - ignore for now, but not really supported - eager in the middle will match one
          }
          if (Boolean.TRUE.equals(node.jjtGetChild(iexp + 1).jjtAccept(this, ctx))) {
            matchCount += 1;
            ictx++;
          } else {
            // assume matched by starting ".." and rewind expression index
            matchCount = 0;
            ictx++;
            iexp = -1;
          }
        }
        if (matchCount == expressionParameterCount) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if (isFirstArgEager) {
        expressionParameterCount--;
        if (contextParametersCount >= expressionParameterCount) {
          // do a match from last to first, break when args() nodes are exhausted
          for (int i = 0; (i < contextParametersCount) && (expressionParameterCount - i >= 0); i++) {
            ctx.setCurrentTargetArgsIndex(contextParametersCount - 1 - i);
            if (Boolean.TRUE.equals(
                    node.jjtGetChild(expressionParameterCount - i).jjtAccept(
                            this,
                            ctx
                    )
            )) {
              //go on with "next" arg
            } else {
              return Boolean.FALSE;
            }
          }
          return Boolean.TRUE;
        } else {
          //args() as more args than context we try to match
          return Boolean.FALSE;
        }
      } else if (isLastArgEager) {
        expressionParameterCount--;
        if (contextParametersCount >= expressionParameterCount) {
          // do a match from first to last, break when args() nodes are exhausted
          for (int i = 0; (i < contextParametersCount) && (i < expressionParameterCount); i++) {
            ctx.setCurrentTargetArgsIndex(i);
            if (Boolean.TRUE.equals(node.jjtGetChild(i).jjtAccept(this, ctx))) {
              //go on with next arg
            } else {
              return Boolean.FALSE;
            }
          }
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else {
        // no eager wildcard in args()
        // check that args length are equals
        if (expressionParameterCount == contextParametersCount) {
          for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            ctx.setCurrentTargetArgsIndex(i);
            if (Boolean.TRUE.equals(node.jjtGetChild(i).jjtAccept(this, ctx))) {
              //go on with next arg
            } else {
              return Boolean.FALSE;
            }
          }
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      }
    }
  }

  public Object visit(ASTArgParameter node, Object data) {
    //TODO we are not doing any hierarchical test when the arg is bound
    // => args(e) and before(Exception e) will not mathch on catch(SubException e) ..
    // is that required ? how AJ syntax behaves ?

    TypePattern typePattern = node.getTypePattern();
    TypePattern realPattern = typePattern;

    // check if the arg is in the pointcut signature. In such a case, use the declared type
    //TODO can we improve that with a lazy attach of the realTypePattern to the node
    // and a method that always return the real pattern
    // It must be lazy since args are not added at info ctor time [can be refactored..]
    // do some filtering first to avoid unnecessary map lookup

    // int pointcutArgIndex = -1;
    if (typePattern.getPattern().indexOf(".") < 0) {
      String boundedType = m_expressionInfo.getArgumentType(typePattern.getPattern());
      if (boundedType != null) {
        // pointcutArgIndex = m_expressionInfo.getArgumentIndex(typePattern.getPattern());
        realPattern = Pattern.compileTypePattern(boundedType, SubtypePatternType.NOT_HIERARCHICAL);
      }
    }
    // grab parameter from context
    ExpressionContext ctx = (ExpressionContext) data;
    ClassInfo argInfo = null;
    try {
      if (ctx.getReflectionInfo() instanceof MethodInfo) {
        argInfo = ((MethodInfo) ctx.getReflectionInfo()).getParameterTypes()[ctx.getCurrentTargetArgsIndex()];
      } else if (ctx.getReflectionInfo() instanceof ConstructorInfo) {
        argInfo = ((ConstructorInfo) ctx.getReflectionInfo()).getParameterTypes()[ctx
                .getCurrentTargetArgsIndex()];
      } else if (ctx.getReflectionInfo() instanceof FieldInfo) {
        argInfo = ((FieldInfo) ctx.getReflectionInfo()).getType();
      } else if (ctx.getPointcutType().equals(PointcutType.HANDLER) && ctx.getReflectionInfo() instanceof ClassInfo) {
        argInfo = (ClassInfo) ctx.getReflectionInfo();
      } else {
        System.err.println("Assigning a null argInfo");
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("ExpressionContext args are exhausted.");
      return Boolean.FALSE;
    }
    if (realPattern.matchType(argInfo)) {
      return Boolean.TRUE;
    } else {
      return Boolean.FALSE;
    }
  }

  public Object visit(ASTAttribute node, Object data) {
    boolean matchAnnotation = false;
    AnnotationElement.Annotation[] annotations = (AnnotationElement.Annotation[]) data;
    for (int i = 0; i < annotations.length; i++) {
      AnnotationElement.Annotation annotation = annotations[i];
      if (annotation.getInterfaceName().equals(node.getName())) {
        matchAnnotation = true;
      }
    }

    if (node.isNot()) {
      return Util.booleanValueOf(!matchAnnotation);
    } else {
      return Util.booleanValueOf(matchAnnotation);
    }
  }

  public Object visit(ASTModifier node, Object data) {
    ReflectionInfo refInfo = (ReflectionInfo) data;
    int modifiersToMatch = refInfo.getModifiers();
    int modifierPattern = node.getModifier();
    if (node.isNot()) {
      if ((modifierPattern & Modifier.PUBLIC) != 0) {
        if (((modifiersToMatch & Modifier.PUBLIC) == 0)) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.PROTECTED) != 0) {
        if ((modifiersToMatch & Modifier.PROTECTED) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.PRIVATE) != 0) {
        if ((modifiersToMatch & Modifier.PRIVATE) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.STATIC) != 0) {
        if ((modifiersToMatch & Modifier.STATIC) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.SYNCHRONIZED) != 0) {
        if ((modifiersToMatch & Modifier.SYNCHRONIZED) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.FINAL) != 0) {
        if ((modifiersToMatch & Modifier.FINAL) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.TRANSIENT) != 0) {
        if ((modifiersToMatch & Modifier.TRANSIENT) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.VOLATILE) != 0) {
        if ((modifiersToMatch & Modifier.VOLATILE) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if ((modifierPattern & Modifier.STRICT) != 0) {
        if ((modifiersToMatch & Modifier.STRICT) == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else {
        return Boolean.FALSE;
      }
    } else {
      if ((modifierPattern & Modifier.PUBLIC) != 0) {
        if (((modifiersToMatch & Modifier.PUBLIC) == 0)) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.PROTECTED) != 0) {
        if ((modifiersToMatch & Modifier.PROTECTED) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.PRIVATE) != 0) {
        if ((modifiersToMatch & Modifier.PRIVATE) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.STATIC) != 0) {
        if ((modifiersToMatch & Modifier.STATIC) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.SYNCHRONIZED) != 0) {
        if ((modifiersToMatch & Modifier.SYNCHRONIZED) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.FINAL) != 0) {
        if ((modifiersToMatch & Modifier.FINAL) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.TRANSIENT) != 0) {
        if ((modifiersToMatch & Modifier.TRANSIENT) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.VOLATILE) != 0) {
        if ((modifiersToMatch & Modifier.VOLATILE) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else if ((modifierPattern & Modifier.STRICT) != 0) {
        if ((modifiersToMatch & Modifier.STRICT) == 0) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } else {
        return Boolean.TRUE;
      }
    }
  }

  protected boolean visitAttributes(SimpleNode node, ReflectionInfo refInfo) {
    int nrChildren = node.jjtGetNumChildren();
    if (nrChildren != 0) {
      for (int i = 0; i < nrChildren; i++) {
        Node child = node.jjtGetChild(i);
        if (child instanceof ASTAttribute) {
          if (Boolean.TRUE.equals(child.jjtAccept(this, refInfo.getAnnotations()))) {
            continue;
          } else {
            return false;
          }
        }
      }
    }
    return true;
  }

  protected boolean visitModifiers(SimpleNode node, ReflectionInfo refInfo) {
    int nrChildren = node.jjtGetNumChildren();
    if (nrChildren != 0) {
      for (int i = 0; i < nrChildren; i++) {
        Node child = node.jjtGetChild(i);
        if (child instanceof ASTModifier) {
          if (Boolean.TRUE.equals(child.jjtAccept(this, refInfo))) {
            continue;
          } else {
            return false;
          }
        }
      }
    }
    return true;
  }

  protected boolean visitParameters(SimpleNode node, ClassInfo[] parameterTypes) {
    int nrChildren = node.jjtGetNumChildren();
    if (nrChildren <= 0) {
      return (parameterTypes.length == 0);
    }

    // collect the parameter nodes
    List parameterNodes = new ArrayList();
    for (int i = 0; i < nrChildren; i++) {
      Node child = node.jjtGetChild(i);
      if (child instanceof ASTParameter) {
        parameterNodes.add(child);
      }
    }

    if (parameterNodes.size() <= 0) {
      return (parameterTypes.length == 0);
    }

    //TODO duplicate code with args() match
    //TODO refactor parameterNodes in an array for faster match

    // look for eager pattern at the beginning and end
    int expressionParameterCount = parameterNodes.size();
    boolean isFirstArgEager = ((ASTParameter) parameterNodes.get(0)).getDeclaringClassPattern().isEagerWildCard();
    boolean isLastArgEager = ((ASTParameter) parameterNodes.get(expressionParameterCount - 1)).getDeclaringClassPattern()
            .isEagerWildCard();
    // foo(..)
    if (isFirstArgEager && expressionParameterCount == 1) {
      return true;
    }
    int contextParametersCount = parameterTypes.length;
    if (isFirstArgEager && isLastArgEager) {
      expressionParameterCount -= 2;
      if (expressionParameterCount == 0) {
        // foo(.., ..)
        return true;
      }
      // we need to find a starting position - foo(..,int, bar, ..)
      // foo(int) //int is ok
      // foo(bar,int,bar) //int is ok
      // foo(bar,int,foo,int,bar) // int is ok, but then we fail, so move on to next..
      int matchCount = 0;
      int ictx = 0;
      for (int iexp = 0; iexp < expressionParameterCount; iexp++) {
        if (ictx >= contextParametersCount) {
          // too many args in foo()
          matchCount = -1;
          break;
        }
        // do we have an eager wildcard in the middle ?
        ASTParameter parameterNode = (ASTParameter) parameterNodes.get(iexp + 1);
        boolean isEager = parameterNode.getDeclaringClassPattern().isEagerWildCard();
        if (isEager) {
          // TODO - ignore for now, but not really supported - eager in the middle will match one
        }
        if (Boolean.TRUE.equals(parameterNode.jjtAccept(this, parameterTypes[ictx]))) {
          matchCount += 1;
          ictx++;
        } else {
          // assume matched by starting ".." and rewind expression index
          matchCount = 0;
          ictx++;
          iexp = -1;
        }
      }
      if (matchCount == expressionParameterCount) {
        return true;
      } else {
        return false;
      }
    } else if (isFirstArgEager) {
      expressionParameterCount--;
      if (contextParametersCount >= expressionParameterCount) {
        // do a match from last to first, break when foo() nodes are exhausted
        for (int i = 0; (i < contextParametersCount) && (expressionParameterCount - i >= 0); i++) {
          ASTParameter parameterNode = (ASTParameter) parameterNodes.get(expressionParameterCount - i);
          if (Boolean.TRUE.equals(
                  parameterNode.jjtAccept(
                          this,
                          parameterTypes[contextParametersCount - 1 - i]
                  )
          )) {
            //go on with "next" param
          } else {
            return false;
          }
        }
        return true;
      } else {
        //foo() as more param than context we try to match
        return false;
      }
    } else if (isLastArgEager) {
      expressionParameterCount--;
      if (contextParametersCount >= expressionParameterCount) {
        // do a match from first to last, break when foo() nodes are exhausted
        for (int i = 0; (i < contextParametersCount) && (i < expressionParameterCount); i++) {
          ASTParameter parameterNode = (ASTParameter) parameterNodes.get(i);
          if (Boolean.TRUE.equals(parameterNode.jjtAccept(this, parameterTypes[i]))) {
            //go on with next param
          } else {
            return false;
          }
        }
        return true;
      } else {
        return false;
      }
    } else {
      // no eager wildcard in foo()
      // check that param length are equals
      if (expressionParameterCount == contextParametersCount) {
        for (int i = 0; i < parameterNodes.size(); i++) {
          ASTParameter parameterNode = (ASTParameter) parameterNodes.get(i);
          if (Boolean.TRUE.equals(parameterNode.jjtAccept(this, parameterTypes[i]))) {
            //go on with next param
          } else {
            return false;
          }
        }
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Returns the string representation of the expression.
   *
   * @return
   */
  public String toString() {
    return m_expression;
  }

  /**
   * Returns the number of parameters to the target method/constructor else -1.
   *
   * @param ctx
   * @return
   */
  private int getParametersCount(final ExpressionContext ctx) {
    ReflectionInfo reflectionInfo = ctx.getReflectionInfo();
    if (reflectionInfo instanceof MethodInfo) {
      return ((MethodInfo) reflectionInfo).getParameterTypes().length;
    } else if (reflectionInfo instanceof ConstructorInfo) {
      return ((ConstructorInfo) reflectionInfo).getParameterTypes().length;
    } else if (reflectionInfo instanceof FieldInfo) {
      return 1;//field set support for args()
    } else if (ctx.getPointcutType().equals(PointcutType.HANDLER) && reflectionInfo instanceof ClassInfo) {
      // handler args(e) binding
      return 1;
    } else {
      return -1;
    }
  }

  /**
   * Test the context upon the expression tree, under a node that can
   * contain annotations.
   *
   * @param node        root node of the annotation expression
   * @param reflectInfo context reflection info
   * @return <CODE>Boolean.TRUE</CODE> in case the <tt>reflectInfo</tt> match
   *         the expression subtree, <CODE>Boolean.FALSE</CODE> otherwise.
   */
  protected Object visitAnnotatedNode(SimpleNode node,
                                      ReflectionInfo reflectInfo) {
    // In an annotated subtree, only the last child node may represent the pattern
    Node patternNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
    if (!(patternNode instanceof ASTAttribute)) {
      if (Boolean.FALSE.equals(patternNode.jjtAccept(this, reflectInfo))) {
        return Boolean.FALSE;
      }
    }

    boolean matchedAnnotations = visitAttributes(node, reflectInfo);
    if (!matchedAnnotations) {
      return Boolean.FALSE;
    } else {
      return Boolean.TRUE;
    }
  }

  /**
   * Access the ASTRoot we visit
   *
   * @return
   */
  public Node getASTRoot() {
    return m_root;
  }

  /**
   * Access the ExpressionInfo we are build on
   *
   * @return
   */
  public ExpressionInfo getExpressionInfo() {
    return m_expressionInfo;
  }
}
