/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect.container;

import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.transform.Properties;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.util.Strings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Create a factory class for different aspect life cycle models.
 * <p/>
 * Each model ends in instantiating the aspect thru "new XAspect()" if there is no aspect container, or thru
 * keeping a reference to the container and delegating to it thru container.aspectOf(..).
 * The container itself is created with a one per factory basis (thus controlled by QName) thru
 * "new XContainer()" or "new XContainer(aspectClass, aopSystemClassLoader, String uuid, String aspectQName, Map aspectParam)"
 * <p/>
 * Each model has the aspectOf(..) method, and the "hasAspect(..)" method.
 * <p/>
 * PerObject has a bind(object) method suitable for perTarget / perThis, that delegates to a supposely implemented
 * interface in the target object itself (to avoid a map)
 * <p/>
 * PerCflowX has a bind(thread) / unbind(thread) suitable for perCflow / perCflowBelow.
 * <p/>
 * TODO: none is synchronized. AspectJ does not synchronize neither...
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 * @author Jonas Bon&#233;r
 */
public abstract class AbstractAspectFactoryCompiler implements Opcodes, TransformationConstants {

  final static Artifact[] EMPTY_ARTIFACT_ARRAY = new Artifact[0];

  private final ClassLoader m_loader;

  protected String m_aspectFactoryClassName;
  protected String[] m_interfaces = null;
  protected final String m_aspectQualifiedName;
  protected final String m_aspectClassName;
  protected final String m_aspectClassSignature;
  protected final String m_aspectContainerClassName;
  protected final boolean m_hasAspectContainer;

  private final String m_uuid;
  private boolean m_hasParameters;
  private String m_rawParameters;
  private Map m_parametersLazy;

  protected ClassWriter m_cw;
  protected MethodVisitor m_clinit;

  public AbstractAspectFactoryCompiler(String uuid,
                                       String aspectClassName,
                                       String aspectQualifiedName,
                                       String containerClassName,
                                       String rawParameters,
                                       ClassLoader loader) {
    m_uuid = uuid;
    m_aspectClassName = aspectClassName.replace('.', '/');
    m_aspectClassSignature = 'L' + m_aspectClassName + ';';
    m_aspectQualifiedName = aspectQualifiedName;
    m_aspectFactoryClassName = AspectFactoryManager.getAspectFactoryClassName(
            m_aspectClassName, m_aspectQualifiedName
    );
    if (containerClassName != null) {
      m_aspectContainerClassName = containerClassName.replace('.', '/');
      m_hasAspectContainer = true;
    } else {
      m_aspectContainerClassName = null;
      m_hasAspectContainer = false;
    }
    if (rawParameters != null) {
      m_rawParameters = rawParameters;
      m_hasParameters = true;
    } else {
      m_rawParameters = null;
      m_hasParameters = false;
    }
    m_loader = loader;
  }

  private Map getParameters() {
    if (m_parametersLazy == null) {
      Map map;
      if (m_rawParameters != null) {
        map = new HashMap();
        String[] raw = Strings.splitString(m_rawParameters, DELIMITER);
        for (int i = 0; i < raw.length; i++) {
          if (i < raw.length) {
            map.put(raw[i], raw[++i]);
          }
        }
      } else {
        map = new HashMap(0);
      }
      m_parametersLazy = map;
    }
    return m_parametersLazy;
  }

  public Artifact compile() {
    m_cw = AsmHelper.newClassWriter(true);

    m_cw.visit(
            AsmHelper.JAVA_VERSION,
            ACC_PUBLIC,
            m_aspectFactoryClassName,
            null,
            OBJECT_CLASS_NAME,
            m_interfaces
    );

    // create a CLASS field to host this factory class
    m_cw.visitField(
            ACC_STATIC + ACC_PRIVATE,
            FACTORY_CLASS_FIELD_NAME,
            CLASS_CLASS_SIGNATURE,
            null,
            null
    );

    // create a clinit method
    m_clinit = m_cw.visitMethod(
            ACC_STATIC,
            CLINIT_METHOD_NAME,
            CLINIT_METHOD_SIGNATURE,
            null,
            null
    );

    // init the CLASS field
    m_clinit.visitLdcInsn(m_aspectFactoryClassName.replace('/', '.'));
    m_clinit.visitMethodInsn(
            INVOKESTATIC,
            CLASS_CLASS,
            FOR_NAME_METHOD_NAME,
            FOR_NAME_METHOD_SIGNATURE
    );
    m_clinit.visitFieldInsn(
            PUTSTATIC,
            m_aspectFactoryClassName,
            FACTORY_CLASS_FIELD_NAME,
            CLASS_CLASS_SIGNATURE
    );

    if (m_hasParameters) {
      createParametersFieldAndClinit();
    }
    if (m_hasAspectContainer) {
      createAspectContainerFieldAndClinit();
    }

    createAspectOf();
    createHasAspect();
    createOtherArtifacts();

    m_clinit.visitInsn(RETURN);
    m_clinit.visitMaxs(0, 0);

    Artifact artifact = new Artifact(m_aspectFactoryClassName, m_cw.toByteArray());

    if (Properties.DUMP_JIT_FACTORIES) try {
      AsmHelper.dumpClass(Properties.DUMP_DIR_FACTORIES, artifact.className, artifact.bytecode);
    } catch (IOException e) {
    }
    return artifact;
  }

