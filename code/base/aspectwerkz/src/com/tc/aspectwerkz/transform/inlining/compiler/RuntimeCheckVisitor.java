/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.compiler;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import com.tc.aspectwerkz.aspect.AdviceInfo;
import com.tc.aspectwerkz.aspect.container.AspectFactoryManager;
import com.tc.aspectwerkz.cflow.CflowCompiler;
import com.tc.aspectwerkz.expression.ast.ASTAnd;
import com.tc.aspectwerkz.expression.ast.ASTArgs;
import com.tc.aspectwerkz.expression.ast.ASTCall;
import com.tc.aspectwerkz.expression.ast.ASTCflow;
import com.tc.aspectwerkz.expression.ast.ASTCflowBelow;
import com.tc.aspectwerkz.expression.ast.ASTExecution;
import com.tc.aspectwerkz.expression.ast.ASTGet;
import com.tc.aspectwerkz.expression.ast.ASTHandler;
import com.tc.aspectwerkz.expression.ast.ASTHasField;
import com.tc.aspectwerkz.expression.ast.ASTHasMethod;
import com.tc.aspectwerkz.expression.ast.ASTNot;
import com.tc.aspectwerkz.expression.ast.ASTOr;
import com.tc.aspectwerkz.expression.ast.ASTPointcutReference;
import com.tc.aspectwerkz.expression.ast.ASTSet;
import com.tc.aspectwerkz.expression.ast.ASTStaticInitialization;
import com.tc.aspectwerkz.expression.ast.ASTTarget;
import com.tc.aspectwerkz.expression.ast.ASTThis;
import com.tc.aspectwerkz.expression.ast.ASTWithin;
import com.tc.aspectwerkz.expression.ast.ASTWithinCode;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.Undeterministic;
import com.tc.aspectwerkz.expression.ExpressionNamespace;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.compiler.AbstractJoinPointCompiler;

