/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.expression;

import java.util.List;

import com.tc.aspectwerkz.expression.ast.*;
import com.tc.aspectwerkz.expression.regexp.TypePattern;

/**
 * The visitor that extract all possible arguments referenced by the expression.
 * <p/>
 * TODO handle pointcut reference and handle parameter transition
 * + checks as done in the ArgIndexVisitor for this / target compliance.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur </a>
 */
public class ExpressionValidateVisitor implements ExpressionParserVisitor {

  protected Node m_root;
  protected String m_expression;
  protected String m_namespace;

  /**
   * Creates a new expression.
   *
   * @param expression the expression as a string
   * @param namespace  the namespace
   * @param root       the AST root
   */
  public ExpressionValidateVisitor(final String expression,
                                   final String namespace,
                                   final Node root) {
    m_expression = expression;
    m_namespace = namespace;
    m_root = root;
  }

  /**
   * Populate data with the possible arguments
   *
   * @param data a list to feed with Strings
   */
  public void populate(List data) {
    visit(m_root, data);
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
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      List args = (List) node.jjtGetChild(i).jjtAccept(this, data);
      //((List) data).addAll(args);
    }
    return data;
  }

  public Object visit(ASTAnd node, Object data) {
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return data;
  }

  public Object visit(ASTNot node, Object data) {
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return data;
  }

  // ============ Pointcut types =============
  public Object visit(ASTPointcutReference node, Object data) {
    // visit the args - if any
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return data;
  }

  public Object visit(ASTExecution node, Object data) {
    return data;
  }

  public Object visit(ASTCall node, Object data) {
    return data;
  }

  public Object visit(ASTSet node, Object data) {
    return data;
  }

  public Object visit(ASTGet node, Object data) {
    return data;
  }

  public Object visit(ASTHandler node, Object data) {
    return data;
  }

  public Object visit(ASTStaticInitialization node, Object data) {
    return data;
  }

  public Object visit(ASTIf node, Object data) {
    return data;
  }

  public Object visit(ASTWithin node, Object data) {
    return data;
  }

  public Object visit(ASTWithinCode node, Object data) {
    return data;
  }


  public Object visit(ASTHasMethod node, Object data) {
    return data;
  }

  public Object visit(ASTHasField node, Object data) {
    return data;
  }

  public Object visit(ASTCflow node, Object data) {
    // visit the sub expression
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return data;
  }

  public Object visit(ASTCflowBelow node, Object data) {
    // visit the sub expression
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return data;
  }

  public Object visit(ASTTarget node, Object data) {
    ((List) data).add(node.getIdentifier());
    return data;
  }

  public Object visit(ASTThis node, Object data) {
    ((List) data).add(node.getIdentifier());
    return data;
  }

  // ============ Patterns =============
  public Object visit(ASTClassPattern node, Object data) {
    return data;
  }

  public Object visit(ASTMethodPattern node, Object data) {
    return data;
  }

  public Object visit(ASTConstructorPattern node, Object data) {
    return data;
  }

  public Object visit(ASTFieldPattern node, Object data) {
    return data;
  }

  public Object visit(ASTParameter node, Object data) {
    return data;
  }

  public Object visit(ASTArgs node, Object data) {
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      List args = (List) node.jjtGetChild(i).jjtAccept(this, data);
      ((List) data).addAll(args);
    }
    return data;
  }

  public Object visit(ASTArgParameter node, Object data) {
    TypePattern typePattern = node.getTypePattern();
    ((List) data).add(typePattern.getPattern());
    return data;
  }

  public Object visit(ASTAttribute node, Object data) {
    return data;
  }

  public Object visit(ASTModifier node, Object data) {
    return data;
  }

  /**
   * Returns the string representation of the expression.
   *
   * @return
   */
  public String toString() {
    return m_expression;
  }

}
