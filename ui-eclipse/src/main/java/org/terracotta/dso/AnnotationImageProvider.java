/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;

import java.net.URL;

/**
 * A provider of images for our annotations.
 * 
 * @see org.eclipse.ui.texteditor.IAnnotationImageProvider
 */

public class AnnotationImageProvider implements IAnnotationImageProvider {
  private ImageRegistry         m_imageRegistry;

  private static final Object[] DEFAULT_IMAGES = {
    new String[] { "org.terracotta.dso.adaptedTypeAnnotation", "/com/tc/admin/icons/installed_ovr.gif" },
    new String[] { "org.terracotta.dso.adaptedTypeReferenceAnnotation", "/com/tc/admin/icons/blank.gif" },
    new String[] { "org.terracotta.dso.bootJarTypeAnnotation", "/com/tc/admin/icons/blank.gif" },
    new String[] { "org.terracotta.dso.excludedTypeAnnotation", "/com/tc/admin/icons/error_obj.gif" },
    new String[] { "org.terracotta.dso.nameLockedAnnotation", "/com/tc/admin/icons/namelocked_view.gif" },
    new String[] { "org.terracotta.dso.autolockedAnnotation", "/com/tc/admin/icons/autolocked_view.gif" },
    new String[] { "org.terracotta.dso.rootAnnotation", "/com/tc/admin/icons/hierarchicalLayout.gif" },
    new String[] { "org.terracotta.dso.distributedMethodAnnotation", "/com/tc/admin/icons/jspbrkpt_obj.gif" },
    new String[] { "org.terracotta.dso.transientFieldAnnotation", "/com/tc/admin/icons/transient.gif" }, };

  public AnnotationImageProvider() {
    m_imageRegistry = new ImageRegistry(Display.getDefault());

    String[] mapping;
    for (int i = 0; i < DEFAULT_IMAGES.length; i++) {
      mapping = (String[]) DEFAULT_IMAGES[i];
      addDescriptor(mapping[0], mapping[1]);
    }
  }

  public Image getManagedImage(Annotation annotation) {
    return null;
  }

  public String getImageDescriptorId(Annotation annotation) {
    return annotation.getType();
  }

  public ImageDescriptor getImageDescriptor(String id) {
    return m_imageRegistry.getDescriptor(id);
  }

  public ImageDescriptor addDescriptor(String id, String uri) {
    ImageDescriptor imageDesc = null;
    URL url = AnnotationImageProvider.class.getResource(uri);

    if (url != null) {
      m_imageRegistry.put(id, imageDesc = ImageDescriptor.createFromURL(url));
    }

    return imageDesc;
  }
}
