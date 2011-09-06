/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect.management;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.aspect.DefaultMixinFactory;
import com.tc.aspectwerkz.aspect.MixinFactory;
import com.tc.aspectwerkz.definition.MixinDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.util.ContextClassLoader;

import java.util.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Manages the mixins, registry for the mixin factories (one factory per mixin type).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class Mixins {

  /**
   * The default mixin factory class.
   */
  public static final String DEFAULT_MIXIN_FACTORY = DefaultMixinFactory.class.getName();

  /**
   * Map with all the mixin factories mapped to the mixin class
   */
  private static final Map MIXIN_FACTORIES = new WeakHashMap();

  /**
   * Returns the mixin factory for the mixin with the given name.
   *
   * @param mixinClass            the class of the mixin
   * @param mixinCalledFromLoader
   * @return the factory, put in cache based on mixin class as a key
   */
  public static MixinFactory getFactory(final Class mixinClass, final ClassLoader mixinCalledFromLoader) {
    synchronized (MIXIN_FACTORIES) {
      MixinFactory factory = (MixinFactory) MIXIN_FACTORIES.get(mixinClass);
      if (factory == null) {
        factory = createMixinFactory(mixinClass, mixinCalledFromLoader);
        //FIXME by using a lookup by uuid/aspectNickName
        // right now broken since we have 1 container per mixin CLASS while the definition
        // does allow for some mix (several mixin, several container, same mixin class)
        MIXIN_FACTORIES.put(mixinClass, factory);
      }
      return factory;
    }
  }

  /**
   * Returns the per JVM mixin instance for the mixin with the given name
   *
   * @param name   the name of the mixin
   * @param loader target class classloader
   * @return the per jvm mixin instance
   */
  public static Object mixinOf(final String name, ClassLoader loader) {
    try {
      Class mixinClass = Class.forName(name, false, loader);
      return mixinOf(mixinClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not load mixin " + name + " from " + loader);
    }
  }

  /**
   * Returns the per jvm mixin instance for the mixin with the given implementation class
   * deployed using the perJVM model.
   *
   * @param mixinClass the name of the mixin
   * @return the per jvm mixin instance
   */
  public static Object mixinOf(final Class mixinClass) {
    return getFactory(mixinClass, mixinClass.getClassLoader()).mixinOf();
  }

  /**
   * Returns the per class mixin instance for the mixin with the given name for the perClass model
   *
   * @param name        the name of the mixin
   * @param targetClass the targetClass class
   * @return the per class mixin instance
   */
  public static Object mixinOf(final String name, final Class targetClass) {
    try {
      Class mixinClass = Class.forName(name, false, targetClass.getClassLoader());
      return mixinOf(mixinClass, targetClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not load mixin " + name + " from " + targetClass.getClassLoader());
    }
  }

  /**
   * Returns the per class mixin instance for the mixin with the given implemnentation class
   * deployed using the perClass model.
   *
   * @param mixinClass  the name of the mixin
   * @param targetClass the targetClass class
   * @return the per class mixin instance
   */
  public static Object mixinOf(final Class mixinClass, final Class targetClass) {
    return getFactory(mixinClass, targetClass.getClassLoader()).mixinOf(targetClass);
  }

  /**
   * Returns the per targetClass instance mixin instance for the mixin with the given name for the perInstance model.
   *
   * @param name           the name of the mixin
   * @param targetInstance the targetClass instance, can be null (static method, ctor call)
   * @return the per instance mixin instance, fallback on perClass if targetInstance is null
   */
  public static Object mixinOf(final String name, final Object targetInstance) {
    try {
      Class mixinClass = Class.forName(name, false, targetInstance.getClass().getClassLoader());
      return mixinOf(mixinClass, targetInstance);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
              "could not load mixin " + name + " from " + targetInstance.getClass().getClassLoader()
      );
    }
  }

  /**
   * Returns the per class mixin instance for the mixin with the given implemnentation class
   * deployed using the perClass model.
   *
   * @param mixinClass     the name of the mixin
   * @param targetInstance the targetClass instance, can be null
   * @return the per targetClass instance mixin instance, fallback to perClass if targetInstance is null
   */
  public static Object mixinOf(final Class mixinClass, final Object targetInstance) {
    //TODO WHAT IF targetInstance is null ? f.e. ITD static methods
    return getFactory(mixinClass, targetInstance.getClass().getClassLoader()).mixinOf(targetInstance);
  }

  /**
   * Creates a new mixin factory.
   *
   * @param mixinClass            the mixin class
   * @param mixinCalledFromLoader classloader of the target class advised by the mixin (app server packaging)
   */
  private static MixinFactory createMixinFactory(final Class mixinClass, final ClassLoader mixinCalledFromLoader) {
    final MixinDefinition mixinDefinition = getMixinDefinition(mixinClass, mixinCalledFromLoader);

    String factoryClassName = mixinDefinition.getFactoryClassName();
    try {
      Class containerClass;
      if (factoryClassName == null) {
        containerClass = ContextClassLoader.forName(mixinClass.getClassLoader(), DEFAULT_MIXIN_FACTORY);
      } else {
        containerClass = ContextClassLoader.forName(mixinClass.getClassLoader(), factoryClassName);
      }
      Constructor constructor = containerClass.getConstructor(new Class[]{Class.class, DeploymentModel.class});
      final MixinFactory factory = (MixinFactory) constructor.newInstance(
              new Object[]{mixinClass, mixinDefinition.getDeploymentModel()}
      );
      return factory;
    } catch (InvocationTargetException e) {
      throw new DefinitionException(e.getTargetException().toString());
    } catch (NoSuchMethodException e) {
      throw new DefinitionException(
              "mixin factory does not have a valid constructor ["
                      + factoryClassName
                      + "] need to have a signature like this [MyMixinFactory(Class mixin, DeploymentModel scope)]: "
                      + e.toString()
      );
    } catch (Throwable e) {
      StringBuffer cause = new StringBuffer();
      cause.append("could not create mixin container using the implementation specified [");
      cause.append(factoryClassName);
      cause.append("] due to: ");
      cause.append(e.toString());
      throw new DefinitionException(cause.toString());
    }
  }

  /**
   * Returns the parameter for a mixin based on the mixin implementation class and a classloader from
   * where the mixin is visible (the classloader that owns the aop.xml with the "mixin" element, or a child of it).
   * <p/>
   * Note: the mixinClass classloader can be different, if you place the mixin in the system classpath, and reference
   * it only from a deployed application.
   * <p/>
   * Note: you should not use a mixin more than once. Consider subclassing the mixin in this case. The returned parameters
   * are the one from the first mixin found.
   *
   * @param mixinClass
   * @return
   */
  public static Map getParameters(Class mixinClass, ClassLoader loader) {
    MixinDefinition mixinDefinition = getMixinDefinition(mixinClass, loader);
    return mixinDefinition.getParameters();
  }

  /**
   * Lookups a mixin definition based on the mixin impl class and a classloader from where the mixin is
   * visible. The given classloader can be different from the mixin class classloader.
   *
   * @param mixinClass
   * @param visibleFrom
   * @return
   */
  public static MixinDefinition getMixinDefinition(Class mixinClass, ClassLoader visibleFrom) {
    MixinDefinition mixinDefinition = null;

    Set definitions = SystemDefinitionContainer.getDefinitionsFor(visibleFrom);
    for (Iterator iterator = definitions.iterator(); iterator.hasNext() && mixinDefinition == null;) {
      SystemDefinition systemDefinition = (SystemDefinition) iterator.next();
      for (Iterator iterator1 = systemDefinition.getMixinDefinitions().iterator(); iterator1.hasNext();) {
        MixinDefinition mixinDef = (MixinDefinition) iterator1.next();
        if (mixinClass.getName().replace('/', '.').equals(mixinDef.getMixinImpl().getName())) {
          mixinDefinition = mixinDef;
          break;
        }
      }
    }
    if (mixinDefinition == null) {
      throw new DefinitionException("could not find definition for mixin: " + mixinClass.getName()
              + " (loader " + mixinClass.getClassLoader() + ")"
              + " from loader " + visibleFrom);
    }
    return mixinDefinition;
  }

  /**
   * Class is non-instantiable.
   */
  private Mixins() {
  }
}
