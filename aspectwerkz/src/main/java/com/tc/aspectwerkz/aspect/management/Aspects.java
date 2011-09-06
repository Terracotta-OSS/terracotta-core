/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect.management;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.aspect.container.AspectFactoryManager;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.util.ContextClassLoader;

import java.util.*;
import java.lang.reflect.Method;

/**
 * Manages the aspects.
 * <p/>
 * Each Aspect qName has a generated factory (one factory per aspect qName) on which we invoke reflectively
 * the aspectOf and alike. Those are user exposed method. The weaved code does not use those.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class Aspects {

  /**
   * Returns the aspect container class for the given aspect class qName.
   * The qName is returned since we may have only the aspect class name upon lookup
   *
   * @param visibleFrom class loader to look from
   * @param qName
   * @return array of qName and aspect class name (dot formatted as in the aspect definition)
   */
  private static String[] getAspectQNameAndAspectClassName(final ClassLoader visibleFrom, final String qName) {
    AspectDefinition aspectDefinition = lookupAspectDefinition(visibleFrom, qName);
    return new String[]{aspectDefinition.getQualifiedName(), aspectDefinition.getClassName()};
  }

  /**
   * Returns the singleton aspect instance for the aspect with the given qualified name.
   * The aspect is looked up from the thread context classloader.
   *
   * @param qName the qualified name of the aspect
   * @return the singleton aspect instance
   */
  public static Object aspectOf(final String qName) {
    return aspectOf(Thread.currentThread().getContextClassLoader(), qName);
  }

  /**
   * Returns the singleton aspect instance for the given aspect class.
   * Consider using aspectOf(visibleFrom, qName) if the aspect is used more than once
   * or if it is used in a class loader which is child of its own classloader.
   *
   * @param aspectClass the class of the aspect
   * @return the singleton aspect instance
   */
  public static Object aspectOf(final Class aspectClass) {
    String aspectClassName = aspectClass.getName().replace('/', '.');
    return aspectOf(aspectClass.getClassLoader(), aspectClassName);
  }

  /**
   * Returns the singleton aspect instance for given aspect qName, with visibility from the given class loader
   *
   * @param visibleFrom the class loader from where aspect is visible, likely to be the class loader of the
   *                    advised classes, or the one where the system hosting the aspect is deployed.
   * @return the singleton aspect instance
   */
  public static Object aspectOf(final ClassLoader visibleFrom, final String qName) {
    String[] qNameAndAspectClassName = getAspectQNameAndAspectClassName(visibleFrom, qName);
    return aspect$Of(qNameAndAspectClassName[0], qNameAndAspectClassName[1], visibleFrom);
  }

  /**
   * Returns the per class aspect attached to targetClass
   * Consider using aspectOf(qName, targetClass) if the aspect is used more than once
   *
   * @param aspectClass the name of the aspect
   * @param targetClass the target class
   * @return the per class aspect instance
   */
  public static Object aspectOf(final Class aspectClass, final Class targetClass) {
    String aspectClassName = aspectClass.getName().replace('/', '.');
    return aspectOf(aspectClassName, targetClass);
  }

  /**
   * Returns the per class aspect instance for the aspect with the given qualified name for targetClass
   *
   * @param qName       the qualified name of the aspect
   * @param targetClass the target class
   * @return the per class aspect instance
   */
  public static Object aspectOf(final String qName, final Class targetClass) {
    // look up from the targetClass loader is enough in that case
    String[] qNameAndAspectClassName = getAspectQNameAndAspectClassName(targetClass.getClassLoader(), qName);
    return aspect$Of(qNameAndAspectClassName[0], qNameAndAspectClassName[1], targetClass);
  }

  /**
   * Returns the per instance aspect attached to targetInstance
   * Consider using aspectOf(qName, targetInstance) if the aspect is used more than once
   *
   * @param aspectClass    the name of the aspect
   * @param targetInstance the target instance
   * @return the per class aspect instance
   */
  public static Object aspectOf(final Class aspectClass, final Object targetInstance) {
    String aspectClassName = aspectClass.getName().replace('/', '.');
    return aspectOf(aspectClassName, targetInstance);
  }

  /**
   * Returns the per instance aspect attached to targetInstance
   *
   * @param qName          the qualified name of the aspect
   * @param targetInstance the target instance
   * @return the per class aspect instance
   */
  public static Object aspectOf(final String qName, final Object targetInstance) {
    // look up from the targetInstance loader is enough in that case
    AspectDefinition aspectDef = lookupAspectDefinition(targetInstance.getClass().getClassLoader(), qName);
    DeploymentModel deployModel = aspectDef.getDeploymentModel();
    String[] qNameAndAspectClassName = getAspectQNameAndAspectClassName(
            targetInstance.getClass().getClassLoader(), qName);

    if (DeploymentModel.PER_INSTANCE.equals(deployModel)
            || DeploymentModel.PER_THIS.equals(deployModel)
            || DeploymentModel.PER_TARGET.equals(deployModel)) {
      return aspect$Of(qNameAndAspectClassName[0], qNameAndAspectClassName[1], targetInstance);
    } else {
      throw new NoAspectBoundException("Cannot retrieve instance level aspect with "
              + "deployment-scope "
              + deployModel.toString()
              + " named ",
              qName);
    }
  }

  /**
   * Test if there is a per instance aspect (per instance, perthis/target) attached to targetInstance
   *
   * @param qName
   * @param targetInstance
   * @return
   */
  public static boolean hasAspect(final String qName, final Object targetInstance) {
    String[] qNameAndAspectClassName = getAspectQNameAndAspectClassName(targetInstance.getClass().getClassLoader(), qName);
    try {
      Class factory = ContextClassLoader.forName(
              targetInstance.getClass().getClassLoader(),
              AspectFactoryManager.getAspectFactoryClassName(qNameAndAspectClassName[1], qName).replace('/', '.')
      );
      Method m = factory.getMethod("hasAspect", new Class[]{Object.class});
      Boolean b = (Boolean) m.invoke(null, new Object[]{targetInstance});
      return b.booleanValue();
    } catch (Throwable t) {
      return false;
    }
  }

  //---------- weaver exposed
  // TODO can we cache all those ? what would be the key ?

  public static Object aspect$Of(String qName, String aspectClassName, ClassLoader loader) {
    try {
      Class factory = ContextClassLoader.forName(
              loader,
              AspectFactoryManager.getAspectFactoryClassName(aspectClassName, qName).replace('/', '.')
      );
      Method m = factory.getMethod("aspectOf", new Class[0]);
      return m.invoke(null, new Object[0]);
    } catch (NoAspectBoundException nabe) {
      throw nabe;
    } catch (Throwable t) {
      throw new NoAspectBoundException(t, qName);
    }
  }

  public static Object aspect$Of(String qName, String aspectClassName, final Class perClass) {
    try {
      Class factory = ContextClassLoader.forName(
              perClass.getClassLoader(),
              AspectFactoryManager.getAspectFactoryClassName(aspectClassName, qName).replace('/', '.')
      );
      Method m = factory.getMethod("aspectOf", new Class[]{Class.class});
      return m.invoke(null, new Object[]{perClass});
    } catch (NoAspectBoundException nabe) {
      throw nabe;
    } catch (Throwable t) {
      throw new NoAspectBoundException(t, qName);
    }
  }

  public static Object aspect$Of(String qName, String aspectClassName, final Object perInstance) {
    try {
      ClassLoader loader = perInstance.getClass().getClassLoader();
      Class containerClass = ContextClassLoader.forName(
              loader,
              AspectFactoryManager.getAspectFactoryClassName(aspectClassName, qName).replace('/', '.')
      );
      Method m = containerClass.getMethod("aspectOf", new Class[]{Object.class});
      return m.invoke(null, new Object[]{perInstance});
    } catch (NoAspectBoundException nabe) {
      throw nabe;
    } catch (Throwable t) {
      throw new NoAspectBoundException(t, qName);
    }
  }

  //---------- helpers

  /**
   * Lookup the aspect definition with the given qName, visible from the given loader.
   * If qName is a class name only, the fallback will ensure only one aspect use is found.
   *
   * @param visibleFrom
   * @param qName
   * @return
   */
  private static AspectDefinition lookupAspectDefinition(final ClassLoader visibleFrom, final String qName) {
    AspectDefinition aspectDefinition = null;

    Set definitions = SystemDefinitionContainer.getDefinitionsFor(visibleFrom);
    if (qName.indexOf('/') > 0) {
      // has system uuid ie real qName
      for (Iterator iterator = definitions.iterator(); iterator.hasNext();) {
        SystemDefinition systemDefinition = (SystemDefinition) iterator.next();
        if (!qName.startsWith(systemDefinition.getUuid())) {
          continue;
        }
        for (Iterator iterator1 = systemDefinition.getAspectDefinitions().iterator(); iterator1.hasNext();) {
          AspectDefinition aspectDef = (AspectDefinition) iterator1.next();
          if (qName.equals(aspectDef.getQualifiedName())) {
            aspectDefinition = aspectDef;
            break;
          }
        }
      }
    } else {
      // fallback on class name lookup
      // must find at most one
      int found = 0;
      for (Iterator iterator = definitions.iterator(); iterator.hasNext();) {
        SystemDefinition systemDefinition = (SystemDefinition) iterator.next();
        for (Iterator iterator1 = systemDefinition.getAspectDefinitions().iterator(); iterator1.hasNext();) {
          AspectDefinition aspectDef = (AspectDefinition) iterator1.next();
          if (qName.equals(aspectDef.getClassName())) {
            aspectDefinition = aspectDef;
            found++;
          }
        }
      }
      if (found > 1) {
        throw new NoAspectBoundException("More than one AspectDefinition found, consider using other API methods", qName);
      }

    }

    if (aspectDefinition == null) {
      throw new NoAspectBoundException("Could not find AspectDefinition", qName);
    }

    return aspectDefinition;
  }

  /**
   * Class is non-instantiable.
   */
  private Aspects() {
  }

}
