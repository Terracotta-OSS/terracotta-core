/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining;


import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.transform.inlining.model.AopAllianceAspectModel;
import com.tc.aspectwerkz.transform.inlining.model.AspectWerkzAspectModel;
import com.tc.aspectwerkz.transform.inlining.model.SpringAspectModel;
import com.tc.aspectwerkz.transform.inlining.spi.AspectModel;
import com.tc.aspectwerkz.util.ContextClassLoader;
import com.tc.aspectwerkz.reflect.ClassInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Manages the different aspect model implementations that is registered.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class AspectModelManager {

  public static final String ASPECT_MODELS_VM_OPTION = "aspectwerkz.extension.aspectmodels";
  private static final String DELIMITER = ":";

  /**
   * The aspects models that are registered
   */
  private static List ASPECT_MODELS = new ArrayList(1);

  static {
    ASPECT_MODELS.add(new AspectWerkzAspectModel());
    ASPECT_MODELS.add(new AopAllianceAspectModel());
    ASPECT_MODELS.add(new SpringAspectModel());
    registerCustomAspectModels(System.getProperty(ASPECT_MODELS_VM_OPTION, null));

  }

  /**
   * Returns an array with all the aspect models that has been registered.
   *
   * @return an array with the aspect models
   */
  public static AspectModel[] getModels() {
    return (AspectModel[]) ASPECT_MODELS.toArray(new AspectModel[]{});
  }

  /**
   * Returns the advice model for a specific aspect model type id.
   *
   * @param type the aspect model type id
   * @return the aspect model
   */
  public static AspectModel getModelFor(String type) {
    for (Iterator iterator = ASPECT_MODELS.iterator(); iterator.hasNext();) {
      AspectModel aspectModel = (AspectModel) iterator.next();
      if (aspectModel.getAspectModelType().equals(type)) {
        return aspectModel;
      }
    }
    return null;
  }

  /**
   * Let all aspect models try to define the aspect (only one should succeed).
   *
   * @param aspectClassInfo
   * @param aspectDef
   * @param loader
   */
  public static void defineAspect(final ClassInfo aspectClassInfo,
                                  final AspectDefinition aspectDef,
                                  final ClassLoader loader) {
    for (Iterator iterator = ASPECT_MODELS.iterator(); iterator.hasNext();) {
      AspectModel aspectModel = (AspectModel) iterator.next();
      aspectModel.defineAspect(aspectClassInfo, aspectDef, loader);
    }
  }

  /**
   * Registers aspect models.
   *
   * @param aspectModels the class names of the aspect models to register concatenated and separated with a ':'.
   */
  public static void registerCustomAspectModels(final String aspectModels) {
    if (aspectModels != null) {
      StringTokenizer tokenizer = new StringTokenizer(aspectModels, DELIMITER);
      while (tokenizer.hasMoreTokens()) {
        final String className = tokenizer.nextToken();
        try {
          final Class modelClass = ContextClassLoader.forName(className);
          ASPECT_MODELS.add(modelClass.newInstance());
        } catch (ClassNotFoundException e) {
          throw new DefinitionException(
                  "aspect model implementation class not found [" +
                          className + "]: " + e.toString()
          );
        } catch (Exception e) {
          throw new DefinitionException(
                  "aspect model implementation class could not be instantiated [" +
                          className +
                          "] - make sure it has a default no argument constructor: " +
                          e.toString()
          );
        }
      }
    }
  }
}
