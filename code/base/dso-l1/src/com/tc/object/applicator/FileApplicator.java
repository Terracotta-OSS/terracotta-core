/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.exception.TCRuntimeException;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.util.Assert;
import com.tc.util.FieldUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class FileApplicator extends PhysicalApplicator {
  private final static String FILE_SEPARATOR_FIELD = "File.fileSeparator";
  private final static String PATH_FIELD           = "path";
  
  public FileApplicator(TCClass clazz, DNAEncoding encoding) {
    super(clazz, encoding);
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    String fieldName;
    Object fieldValue;
    boolean remoteFileSeparatorObtained = false;

    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      Assert.eval(a.isTruePhysical());
      fieldName = a.getFieldName();
      fieldValue = a.getObject();
      if (FILE_SEPARATOR_FIELD.equals(fieldName)) {
        replaceFileSeparator(po, fieldValue);
        remoteFileSeparatorObtained = true;
      } else {
        tcObject.setValue(fieldName, fieldValue);
      }
    }
    Assert.assertTrue(remoteFileSeparatorObtained);
  }

  private void replaceFileSeparator(Object po, Object fieldValue) {
    String remoteFileSeparator = (String) fieldValue;
    if (!remoteFileSeparator.equals(File.separator)) {
      try {
        Field pathField = po.getClass().getDeclaredField(PATH_FIELD);
        pathField.setAccessible(true);
        String path = (String) pathField.get(po);
        path = path.replace(remoteFileSeparator.charAt(0), File.separatorChar);
        FieldUtils.tcSet(po, path, pathField);
      } catch (SecurityException e) {
        throw new TCRuntimeException(e);
      } catch (NoSuchFieldException e) {
        throw new TCRuntimeException(e);
      } catch (IllegalArgumentException e) {
        throw new TCRuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    super.dehydrate(objectManager, tcObject, writer, pojo);

    String fieldName = FILE_SEPARATOR_FIELD;
    String fieldValue = File.separator;
    writer.addPhysicalAction(fieldName, fieldValue, false);
  }
}
