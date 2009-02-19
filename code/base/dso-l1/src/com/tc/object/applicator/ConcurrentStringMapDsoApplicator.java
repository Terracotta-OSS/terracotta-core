package com.tc.object.applicator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.applicator.PartialHashMapApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;

//XXX: This is a rather ugly hack to get around the requirements of tim-concurrent-collections.
public class ConcurrentStringMapDsoApplicator extends PartialHashMapApplicator {
  private static final String CSM_DSO_CLASSNAME = "org.terracotta.modules.concurrent.collections.ConcurrentStringMapDsoInstrumented";
  private static final String DSO_LOCK_TYPE_FIELDNAME = "dsoLockType";
  
  public ConcurrentStringMapDsoApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  @Override
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    writer.addPhysicalAction(DSO_LOCK_TYPE_FIELDNAME, getDsoLockType(pojo));
    super.dehydrate(objectManager, tcObject, writer, pojo);
  }

  @Override
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) {
    try {
      DNACursor cursor = dna.getCursor();
      Assert.assertTrue("DNA contains action", cursor.next());
      PhysicalAction physicalAction = cursor.getPhysicalAction();

      /*
       * This trickery is necessary to avoid getting an instance of class loaded by the wrong classloader.
       * Otherwise we get ClassCastExceptions when we try and store the result in the ConcurrentStringMap
       * instance.
       */
      Class csmClazz = objectManager.getClassFor(CSM_DSO_CLASSNAME, dna.getDefiningLoaderDescription());
      Constructor cons = csmClazz.getDeclaredConstructor(Integer.TYPE);
      cons.setAccessible(true);
      return cons.newInstance(physicalAction.getObject());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Integer getDsoLockType(Object pojo) {
    try {
      Field f = pojo.getClass().getDeclaredField(DSO_LOCK_TYPE_FIELDNAME);
      f.setAccessible(true);
      return Integer.valueOf(f.getInt(pojo));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
