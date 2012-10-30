/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.meta;


import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.platform.PlatformService;

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
   * 
   * @param platformService
   */
  public static MetaDataDescriptor extractInternalDescriptorFrom(PlatformService platformService, MetaData metaData) {
    if (metaData instanceof MetaDataImpl) {
      return ((MetaDataImpl) metaData).getInternalMetaDataDescriptor();
    } else {
      MetaDataDescriptor mdd = platformService.createMetaDataDescriptor(metaData.getCategory());
      Map<String, Object> map = metaData.getMetaDatas();
      for (Entry<String, Object> entry : map.entrySet()) {
        mdd.set(entry.getKey(), entry.getValue());
      }
      return mdd;
    }
  }

}
