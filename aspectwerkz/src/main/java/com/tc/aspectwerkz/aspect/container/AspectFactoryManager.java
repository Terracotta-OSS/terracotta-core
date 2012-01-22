/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect.container;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.util.ContextClassLoader;

/**
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class AspectFactoryManager {

  public static String getAspectFactoryClassName(String aspectClassName, String aspectQualifiedName) {
    return aspectClassName.replace('.', '/') + "$" + aspectQualifiedName.replace('.', '/').replace('/', '_') + "_AWFactory";
  }

  // TODO refactor for offline stuff -genjp

  /**
   * Ensure that the aspect factory is loaded.
   *
   * @param aspectFactoryClassName
   * @param aspectClassName
   * @param aspectQualifiedName
   * @param containerClassName
   * @param rawParameters
   * @param loader
   * @param deploymentModelAsString
   */
  public static void loadAspectFactory(
          String aspectFactoryClassName,
          String uuid,
          String aspectClassName,
          String aspectQualifiedName,
          String containerClassName,
          String rawParameters,
          ClassLoader loader,
          String deploymentModelAsString) {

    try {
      ContextClassLoader.forName(loader, aspectFactoryClassName.replace('/', '.'));
    } catch (ClassNotFoundException e) {
      // compile it
      DeploymentModel deploymentModel = DeploymentModel.getDeploymentModelFor(deploymentModelAsString);
      AbstractAspectFactoryCompiler compiler;

      if (DeploymentModel.PER_JVM.equals(deploymentModel)) {
        compiler = new PerJVMAspectFactoryCompiler(
                uuid,
                aspectClassName,
                aspectQualifiedName,
                containerClassName,
                rawParameters,
                loader
        );
      } else if (DeploymentModel.PER_CLASS.equals(deploymentModel)) {
        compiler = new LazyPerXFactoryCompiler.PerClassAspectFactoryCompiler(
                uuid,
                aspectClassName,
                aspectQualifiedName,
                containerClassName,
                rawParameters,
                loader
        );
      } else if (DeploymentModel.PER_INSTANCE.equals(deploymentModel)) {
        compiler = new PerObjectFactoryCompiler.PerInstanceFactoryCompiler(
                uuid,
                aspectClassName,
                aspectQualifiedName,
                containerClassName,
                rawParameters,
                loader
        );
      } else if (DeploymentModel.PER_TARGET.equals(deploymentModel)
              || DeploymentModel.PER_THIS.equals(deploymentModel)) {
        compiler = new PerObjectFactoryCompiler(
                uuid,
                aspectClassName,
                aspectQualifiedName,
                containerClassName,
                rawParameters,
                loader
        );
      } else if (DeploymentModel.PER_CFLOW.equals(deploymentModel)
              || DeploymentModel.PER_CFLOWBELOW.equals(deploymentModel)) {
        compiler = new PerCflowXAspectFactoryCompiler(
                uuid,
                aspectClassName,
                aspectQualifiedName,
                containerClassName,
                rawParameters,
                loader
        );
      } else {
        //FIXME perThread
        throw new Error("aspect factory not implemented for deployment model: " + deploymentModel);
      }
      Artifact artifact = compiler.compile();
      Class factory = AsmHelper.defineClass(loader, artifact.bytecode, artifact.className);

//            System.out.println("factory.getClassLoader() = " + factory.getClassLoader());
//            try {
//                Method m = factory.getMethod("aspectOf", new Class[]{});
//                Object aspect = m.invoke(null, new Object[]{});
//                System.out.println("aspect = " + aspect);
//            } catch (NoSuchMethodException e1) {
//                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            } catch (IllegalAccessException e1) {
//                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            } catch (InvocationTargetException e1) {
//                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
    }
  }
}
