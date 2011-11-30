/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.deployer;

import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.annotation.AspectAnnotationParser;
import com.tc.aspectwerkz.definition.AdviceDefinition;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.DeploymentScope;
import com.tc.aspectwerkz.definition.DocumentParser;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.joinpoint.management.AdviceInfoContainer;
import com.tc.aspectwerkz.joinpoint.management.JoinPointManager;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.transform.inlining.AspectModelManager;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilationInfo;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilerHelper;
import com.tc.aspectwerkz.transform.inlining.compiler.MatchingJoinPointInfo;

/**
 * Manages deployment and undeployment of aspects. Aspects can be deployed and undeployed into a running system(s).
 * <p/>
 * Supports annotation defined and XML defined aspects.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class Deployer {

  /**
   * Deploys an annotation defined aspect.
   * <p/>
   * Deploys the aspect in all systems in the class loader that has loaded the aspect class.
   * <p/>
   * <b>CAUTION</b>: use a to own risk, the aspect might have a wider scope than your set of instrumented join points,
   * then the aspect will not be applied to all intended points, to play safe -
   * use <code>deploy(final Class aspect, final DeploymentScope deploymentScope)</code>
   *
   * @param aspect the aspect class
   * @return a unique deployment handle for this deployment
   */
  public static DeploymentHandle deploy(final Class aspect) {
    return deploy(aspect, DeploymentScope.MATCH_ALL);
  }

  /**
   * Deploys an annotation defined aspect.
   * <p/>
   * Deploys the aspect in all systems in the class loader that has loaded the aspect class.
   * <p/>
   * <b>CAUTION</b>: use a to own risk, the aspect might have a wider scope than your set of instrumented join points,
   * then the aspect will not be applied to all intended points, to play safe -
   * use <code>deploy(final Class aspect, final DeploymentScope preparedPointcut)</code>
   *
   * @param aspectClassName the aspect class name
   * @return a unique deployment handle for this deployment
   */
  public static DeploymentHandle deploy(final String aspectClassName) {
    return deploy(aspectClassName, DeploymentScope.MATCH_ALL);
  }

  /**
   * Deploys an annotation defined aspect.
   * <p/>
   * Deploys the aspect in all systems in the class loader that is specified.
   * <p/>
   * <b>CAUTION</b>: use a to own risk, the aspect might have a wider scope than your set of instrumented join points,
   * then the aspect will not be applied to all intended points, to play safe -
   * use <code>deploy(final Class aspect, final DeploymentScope preparedPointcut)</code>
   *
   * @param aspect       the aspect class
   * @param deployLoader
   * @return a unique deployment handle for this deployment
   */
  public static DeploymentHandle deploy(final Class aspect, final ClassLoader deployLoader) {
    return deploy(aspect, DeploymentScope.MATCH_ALL, deployLoader);
  }

  /**
   * Deploys an annotation defined aspect.
   * <p/>
   * Deploys the aspect in all systems in the class loader that is specified.
   * <p/>
   * <b>CAUTION</b>: use a to own risk, the aspect might have a wider scope than your set of instrumented join points,
   * then the aspect will not be applied to all intended points, to play safe -
   * use <code>deploy(final Class aspect, final DeploymentScope preparedPointcut)</code>
   *
   * @param aspectClassName the aspect class name
   * @param deployLoader
   * @return a unique deployment handle for this deployment
   */
  public static DeploymentHandle deploy(final String aspectClassName, final ClassLoader deployLoader) {
    return deploy(aspectClassName, DeploymentScope.MATCH_ALL, deployLoader);
  }

  /**
   * Deploys an annotation defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * Deploys the aspect in all systems in the class loader that has loaded the aspect class.
   *
   * @param aspect          the aspect class
   * @param deploymentScope
   * @return a unique deployment handle for this deployment
   */
  public static DeploymentHandle deploy(final Class aspect, final DeploymentScope deploymentScope) {
    return deploy(aspect, deploymentScope, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Deploys an annotation defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * Deploys the aspect in all systems in the class loader that has loaded the aspect class.
   *
   * @param aspectClassName the aspect class name
   * @param deploymentScope
   * @return a unique deployment handle for this deployment
   */
  public static DeploymentHandle deploy(final String aspectClassName, final DeploymentScope deploymentScope) {
    return deploy(aspectClassName, deploymentScope, Thread.currentThread().getContextClassLoader());
  }

  /**
   * TODO allow deployment in other systems than virtual system?
   * <p/>
   * Deploys an annotation defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * Deploys the aspect in the class loader that is specified.
   *
   * @param aspect          the aspect class
   * @param deployLoader    the loader to deploy the aspect in
   * @param deploymentScope the prepared pointcut
   * @return a unique deployment handle for this deployment
   */
  public static DeploymentHandle deploy(final Class aspect,
                                        final DeploymentScope deploymentScope,
                                        final ClassLoader deployLoader) {
    if (aspect == null) {
      throw new IllegalArgumentException("aspect to deploy can not be null");
    }
    if (deploymentScope == null) {
      throw new IllegalArgumentException("prepared pointcut can not be null");
    }
    if (deployLoader == null) {
      throw new IllegalArgumentException("class loader to deploy aspect in can not be null");
    }

    final String className = aspect.getName();
    return deploy(className, deploymentScope, deployLoader);

  }

  /**
   * Deploys an annotation defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * Deploys the aspect in the class loader that is specified.
   *
   * @param className
   * @param deploymentScope
   * @param deployLoader
   * @return
   */
  public synchronized static DeploymentHandle deploy(final String className,
                                                     final DeploymentScope deploymentScope,
                                                     final ClassLoader deployLoader) {
    logDeployment(className, deployLoader);

    Class aspectClass = null;
    try {
      aspectClass = Class.forName(className, false, deployLoader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
              "could not load class [" + className + "] in class loader [" + deployLoader + "]"
      );
    }

    final DeploymentHandle deploymentHandle = new DeploymentHandle(aspectClass, deployLoader);

    final ClassInfo aspectClassInfo = JavaClassInfo.getClassInfo(aspectClass);

    // create a new aspect def and fill it up with the annotation def from the aspect class
    final SystemDefinition systemDef = SystemDefinitionContainer.getVirtualDefinitionFor(deployLoader);
    final AspectDefinition newAspectDef = new AspectDefinition(className, aspectClassInfo, systemDef);
    final Set newExpressions = getNewExpressionsForAspect(
            aspectClass, newAspectDef, systemDef, deploymentScope, deploymentHandle
    );

    redefine(newExpressions);
    return deploymentHandle;
  }

  /**
   * Deploys an XML defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * If the aspect class has annotations, those will be read but the XML definition will override the
   * annotation definition.
   * <p/>
   * Deploys the aspect in the class loader that has loaded the aspect.
   *
   * @param aspect the aspect class
   * @param xmlDef
   * @return
   */
  public static DeploymentHandle deploy(final Class aspect, final String xmlDef) {
    return deploy(aspect, xmlDef, DeploymentScope.MATCH_ALL);
  }

  /**
   * Deploys an XML defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * If the aspect class has annotations, those will be read but the XML definition will override the
   * annotation definition.
   * <p/>
   * Deploys the aspect in the class loader that has loaded the aspect.
   *
   * @param aspect          the aspect class
   * @param xmlDef
   * @param deploymentScope
   * @return
   */
  public static DeploymentHandle deploy(final Class aspect,
                                        final String xmlDef,
                                        final DeploymentScope deploymentScope) {
    return deploy(aspect, xmlDef, deploymentScope, aspect.getClassLoader());
  }

  /**
   * Deploys an XML defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * If the aspect class has annotations, those will be read but the XML definition will override the
   * annotation definition.
   * <p/>
   * Deploys the aspect in the class loader that is specified.
   *
   * @param aspect       the aspect class
   * @param xmlDef
   * @param deployLoader
   * @return
   */
  public static DeploymentHandle deploy(final Class aspect, final String xmlDef, final ClassLoader deployLoader) {
    return deploy(aspect, xmlDef, DeploymentScope.MATCH_ALL, deployLoader);
  }

  /**
   * TODO allow deployment in other systems than virtual system?
   * <p/>
   * Deploys an XML defined aspect in the scope defined by the prepared pointcut.
   * <p/>
   * If the aspect class has annotations, those will be read but the XML definition will override the
   * annotation definition.
   * <p/>
   * Deploys the aspect in the class loader that is specified.
   *
   * @param aspect          the aspect class
   * @param deploymentScope
   * @param xmlDef
   * @param deployLoader
   * @return
   */
  public synchronized static DeploymentHandle deploy(final Class aspect,
                                                     final String xmlDef,
                                                     final DeploymentScope deploymentScope,
                                                     final ClassLoader deployLoader) {
    if (aspect == null) {
      throw new IllegalArgumentException("aspect to deploy can not be null");
    }
    if (deploymentScope == null) {
      throw new IllegalArgumentException("prepared pointcut can not be null");
    }
    if (xmlDef == null) {
      throw new IllegalArgumentException("xml definition can not be null");
    }
    if (deployLoader == null) {
      throw new IllegalArgumentException("class loader to deploy aspect in can not be null");
    }
    final String className = aspect.getName();
    logDeployment(className, deployLoader);

    final DeploymentHandle deploymentHandle = new DeploymentHandle(aspect, deployLoader);

    final SystemDefinition systemDef = SystemDefinitionContainer.getVirtualDefinitionFor(deployLoader);

    final AspectDefinition newAspectDef = DocumentParser.parseAspectDefinition(xmlDef, systemDef, aspect);
    systemDef.addAspect(newAspectDef);
    final Set newExpressions = getNewExpressionsForAspect(aspect, newAspectDef, systemDef, deploymentScope,
                                                          deploymentHandle);
    redefine(newExpressions);
    return deploymentHandle;
  }

  /**
   * Undeploys an aspect from the same loader that has loaded the class.
   *
   * @param aspect the aspect class
   */
  public static void undeploy(final Class aspect) {
    undeploy(aspect, aspect.getClassLoader());
  }

  /**
   * Undeploys an aspect from a specific class loader.
   *
   * @param aspect the aspect class
   * @param loader the loader that you want to undeploy the aspect from
   */
  public static void undeploy(final Class aspect, final ClassLoader loader) {
    if (aspect == null) {
      throw new IllegalArgumentException("aspect to undeploy can not be null");
    }
    if (loader == null) {
      throw new IllegalArgumentException("loader to undeploy aspect from can not be null");
    }
    undeploy(aspect.getName(), loader);
  }

  /**
   * Undeploys an aspect from a specific class loader.
   *
   * @param className the aspect class name
   * @param loader    the loader that you want to undeploy the aspect from
   */
  public static void undeploy(final String className, final ClassLoader loader) {
    logUndeployment(className, loader);

    //TODO: this one should acquire lock or something

    // lookup only in the given classloader scope
    // since the system hierarchy holds reference, they will see the change
    Set systemDefs = SystemDefinitionContainer.getAllDefinitionsFor(loader);

    for (Iterator it = systemDefs.iterator(); it.hasNext();) {
      SystemDefinition systemDef = (SystemDefinition) it.next();
      final AspectDefinition aspectDef = systemDef.getAspectDefinition(className);
      if (aspectDef != null) {

        final Set newExpressions = new HashSet();
        for (Iterator it2 = aspectDef.getAdviceDefinitions().iterator(); it2.hasNext();) {
          AdviceDefinition adviceDef = (AdviceDefinition) it2.next();
          ExpressionInfo oldExpression = adviceDef.getExpressionInfo();
          if (oldExpression == null) { // if null, then already undeployed
            continue;
          }
          adviceDef.setExpressionInfo(null);
          newExpressions.add(oldExpression);
        }
        redefine(newExpressions);
      }
    }
  }

  /**
   * Undeploys an aspect in the same way that it has been deployed in in the previous deploy event
   * defined by the deployment handle.
   *
   * @param deploymentHandle the handle to the previous deployment event
   */
  public static void undeploy(final DeploymentHandle deploymentHandle) {
    if (deploymentHandle == null) {
      throw new IllegalArgumentException("deployment handle can not be null");
    }

    deploymentHandle.revertChanges();

    final Class aspectClass = deploymentHandle.getAspectClass();
    if (aspectClass == null) {
      return; // already undeployed
    }
    undeploy(aspectClass);
  }

  /**
   * Redefines all join points that are affected by the system redefinition.
   *
   * @param expressions the expressions that will pick out the join points that are affected
   */
  private static void redefine(final Set expressions) {
    final Set allMatchingJoinPoints = new HashSet();
    for (Iterator itExpr = expressions.iterator(); itExpr.hasNext();) {
      ExpressionInfo expression = (ExpressionInfo) itExpr.next();
      Set matchingJoinPoints = CompilerHelper.getJoinPointsMatching(expression);
      allMatchingJoinPoints.addAll(matchingJoinPoints);
    }

    final ChangeSet changeSet = new ChangeSet();
    for (Iterator it = allMatchingJoinPoints.iterator(); it.hasNext();) {
      final MatchingJoinPointInfo joinPointInfo = (MatchingJoinPointInfo) it.next();

      final CompilationInfo compilationInfo = joinPointInfo.getCompilationInfo();
      compilationInfo.incrementRedefinitionCounter();

      changeSet.addElement(new ChangeSet.Element(compilationInfo, joinPointInfo));
    }

    doRedefine(changeSet);
  }

  /**
   * Do the redefinition of the existing join point and the compilation of the new join point.
   *
   * @param changeSet
   */
  private static void doRedefine(final ChangeSet changeSet) {
    for (Iterator it = changeSet.getElements().iterator(); it.hasNext();) {
      compileNewJoinPoint((ChangeSet.Element) it.next());
    }
    redefineInitialJoinPoints(changeSet);
  }

  /**
   * Compiles a completely new join point instance based on the new redefined model.
   *
   * @param changeSetElement the change set item
   */
  private static void compileNewJoinPoint(final ChangeSet.Element changeSetElement) {
    final CompilationInfo compilationInfo = changeSetElement.getCompilationInfo();
    final MatchingJoinPointInfo joinPointInfo = changeSetElement.getJoinPointInfo();
    final ClassLoader loader = joinPointInfo.getJoinPointClass().getClassLoader();
    final AdviceInfoContainer newAdviceContainer = JoinPointManager.getAdviceInfoContainerForJoinPoint(
            joinPointInfo.getExpressionContext(), loader, null
    );
    final CompilationInfo.Model redefinedModel = new CompilationInfo.Model(
            compilationInfo.getInitialModel().getEmittedJoinPoint(), // copy the reference since it is the same
            newAdviceContainer,
            compilationInfo.getRedefinitionCounter(),
            compilationInfo.getInitialModel().getThisClassInfo()
    );
    CompilerHelper.compileJoinPointAndAttachToClassLoader(redefinedModel, loader);

    compilationInfo.setRedefinedModel(redefinedModel);
    CompilerHelper.addCompilationInfo(joinPointInfo.getJoinPointClass(), compilationInfo);
  }

  /**
   * Redefines the intial (weaved in) join point to delegate to the newly compiled "real" join point which is
   * based on the new redefined model.
   *
   * @param changeSet the change set
   */
  private static void redefineInitialJoinPoints(final ChangeSet changeSet) {
    // TODO type should be pluggable
    RedefinerFactory.newRedefiner(RedefinerFactory.Type.HOTSWAP).redefine(changeSet);
  }

  /**
   * Returns a set with the new expressions for the advice in the aspect to deploy.
   *
   * @param aspectClass
   * @param newAspectDef
   * @param systemDef
   * @param deploymentScope
   * @param deploymentHandle
   * @return a set with the new expressions
   */
  private static Set getNewExpressionsForAspect(final Class aspectClass,
                                                final AspectDefinition newAspectDef,
                                                final SystemDefinition systemDef,
                                                final DeploymentScope deploymentScope,
                                                final DeploymentHandle deploymentHandle) {
    final ClassLoader aspectLoader = aspectClass.getClassLoader();
    final String aspectName = aspectClass.getName();

    // keep XML settings so that they don't get changed when we read the annotations
    String keptContainerClassName = newAspectDef.getContainerClassName();
    DeploymentModel keptModel = newAspectDef.getDeploymentModel();

    final ClassInfo classInfo = AsmClassInfo.getClassInfo(aspectName, aspectLoader);
    AspectModelManager.defineAspect(classInfo, newAspectDef, aspectLoader);
    AspectAnnotationParser.parse(classInfo, newAspectDef, aspectLoader);

    AspectDefinition aspectDef = systemDef.getAspectDefinition(aspectName);
    if (aspectDef != null) {
      // if in def already reuse some of the settings that can have been overridded by XML def
      //AW-461
      newAspectDef.setContainerClassName(keptContainerClassName);//aspectDef.getContainerClassName());
      newAspectDef.setDeploymentModel(keptModel);//aspectDef.getDeploymentModel());
    }

    systemDef.addAspectOverwriteIfExists(newAspectDef);

    final Set newExpressions = new HashSet();
    for (Iterator it2 = newAspectDef.getAdviceDefinitions().iterator(); it2.hasNext();) {
      AdviceDefinition adviceDef = (AdviceDefinition) it2.next();
      ExpressionInfo oldExpression = adviceDef.getExpressionInfo();
      if (oldExpression == null) {
        continue;
      }
      deploymentHandle.registerDefinitionChange(adviceDef, oldExpression);

      final ExpressionInfo newExpression = deploymentScope.newExpressionInfo(oldExpression);
      adviceDef.setExpressionInfo(newExpression);
      newExpressions.add(newExpression);
    }
    return newExpressions;
  }

  /**
   * Imports a class from one class loader to another one.
   *
   * @param clazz    the class to import
   * @param toLoader the loader to import to
   */
//  private static void importClassIntoLoader(final Class clazz, final ClassLoader toLoader) {
//    final ClassLoader fromLoader = clazz.getClassLoader();
//    if (toLoader == fromLoader) {
//      return;
//    }
//    final String className = clazz.getName();
//    try {
//      Class.forName(className, false, toLoader);
//    } catch (ClassNotFoundException cnfe) {
//      try {
//        InputStream stream = null;
//        byte[] bytes;
//        try {
//          stream = fromLoader.getResourceAsStream(className.replace('.', '/') + ".class");
//          bytes = new ClassReader(stream).b;
//        } finally {
//          try {
//            stream.close();
//          } catch (Exception e) {
//            // ignore
//          }
//        }
//        Class klass = Class.forName("java.lang.ClassLoader", false, toLoader);
//        Method method = klass.getDeclaredMethod(
//                "defineClass",
//                new Class[]{String.class, byte[].class, int.class, int.class}
//        );
//        method.setAccessible(true);
//        Object[] args = new Object[]{
//                clazz.getName(), bytes, Integer.valueOf(0), Integer.valueOf(bytes.length)
//        };
//        method.invoke(toLoader, args);
//        method.setAccessible(false);
//      } catch (Exception e) {
//        throw new RuntimeException(
//                new StringBuffer().append("could not deploy aspect [").
//                        append(className).append("] in class loader [").append(toLoader)
//                        .append(']').toString()
//        );
//      }
//    }
//  }

  /**
   * Logs undeployment.
   * <p/>
   * TODO unified way or at least format for logging
   *
   * @param className
   * @param loader
   */
  private static void logUndeployment(final String className, final ClassLoader loader) {
    System.out.println(
            new StringBuffer().append("Deployer::INFO - undeploying aspect [").
                    append(className).append("] from class loader [").
                    append(loader).append(']').toString()
    );
  }

  /**
   * Logs deployment.
   * <p/>
   * TODO unified way or at least format for logging
   *
   * @param className
   * @param loader
   */
  private static void logDeployment(final String className, final ClassLoader loader) {
    System.out.println(
            new StringBuffer().append("Deployer::INFO - deploying aspect [").
                    append(className).append("] in class loader [").
                    append(loader).append(']').toString()
    );
  }
}
