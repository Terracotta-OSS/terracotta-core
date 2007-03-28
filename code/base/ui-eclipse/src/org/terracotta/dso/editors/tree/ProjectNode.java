/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import com.tc.admin.common.XTreeNode;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.HashMap;

import javax.swing.ImageIcon;

/**
 * Base type for tree nodes that represent Java project artifacts.
 * 
 * Used by the navigators that support various editor chooser:
 * 
 *   @see org.terracotta.dso.editors.chooser.ClassNavigator
 *   @see org.terracotta.dso.editors.chooser.FieldNavigator
 *   @see org.terracotta.dso.editors.chooser.MethodNavigator
 * 
 * @see CompilationUnitNode
 * @see FieldNode
 * @see JavaProjectModel
 * @see JavaProjectRoot
 * @see MethodNode
 * @see PackageFragmentNode
 * @see TypeNode
 */

public abstract class ProjectNode extends XTreeNode {
  private static HashMap<String, Image>   m_imageMap;
  private static JavaElementLabelProvider m_javaLabelProvider;

  static {
    m_imageMap          = new HashMap<String, Image>();
    m_javaLabelProvider = new JavaElementLabelProvider();
  }
  
  public ProjectNode() {
    super();
  }
  
  public ProjectNode(Object obj) {
    super(obj);
    setIcon(new ImageIcon(getAWTImage(obj)));
  }

  public Image getAWTImage(Object adaptable) {
    org.eclipse.swt.graphics.Image swtImage = getImage(adaptable);
    Image                          awtImage = null;
    
    if(swtImage != null) {
      String swtHandle = swtImage.toString();
      
      if((awtImage = m_imageMap.get(swtHandle)) == null) {
        m_imageMap.put(swtHandle, awtImage = swt2Swing(swtImage));
      }
    }
    
    return awtImage;
  }
  
  private org.eclipse.swt.graphics.Image getImage(Object adaptable) {
    if(adaptable instanceof IRuntimeClasspathEntry) {
      return getImage((IRuntimeClasspathEntry)adaptable);
    }
    else {
      return m_javaLabelProvider.getImage(adaptable);
    }
  }

  private org.eclipse.swt.graphics.Image getImage(IRuntimeClasspathEntry entry) {
    IResource resource = entry.getResource();
    
    switch(entry.getType()) {
      case IRuntimeClasspathEntry.PROJECT:
        //TODO what if project not loaded?
        IJavaElement proj = JavaCore.create(resource);
        return m_javaLabelProvider.getImage(proj);
      case IRuntimeClasspathEntry.ARCHIVE:
        if(resource instanceof IContainer) {
          return m_javaLabelProvider.getImage(resource);
        }
        boolean external = resource == null;
        boolean source = (entry.getSourceAttachmentPath() != null && !Path.EMPTY.equals(entry.getSourceAttachmentPath()));
        String key = null;
        if(external) {
          IPath path = entry.getPath();
          if(path != null) {
            //TODO: check for invalid paths and change image
            File file = path.toFile();
            if(file.exists() && file.isDirectory()) {
              key = ISharedImages.IMG_OBJS_PACKFRAG_ROOT;
            } else if(source) {
              key = ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE;
            } else {
              key = ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE;
            } 
          }
        } else {
          if (source) {
            key = ISharedImages.IMG_OBJS_JAR_WITH_SOURCE;
          } else {
            key = ISharedImages.IMG_OBJS_JAR;
          }
        }
        return JavaUI.getSharedImages().getImage(key);
      case IRuntimeClasspathEntry.VARIABLE:
        return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_ENV_VAR);       
      case IRuntimeClasspathEntry.CONTAINER:
        return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
      case IRuntimeClasspathEntry.OTHER:
        IRuntimeClasspathEntry delegate = entry;
        
        org.eclipse.swt.graphics.Image image = m_javaLabelProvider.getImage(delegate);
        if(image != null) {
          return image;
        }
        if(resource == null) {
          return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
        }
        return m_javaLabelProvider.getImage(resource);
    } 
    
    return null;
  }
  
  private Image swt2Swing(org.eclipse.swt.graphics.Image swtImage) {
    return convertToAWT(swtImage.getImageData());
  }
  
  /**
   * I found this on the web.  It didn't work but I fixed it, at least for
   * direct color model.
   */
  static BufferedImage convertToAWT(ImageData data) {
    ColorModel  colorModel = null;
    PaletteData palette    = data.palette;
    
    if(palette.isDirect) {
      ImageData maskData = data.getTransparencyMask();
      int       maskPix;
      
      colorModel = new DirectColorModel(32, 0x000000ff,
                                            0x0000ff00,
                                            0x00ff0000,
                                            0xff000000);
      
      BufferedImage bufferedImage =
        new BufferedImage(colorModel,
                          colorModel.createCompatibleWritableRaster(data.width, data.height),
                          false, null);
      
      WritableRaster raster     = bufferedImage.getRaster();
      int[]          pixelArray = new int[4];

      for(int y = 0; y < data.height; y++) {
        for(int x = 0; x < data.width; x++) {
          int pixel = data.getPixel(x, y);
          RGB rgb   = palette.getRGB(pixel);
          
          pixelArray[0] = rgb.red;
          pixelArray[1] = rgb.green;
          pixelArray[2] = rgb.blue;
          
          maskPix       = maskData.getPixel(x,y);
          pixelArray[3] = (maskPix == 1) ? 0xff : 0;
          
          raster.setPixels(x, y, 1, 1, pixelArray);
        }
      }
      
      return bufferedImage;
    }
    else {
      RGB[]  rgbs  = palette.getRGBs();
      byte[] red   = new byte[rgbs.length];
      byte[] green = new byte[rgbs.length];
      byte[] blue  = new byte[rgbs.length];
      
      for(int i = 0; i < rgbs.length; i++) {
        RGB rgb = rgbs[i];
        
        red[i]   = (byte)rgb.red;
        green[i] = (byte)rgb.green;
        blue[i]  = (byte)rgb.blue;
      }

      colorModel = (data.transparentPixel != -1)  ?
          new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel) :
          new IndexColorModel(data.depth, rgbs.length, red, green, blue);

      BufferedImage bufferedImage =
        new BufferedImage(colorModel,
                          colorModel.createCompatibleWritableRaster(data.width, data.height),
                          false, null);
      
      WritableRaster raster     = bufferedImage.getRaster();
      int[]          pixelArray = new int[1];
      
      for(int y = 0; y < data.height; y++) {
        for(int x = 0; x < data.width; x++) {
          int pixel = data.getPixel(x, y);
          
          pixelArray[0] = pixel;
          
          raster.setPixel(x, y, pixelArray);
        }
      }
      
      return bufferedImage;
    }
  }
  
  protected void setChildAt(ProjectNode child, int index) {
    children.setElementAt(child, index);
    child.setParent(this);
  }
}
