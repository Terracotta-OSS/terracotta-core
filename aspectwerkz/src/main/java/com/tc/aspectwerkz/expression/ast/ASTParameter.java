/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.aspectwerkz.expression.ast;

import com.tc.aspectwerkz.expression.regexp.Pattern;
import com.tc.aspectwerkz.expression.regexp.TypePattern;
import com.tc.aspectwerkz.expression.SubtypePatternType;

public class ASTParameter extends SimpleNode {

  private TypePattern m_declaringClassPattern;

  public ASTParameter(int id) {
    super(id);
  }

  public ASTParameter(ExpressionParser p, int id) {
    super(p, id);
  }

  public Object jjtAccept(ExpressionParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  public void setTypePattern(String pattern) {
    if (pattern.endsWith("+")) {
      pattern = pattern.substring(0, pattern.length() - 1);
      m_declaringClassPattern = Pattern.compileTypePattern(pattern, SubtypePatternType.MATCH_ON_ALL_METHODS);
    } else if (pattern.endsWith("#")) {
      pattern = pattern.substring(0, pattern.length() - 1);
      m_declaringClassPattern = Pattern.compileTypePattern(
              pattern,
              SubtypePatternType.MATCH_ON_BASE_TYPE_METHODS_ONLY
      );
    } else {
      m_declaringClassPattern = Pattern.compileTypePattern(pattern, SubtypePatternType.NOT_HIERARCHICAL);
    }
  }

  public TypePattern getDeclaringClassPattern() {
    return m_declaringClassPattern;
  }
}