/**
 * Visit an expression and push on the bytecode stack the boolean expression that corresponds to the residual
 * part for the target(CALLEE) filtering and cflow / cflowbelow runtime checks
 * <p/>
 * TODO: for now OR / AND / NOT are turned in IAND etc, ie "&" and not "&&" that is more efficient but is using labels.
 * <p/>
 * Note: we have to override here (and maintain) every visit Method that visit a node that appears in an expression
 * (f.e. set , getDefault, etc, but not ASTParameter), since we cannot rely on AND/OR/NOT nodes to push the boolean expressions.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
public class RuntimeCheckVisitor extends ExpressionVisitor implements Opcodes {
  public static final int NULL_PER_OBJECT_TYPE = -1;
  public static final int PER_THIS_TYPE = 1;
  public static final int PER_TARGET_TYPE = 2;

  private MethodVisitor cv;

  private CompilerInput m_input;

  private int m_perObjectCheckType = NULL_PER_OBJECT_TYPE;

  private String m_aspectQName;

  /**
   * Create a new visitor given a specific AdviceInfo
   *
   * @param cv            of the method block we are compiling
   * @param info          expression info
   * @param input
   * @param perObjectType
   * @param aspectQName
   */
  public RuntimeCheckVisitor(final MethodVisitor cv,
                             final ExpressionInfo info,
                             final CompilerInput input,
                             final int perObjectType,
                             final String aspectQName) {
    super(
            info,
            info.toString(),
            info.getNamespace(),
            info.getExpression().getASTRoot()
    );
    m_input = input;
    m_perObjectCheckType = perObjectType;
    m_aspectQName = aspectQName;

    this.cv = cv;
  }

  /**
   * Push the boolean typed expression on the stack.
   *
   * @param adviceInfo
   */
  public void pushCheckOnStack(AdviceInfo adviceInfo) {
    super.match(adviceInfo.getExpressionContext());

    switch (m_perObjectCheckType) {
      case PER_THIS_TYPE: {
        AbstractJoinPointCompiler.loadCaller(cv, m_input);
        cv.visitMethodInsn(
                INVOKESTATIC,
                AspectFactoryManager.getAspectFactoryClassName(
                        adviceInfo.getAspectClassName(),
                        adviceInfo.getAspectQualifiedName()
                ),
                TransformationConstants.FACTORY_HASASPECT_METHOD_NAME,
                TransformationConstants.FACTORY_HASASPECT_PEROBJECT_METHOD_SIGNATURE
        );
        cv.visitInsn(IAND);

        break;
      }

      case PER_TARGET_TYPE: {
        AbstractJoinPointCompiler.loadCallee(cv, m_input);
        cv.visitMethodInsn(
                INVOKESTATIC,
                AspectFactoryManager.getAspectFactoryClassName(
                        adviceInfo.getAspectClassName(),
                        adviceInfo.getAspectQualifiedName()
                ),
                TransformationConstants.FACTORY_HASASPECT_METHOD_NAME,
                TransformationConstants.FACTORY_HASASPECT_PEROBJECT_METHOD_SIGNATURE
        );
        cv.visitInsn(IAND);

        break;
      }
    }
  }

  /**
   * Handles OR expression
   *
   * @param node
   * @param data
   * @return
   */
  public Object visit(ASTOr node, Object data) {
    Boolean matchL = (Boolean) node.jjtGetChild(0).jjtAccept(this, data);
    Boolean matchR = (Boolean) node.jjtGetChild(1).jjtAccept(this, data);
    Boolean intermediate = Undeterministic.or(matchL, matchR);
    cv.visitInsn(IOR);
    for (int i = 2; i < node.jjtGetNumChildren(); i++) {
      Boolean matchNext = (Boolean) node.jjtGetChild(i).jjtAccept(this, data);
      intermediate = Undeterministic.or(intermediate, matchNext);
      cv.visitInsn(IOR);
    }
    return intermediate;
  }

  public Object visit(ASTAnd node, Object data) {
    Boolean matchL = (Boolean) node.jjtGetChild(0).jjtAccept(this, data);
    Boolean matchR = (Boolean) node.jjtGetChild(1).jjtAccept(this, data);
    Boolean intermediate = Undeterministic.and(matchL, matchR);
    cv.visitInsn(IAND);
    for (int i = 2; i < node.jjtGetNumChildren(); i++) {
      Boolean matchNext = (Boolean) node.jjtGetChild(i).jjtAccept(this, data);
      intermediate = Undeterministic.and(intermediate, matchNext);
      cv.visitInsn(IAND);
    }
    return intermediate;
  }

  public Object visit(ASTNot node, Object data) {
    Boolean match = (Boolean) node.jjtGetChild(0).jjtAccept(this, data);
    cv.visitInsn(INEG);
    return Undeterministic.not(match);
  }

  public Object visit(ASTTarget node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    if (match != null) {
      push(match);
    } else {
      // runtime check
      String boundedTypeDesc = AsmHelper.convertReflectDescToTypeDesc(node.getBoundedType(m_expressionInfo));
      AbstractJoinPointCompiler.loadCallee(cv, m_input);
      cv.visitTypeInsn(INSTANCEOF, boundedTypeDesc.substring(1, boundedTypeDesc.length() - 1));
    }
    return match;
  }

  public Object visit(ASTThis node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTCflow node, Object data) {
    // runtime check
    String cflowClassName = CflowCompiler.getCflowAspectClassName(node.hashCode());
    cv.visitMethodInsn(
            INVOKESTATIC,
            cflowClassName,
            TransformationConstants.IS_IN_CFLOW_METOD_NAME,
            TransformationConstants.IS_IN_CFLOW_METOD_SIGNATURE
    );
    return super.visit(node, data);
  }

  public Object visit(ASTCflowBelow node, Object data) {
    // runtime check
    //TODO: cflowbelow ID will differ from cflow one.. => not optimized
    String cflowClassName = CflowCompiler.getCflowAspectClassName(node.hashCode());
    cv.visitMethodInsn(
            INVOKESTATIC,
            cflowClassName,
            TransformationConstants.IS_IN_CFLOW_METOD_NAME,
            TransformationConstants.IS_IN_CFLOW_METOD_SIGNATURE
    );
    return super.visit(node, data);
  }

  public Object visit(ASTArgs node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTPointcutReference node, Object data) {
    ExpressionContext context = (ExpressionContext) data;
    ExpressionNamespace namespace = ExpressionNamespace.getNamespace(m_namespace);
    ExpressionVisitor expression = namespace.getExpression(node.getName());

    // build a new RuntimeCheckVisitor to visit the sub expression
    RuntimeCheckVisitor referenced = new RuntimeCheckVisitor(
            cv,
            expression.getExpressionInfo(),
            m_input,
            m_perObjectCheckType,
            m_aspectQName
    );
    return referenced.matchUndeterministic(context);
  }

  public Object visit(ASTExecution node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTCall node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTSet node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTGet node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTHandler node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTStaticInitialization node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTWithin node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTWithinCode node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTHasMethod node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }

  public Object visit(ASTHasField node, Object data) {
    Boolean match = (Boolean) super.visit(node, data);
    push(match);
    return match;
  }


  private void push(Boolean b) {
    if (b == null) {
      throw new Error("attempt to push an undetermined match result");
    } else if (b.booleanValue()) {
      cv.visitInsn(ICONST_1);
    } else {
      cv.visitInsn(ICONST_M1);
    }
  }
}
