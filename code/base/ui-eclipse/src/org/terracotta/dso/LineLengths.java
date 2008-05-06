/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

/**
 * Keeps information about a text file's line lengths.  Used by TcPlugin
 * to help create SAXMarkers when there are errors on the config document.
 * This object is serialized to the plugin's private working area, along
 * with the config document and config object, after successfully parsing
 * the document into the object.
 * 
 * Eclipse documents provide this sort of information but we can't rely
 * on the config document being loaded into an editor.
 * 
 * @see TcPlugin.loadConfiguration
 * @see TcPlugin.handleXmlErrors
 */

public class LineLengths implements java.io.Serializable {
  private int[] m_lines;
  
  public LineLengths() {
    super();
  }
  
  public LineLengths(IFile file)
    throws ConcurrentModificationException,
           IOException,
           CoreException
  {
    this();
    setFile(file);
  }

  public LineLengths(Reader reader)
    throws ConcurrentModificationException,
           IOException
  {
    this();
    setReader(reader);
  }
  
  public void setFile(IFile file)
    throws ConcurrentModificationException,
           IOException,
           CoreException
  {
    initLines(new InputStreamReader(file.getContents()));
  }

  public void setReader(Reader reader)
    throws ConcurrentModificationException,
           IOException
  {
    initLines(reader);
  }
  
  private void initLines(Reader reader)
    throws ConcurrentModificationException,
           IOException
  {
    ArrayList<String> list = new ArrayList<String>();
    
    try {
      BufferedReader br = new BufferedReader(reader);
      String         s;
      
      while((s = readLine(br)) != null) {
        list.add(s);
      }
    } catch(IOException e) {
      IOUtils.closeQuietly(reader);
      throw e;
    } catch(ConcurrentModificationException e) {
      IOUtils.closeQuietly(reader);
      throw e;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    
    int size = list.size();
    m_lines = new int[size];
    for(int i = 0; i < size; i++) {
      m_lines[i] = list.get(i).length();
    }
  }

  /**
   * Reads a complete line of characters from the reader, preserving the
   * various forms of line-separator that exist.
   */
  private String readLine(BufferedReader br) throws IOException {
    StringBuffer sb     = new StringBuffer();
    boolean      haveCR = false;
    boolean      done   = false;
    int          i;
    char         c;
    
    while((i = br.read()) != -1) {
      c = (char)i;
      
      if(haveCR) {
        switch(c) {
          case '\n':
            sb.append(c);
            done = true;
            break;
          default:
            br.reset();
            done = true;
            break;
        }
      }
      else {
        sb.append(c);
        switch(c) {
          case '\n':
            done = true;
            break;
          case '\r':
            br.mark(1);
            haveCR = true;
        }
      }
      
      if(done) {
        break;
      }
    }

    return sb.length() > 0 ? sb.toString() : null;
  }
  
  public int lineSize(int line) {
    if(line < 0 || line > m_lines.length) {
      return 0;
    }
    return m_lines[line];
  }
  
  public int offset(int line) {
    int result = 0;
    
    if(line == 0) return 0;
    
    for(int i = 0; i < line; i++) {
      result += m_lines[i];
    }

    return result;
  }
  
  public int offset(int line, int col) {
    if(line < 0 || line > m_lines.length) {
      return 0;
    }
    int result = offset(line);
    if(col > 0) {
      result += col;
    }
    return result;
  }
}
