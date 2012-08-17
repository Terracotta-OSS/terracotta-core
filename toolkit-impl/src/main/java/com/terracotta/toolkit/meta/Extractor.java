/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.meta;

import org.terracotta.toolkit.internal.meta.MetaData;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.metadata.MetaDataDescriptor;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class for extracting internal info from public interface {@link MetaData}
 */
public class Extractor {

  private Extractor() {
    //
  }

  /**
   * Extract internal {@link MetaDataDescriptor} from {@link MetaData}
   */
  public static MetaDataDescriptor extractInternalDescriptorFrom(MetaData metaData) {
    if (metaData instanceof MetaDataImpl) {
      return ((MetaDataImpl) metaData).getInternalMetaDataDescriptor();
    } else {
      MetaDataDescriptor mdd = ManagerUtil.createMetaDataDescriptor(metaData.getCategory());
      Map<String, Object> map = metaData.getMetaDatas();
      for (Entry<String, Object> entry : map.entrySet()) {
        mdd.set(entry.getKey(), entry.getValue());
      }
      return mdd;
    }
  }

}
