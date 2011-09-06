/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.annotation;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.ExpressionNamespace;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.definition.DefinitionParserHelper;
import com.tc.aspectwerkz.definition.MixinDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;

/**
 * Extracts the mixin annotations from the class files and creates a meta-data representation of them.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class MixinAnnotationParser {

  /**
   * The sole instance.
   */
  private final static MixinAnnotationParser INSTANCE = new MixinAnnotationParser();

  /**
   * Private constructor to prevent subclassing.
   */
  private MixinAnnotationParser() {
  }

  /**
   * Parse the attributes and create and return a meta-data representation of them.
   *
   * @param classInfo the class to extract attributes from
   * @param mixinDef  the mixin definition
   */
  public static void parse(final ClassInfo classInfo, final MixinDefinition mixinDef) {
    INSTANCE.doParse(classInfo, mixinDef);
  }

  /**
   * Parse the attributes and create and return a meta-data representation of them.
   *
   * @param classInfo the class to extract attributes from
   * @param mixinDef  the mixin definition
   */
  private void doParse(final ClassInfo classInfo, final MixinDefinition mixinDef) {
    if (classInfo == null) {
      throw new IllegalArgumentException("class to parse can not be null");
    }
    final SystemDefinition systemDef = mixinDef.getSystemDefinition();
    com.tc.aspectwerkz.annotation.Mixin annotation = (Mixin) AsmAnnotations.getAnnotation(AnnotationConstants.MIXIN, classInfo);
    if (annotation != null) {
      String expression = AspectAnnotationParser.getExpressionElseValue(
              annotation.value(), annotation.pointcut()
      );
      final ExpressionInfo expressionInfo = new ExpressionInfo(expression, systemDef.getUuid());
      ExpressionNamespace.getNamespace(systemDef.getUuid()).addExpressionInfo(
              DefinitionParserHelper.EXPRESSION_PREFIX + expression.hashCode(),
              expressionInfo
      );
      mixinDef.addExpressionInfo(expressionInfo);
      mixinDef.setTransient(annotation.isTransient());
      if (annotation.deploymentModel() != null) {
        mixinDef.setDeploymentModel(DeploymentModel.getDeploymentModelFor(annotation.deploymentModel()));
      }
    }
  }
}