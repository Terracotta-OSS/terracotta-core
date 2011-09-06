/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is a helper class for building criteria to query for statistics in a
 * StatisticsStore. Note that this class is <b>not thread safe</b>.
 */
public class StatisticsRetrievalCriteria {
  private String sessionId = null;
  private Date start = null;
  private Date stop = null;
  private String agentIp = null;
  private String agentDifferentiator = null;
  private Set names = null;
  private Set elements = null;
  
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(final String sessionId) {
    this.sessionId = sessionId;
  }

  public StatisticsRetrievalCriteria sessionId(final String sessionID) {
    setSessionId(sessionID);
    return this;
  }

  public Date getStart() {
    return start;
  }

  public void setStart(final Date start) {
    this.start = start;
  }

  public StatisticsRetrievalCriteria start(final Date startDate) {
    setStart(startDate);
    return this;
  }

  public Date getStop() {
    return stop;
  }

  public void setStop(final Date stop) {
    this.stop = stop;
  }

  public StatisticsRetrievalCriteria stop(final Date stopDate) {
    setStop(stopDate);
    return this;
  }

  public String getAgentIp() {
    return agentIp;
  }

  public void setAgentIp(final String agentip) {
    this.agentIp = agentip;
  }

  public StatisticsRetrievalCriteria agentIp(final String agentIP) {
    setAgentIp(agentIP);
    return this;
  }

  public String getAgentDifferentiator() {
    return agentDifferentiator;
  }

  public void setAgentDifferentiator(final String agentDifferentiator) {
    this.agentDifferentiator = agentDifferentiator;
  }

  public StatisticsRetrievalCriteria agentDifferentiator(final String agentDiff) {
    setAgentDifferentiator(agentDiff);
    return this;
  }

  public Collection getNames() {
    if (null == names) {
      return Collections.EMPTY_SET;
    }

    return Collections.unmodifiableSet(names);
  }

  public StatisticsRetrievalCriteria addName(final String name) {
    if (null == names) {
      names = new LinkedHashSet();
    }
    names.add(name);

    return this;
  }

  public StatisticsRetrievalCriteria setNames(final String[] names) {
    this.names = null;
    
    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        addName(names[i]);
      }
    }

    return this;
  }

  public Collection getElements() {
    if (null == elements) {
      return Collections.EMPTY_SET;
    }

    return Collections.unmodifiableSet(elements);
  }

  public StatisticsRetrievalCriteria addElement(final String element) {
    if (null == elements) {
      elements = new LinkedHashSet();
    }
    elements.add(element);

    return this;
  }

  public StatisticsRetrievalCriteria setElements(final String[] elements) {
    this.elements = null;

    if (elements != null) {
      for (int i = 0; i < elements.length; i++) {
        addName(elements[i]);
      }
    }

    return this;
  }

  public String toString() {
    DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");
    StringBuffer out = new StringBuffer("[");

    out.append("agentip = ");
    out.append(agentIp);
    out.append("; ");

    out.append("sessionId = ");
    out.append(sessionId);
    out.append("; ");

    out.append("start = ");
    if (null == start) {
      out.append(String.valueOf(start));
    } else {
      out.append(format.format(start));
    }
    out.append("; ");

    out.append("stop = ");
    if (null == stop) {
      out.append(String.valueOf(stop));
    } else {
      out.append(format.format(stop));
    }
    out.append("; ");

    out.append("names = [");
    if (names != null && names.size() > 0) {
      boolean first = true;
      for (Iterator it = names.iterator(); it.hasNext(); ) {
        if (first) {
          first = false;
        } else {
          out.append(", ");
        }
        out.append(it.next());
      }
    }
    out.append("]; ");

    out.append("elements = [");
    if (elements != null && elements.size() > 0) {
      boolean first = true;
      for (Iterator it = elements.iterator(); it.hasNext(); ) {
        if (first) {
          first = false;
        } else {
          out.append(", ");
        }
        out.append(it.next());
      }
    }
    out.append("]");

    out.append("]");

    return out.toString();
  }
}