  protected abstract void createAspectOf();

  protected abstract void createHasAspect();

  protected abstract void createOtherArtifacts();

  private void createParametersFieldAndClinit() {
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC + ACC_FINAL,
            FACTORY_PARAMS_FIELD_NAME,
            MAP_CLASS_SIGNATURE,
            null,
            null
    );

    m_clinit.visitTypeInsn(NEW, HASH_MAP_CLASS_NAME);
    m_clinit.visitInsn(DUP);
    m_clinit.visitMethodInsn(INVOKESPECIAL, HASH_MAP_CLASS_NAME, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    for (Iterator iterator = getParameters().entrySet().iterator(); iterator.hasNext();) {
      m_clinit.visitInsn(DUP);
      Map.Entry entry = (Map.Entry) iterator.next();
      m_clinit.visitLdcInsn(entry.getKey());
      m_clinit.visitLdcInsn(entry.getValue());
      m_clinit.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, PUT_METHOD_NAME, PUT_METHOD_SIGNATURE);
      m_clinit.visitInsn(POP);
    }
    m_clinit.visitFieldInsn(PUTSTATIC, m_aspectFactoryClassName, FACTORY_PARAMS_FIELD_NAME, MAP_CLASS_SIGNATURE);
  }

  private void createAspectContainerFieldAndClinit() {
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            FACTORY_CONTAINER_FIELD_NAME,
            ASPECT_CONTAINER_CLASS_SIGNATURE,
            null,
            null
    );

    //support 2 different ctor for AspectContainer impl.
    ClassInfo containerClassInfo = AsmClassInfo.getClassInfo(
            m_aspectContainerClassName,
            m_loader
    );
    boolean hasConstructor = false;
    for (int i = 0; i < containerClassInfo.getConstructors().length; i++) {
      ConstructorInfo constructorInfo = containerClassInfo.getConstructors()[i];
      if (ASPECT_CONTAINER_OPTIONAL_INIT_SIGNATURE.equals(constructorInfo.getSignature())) {
        hasConstructor = true;
        break;
      }
      //TODO: check for no-arg ctor to avoid verify error and report error
    }
    m_clinit.visitTypeInsn(NEW, m_aspectContainerClassName);
    m_clinit.visitInsn(DUP);
    if (hasConstructor) {
      m_clinit.visitLdcInsn(m_aspectClassName.replace('/', '.'));
      m_clinit.visitMethodInsn(INVOKESTATIC, CLASS_CLASS, FOR_NAME_METHOD_NAME, FOR_NAME_METHOD_SIGNATURE);
      //Note have access to the CL that defines the aspect (can be child of aspect CL)
      m_clinit.visitFieldInsn(
              GETSTATIC, m_aspectFactoryClassName, FACTORY_CLASS_FIELD_NAME, CLASS_CLASS_SIGNATURE
      );
      m_clinit.visitMethodInsn(
              INVOKEVIRTUAL,
              CLASS_CLASS,
              GETCLASSLOADER_METHOD_NAME,
              CLASS_CLASS_GETCLASSLOADER_METHOD_SIGNATURE
      );
      m_clinit.visitLdcInsn(m_uuid);
      m_clinit.visitLdcInsn(m_aspectQualifiedName);
      if (m_hasParameters) {
        m_clinit.visitFieldInsn(
                GETSTATIC, m_aspectFactoryClassName, FACTORY_PARAMS_FIELD_NAME, MAP_CLASS_SIGNATURE
        );
      } else {
        m_clinit.visitInsn(ACONST_NULL);
      }
      m_clinit.visitMethodInsn(
              INVOKESPECIAL,
              m_aspectContainerClassName,
              INIT_METHOD_NAME,
              ASPECT_CONTAINER_OPTIONAL_INIT_SIGNATURE
      );
    } else {
      m_clinit.visitMethodInsn(
              INVOKESPECIAL, m_aspectContainerClassName, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE
      );
    }
    m_clinit.visitFieldInsn(
            PUTSTATIC, m_aspectFactoryClassName, FACTORY_CONTAINER_FIELD_NAME, ASPECT_CONTAINER_CLASS_SIGNATURE
    );
  }
}
