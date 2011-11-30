/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz;

import com.tc.aspectwerkz.definition.AspectDefinition;

import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about and for classes that has been defined as cross-cutting.
 * 
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public final class AspectContext {
  /**
   * An empty <code>Object</code> array.
   */
  public static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};

  /**
   * The name for the cross-cuttable class.
   */
  private String m_name;

  /**
   * The qualified name of the aspect. Stored for serialization purpose.
   */
  private String m_qName;

  /**
   * The aspect class, wrapped in a weak reference since is a key of aspect container referenced by this object.
   */
  private transient WeakReference m_aspectClassRef;

  /**
   * Holds the deployment model.
   */
  private DeploymentModel m_deploymentModel;

  /**
   * Holds the parameters passed to the aspect.
   */
  private Map m_parameters = new HashMap();

  /**
   * Holds the metadata.
   */
  private Map m_metaData = new HashMap();

  /**
   * The UUID for the system.
   */
  private String m_uuid;

  /**
   * The aspect definition.
   */
  private transient AspectDefinition m_aspectDefinition;

  /**
   * The associated object.
   * Null for perJVM, but present for the class, target, this, instance or thread deployment models.
   */
  private transient Object m_associatedObject;

  /**
   * Creates a new cross-cutting info instance.
   *
   * @param uuid
   * @param aspectClass
   * @param deploymentModel
   * @param aspectDef
   * @param parameters
   * @param associated      instance (null/class/instance/thread)
   */
  public AspectContext(final String uuid,
                       final Class aspectClass,
                       final String name,
                       final DeploymentModel deploymentModel,
                       final AspectDefinition aspectDef,
                       final Map parameters,
                       final Object associated) {
    m_uuid = uuid;
    m_aspectClassRef = new WeakReference(aspectClass);
    m_name = name;
    m_qName = aspectDef.getQualifiedName();
    m_deploymentModel = deploymentModel;
    m_aspectDefinition = aspectDef;
    if (parameters != null) {
      m_parameters = parameters;
    }
    m_associatedObject = associated;
  }

  /**
   * Returns the UUID for the system.
   *
   * @return the UUID for the system
   */
  public String getUuid() {
    return m_uuid;
  }

  /**
   * Returns the name of the aspect.
   *
   * @return the name of the aspect
   */
  public String getName() {
    return m_name;
  }

  /**
   * Returns the deployment model.
   *
   * @return the deployment model
   */
  public DeploymentModel getDeploymentModel() {
    return m_deploymentModel;
  }

  /**
   * Returns the cross-cuttable class.
   *
   * @return the cross-cuttable class
   */
  public Class getAspectClass() {
    return (Class) m_aspectClassRef.get();
  }

  /**
   * Returns the aspect definition.
   * <p/>
   * Will return null after deserialization.
   *
   * @return the aspect definition
   */
  public AspectDefinition getAspectDefinition() {
    return m_aspectDefinition;
  }

  /**
   * Sets a parameter.
   *
   * @param name  the name of the parameter
   * @param value the value of the parameter
   */
  public void setParameter(final String name, final String value) {
    m_parameters.put(name, value);
  }

  /**
   * Returns the value of a parameter.
   *
   * @param name the name of the parameter
   * @return the value of the parameter or null if not specified
   */
  public String getParameter(final String name) {
    return (String) m_parameters.get(name);
  }

  /**
   * Adds metadata.
   *
   * @param key   the key
   * @param value the value
   */
  public void addMetaData(final Object key, final Object value) {
    m_metaData.put(key, value);
  }

  /**
   * Returns the metadata for a specific key.
   *
   * @param key the key
   * @return the value
   */
  public Object getMetaData(final Object key) {
    return m_metaData.get(key);
  }

  /**
   * Returns the associated object with the aspect beeing instantiated. This depend on the aspect deployment model.
   * It can be a null/class/instance/thread.
   *
   * @return the associated object
   */
  public Object getAssociatedObject() {
    return m_associatedObject;
  }

  /**
   * Provides custom deserialization.
   *
   * @param stream the object input stream containing the serialized object
   * @throws Exception in case of failure
   */
  private void readObject(final ObjectInputStream stream) throws Exception {
    ObjectInputStream.GetField fields = stream.readFields();
    m_uuid = (String) fields.get("m_uuid", null);
    m_name = (String) fields.get("m_name", null);
    m_qName = (String) fields.get("m_qName", null);
    Class aspectClass = Class.forName(m_name);
    m_aspectClassRef = new WeakReference(aspectClass);
    m_deploymentModel = (DeploymentModel) fields.get("m_deploymentModel", DeploymentModel.PER_JVM);
    m_parameters = (Map) fields.get("m_parameters", new HashMap());
    m_metaData = (Map) fields.get("m_metaData", new HashMap());

    //TODO aspectDef from m_qName
  }
}