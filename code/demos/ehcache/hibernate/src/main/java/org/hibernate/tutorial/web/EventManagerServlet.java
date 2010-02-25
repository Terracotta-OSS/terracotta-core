package org.hibernate.tutorial.web;

import org.hibernate.Criteria;
import org.hibernate.tutorial.domain.Event;
import org.hibernate.tutorial.util.HibernateUtil;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EventManagerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy");

    try {
      // Begin unit of work
      HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();

      // Write HTML header
      PrintWriter out = response.getWriter();
      out.println("<html><head><title>Event Manager</title></head><body>");

      // Handle actions
      if ("store".equals(request.getParameter("action"))) {
        String eventTitle = request.getParameter("eventTitle");
        String eventDate = request.getParameter("eventDate");

        if ("".equals(eventTitle) || "".equals(eventDate)) {
          out.println("<b><i>Please enter event title and date.</i></b>");
        } else {
          createAndStoreEvent(eventTitle, dateFormatter.parse(eventDate));
          out.println("<b><i>Added event.</i></b>");
        }
      }

      // Print page
      printEventForm(out);
      listEvents(out, dateFormatter);
      printSummary(request, out);

      // Write HTML footer
      out.println("</body></html>");
      out.flush();
      out.close();
      // End unit of work
      HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
    } catch (Exception ex) {
      HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
      if (ServletException.class.isInstance(ex)) {
        throw (ServletException) ex;
      } else {
        throw new ServletException(ex);
      }
    }
  }

  protected void createAndStoreEvent(String title, Date theDate) {
    Event theEvent = new Event();
    theEvent.setTitle(title);
    theEvent.setDate(theDate);
    HibernateUtil.getSessionFactory().getCurrentSession().save(theEvent);
  }

  private void printEventForm(PrintWriter out) {
    out.println("<h2>Add new event:</h2>");
    out.println("<form>");
    out.println("Title: <input name='eventTitle' length='50'/><br/>");
    out.println("Date (e.g. 24.12.2009): <input name='eventDate' length='10'/><br/>");
    out.println("<input type='submit' name='action' value='store'/>");
    out.println("</form>");
  }

  private void listEvents(PrintWriter out, SimpleDateFormat dateFormatter) {
    Criteria crit = HibernateUtil.getSessionFactory().getCurrentSession().createCriteria(Event.class);
    crit.setCacheable(true);
    List result = crit.list();
    if (result.size() > 0) {
      out.println("<h2>Events in database:</h2>");
      out.println("<table border='1'>");
      out.println("<tr>");
      out.println("<th>Event title</th>");
      out.println("<th>Event date</th>");
      out.println("</tr>");
      Iterator it = result.iterator();
      while (it.hasNext()) {
        Event event = (Event) it.next();
        out.println("<tr>");
        out.println("<td>" + event.getTitle() + "</td>");
        out.println("<td>" + dateFormatter.format(event.getDate()) + "</td>");
        out.println("</tr>");
      }
      out.println("</table>");
    }
  }

  private void printSummary(HttpServletRequest request, PrintWriter out) {
    String server1 = "9081";
    String server2 = "9082";
    String currentServer = request.getRequestURL().indexOf(server1) == -1 ? server2 : server1;
    String otherServer = currentServer == server1 ? server2 : server1;
    String serverColor = currentServer == server1 ? "goldenrod" : "darkseagreen";
    String summaryMsg = "<p>The Events sample demonstrates a standard Hibernate demo configured to use clustered Terracotta Ehcache as the 2nd-level cache.<p>With the Terracotta Developer Console, you can monitor the cache activity and dynamically change it's eviction configuration; see the <i><b>Hibernate</b></i> tab under <i><b>My application</b></i>.";
    String rowStart = "<tr><td style='text-align: right'><nobr>";
    String rowMiddle = "</nobr></td><td><nobr><b>";
    String rowEnd = "</b></nobr></td></tr>";

    out.println("<hr style='margin-top: 10px'>");
    out
        .println("<div style='background: "
                 + serverColor
                 + "; font-size: 85%; font-family: sans-serif; border: 3px solid gray; padding: 15px; margin: 50px 80px'>");
    out.println("<table style='font-size: 100%'><tr style='vertical-align: top'><td><table style='font-size: 100%'>");
    out.println(rowStart + "Current server:" + rowMiddle + currentServer + rowEnd);
    out.println(rowStart + "Go to:" + rowMiddle + "<a href='http://" + request.getServerName() + ":" + otherServer
                + request.getContextPath() + "'><b>Server " + otherServer + "</b></a>" + rowEnd
                + "</table></td><td style='padding-left: 15px'>");
    out.println(summaryMsg);
    out.println("</td></tr></table></div>");
  }
}