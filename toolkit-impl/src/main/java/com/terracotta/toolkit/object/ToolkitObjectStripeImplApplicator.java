/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.SupportedConfigurationType;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.config.UnclusteredConfiguration;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

public class ToolkitObjectStripeImplApplicator extends BaseApplicator {

  public ToolkitObjectStripeImplApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    ToolkitObjectStripeImpl cos = (ToolkitObjectStripeImpl) pojo;
    for (TCToolkitObject tco : cos.internalGetComponents()) {
      addTo.addAnonymousReference(tco);
    }
    return addTo;
  }

  @Override
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    ToolkitObjectStripeImpl clusteredObjectStripe = (ToolkitObjectStripeImpl) pojo;
    TCToolkitObject[] components = clusteredObjectStripe.internalGetComponents();
    Object[] dehydratableArray = new Object[components.length];
    for (int i = 0; i < dehydratableArray.length; i++) {
      dehydratableArray[i] = getDehydratableObject(components[i], objectManager);
    }
    writer.addEntireArray(dehydratableArray);

    Configuration configuration = clusteredObjectStripe.getConfiguration();
    for (String key : configuration.getKeys()) {
      final Object addValue = getDehydratableObject(configuration.getObjectOrNull(key), objectManager);
      if (addValue == null) {
        continue;
      }
      writer.addPhysicalAction(key, addValue);
    }
  }

  @Override
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna, PlatformService platformService)
      throws ClassNotFoundException {
    UnclusteredConfiguration config = new UnclusteredConfiguration();
    TCToolkitObject[] components = null;

    DNACursor cursor = dna.getCursor();
    try {

      while (cursor.next(encoding)) {
        PhysicalAction pa = cursor.getPhysicalAction();
        if (pa.isEntireArray()) {
          Object[] array = (Object[]) pa.getObject();
          components = new TCToolkitObject[array.length];
          for (int i = 0; i < array.length; i++) {
            if (!(array[i] instanceof ObjectID)) { throw new AssertionError(
                                                                            "ClusteredObjectStripe should fault in only ObjectID's for components - "
                                                                                + array[i]); }
            try {
              components[i] = (TCToolkitObject) objectManager.lookupObject((ObjectID) array[i]);
            } catch (AbortedOperationException e) {
              throw new TCRuntimeException(e);
            }
          }
        } else {
          String key = pa.getFieldName();
          Serializable value = (Serializable) pa.getObject();
          if (!SupportedConfigurationType.isTypeSupported(value)) {
            //
            throw new AssertionError("Faulted in unsupported value type: key: " + key + ", value: " + value);
          }
          config.internalSetConfigMapping(key, value);
        }
      }

      if (components == null) { throw new AssertionError("Didn't fault in components"); }

      return new ToolkitObjectStripeImpl(config, components);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException {
    final DNACursor cursor = dna.getCursor();

    if (!dna.isDelta()) {
      if (cursor.next(this.encoding)) { throw new AssertionError("Shouldn't have any more changes in delta"); }
    } else {
      while (cursor.next(this.encoding)) {
        // when delta changes, only logical actions should get broadcasted
        final LogicalAction action = cursor.getLogicalAction();
        final LogicalOperation method = action.getLogicalOperation();
        final Object[] params = action.getParameters();
        apply(objectManager, pojo, method, params);
      }
    }

  }

  private void apply(final ClientObjectManager objectManager, final Object po, final LogicalOperation method, final Object[] params) {
    final ToolkitObjectStripeImpl clusteredObjectStripe = (ToolkitObjectStripeImpl) po;
    switch (method) {
      case PUT:
        Object k = params[0];
        Serializable v = (Serializable) params[1];
        SupportedConfigurationType type = SupportedConfigurationType.getTypeForObject(k);
        if (type != SupportedConfigurationType.STRING) {
          //
          throw new AssertionError("Got apply of unsupported key type: " + k);
        }
        if (!SupportedConfigurationType.isTypeSupported(v)) {
          //
          throw new AssertionError("Received apply of unsupported value type in config: " + v);
        }
        clusteredObjectStripe.internalSetMapping((String) k, v);
        break;
      default:
        throw new AssertionError("invalid action:" + method + ", params: " + Arrays.asList(params));
    }
  }

}
