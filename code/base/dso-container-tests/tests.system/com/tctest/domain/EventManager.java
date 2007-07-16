package com.tctest.domain;

import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class EventManager {
  
  public List listEmailsOfEvent(Long eventId) {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    
    List emailList = new ArrayList();
    Event event = (Event) session.load(Event.class, eventId);
    for (Iterator it = event.getParticipants().iterator(); it.hasNext(); ) {
      Person person = (Person)it.next();
      emailList.addAll(person.getEmailAddresses());
    }
    
    session.getTransaction().commit();    
    return emailList;
  }
  
  public Long createAndStoreEvent(String title, Date theDate) {

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();

    session.beginTransaction();

    Event theEvent = new Event();
    theEvent.setTitle(title);
    theEvent.setDate(theDate);

    Long eventId = (Long) session.save(theEvent);

    session.getTransaction().commit();
    return eventId;
  }

  public Long createAndStorePerson(String firstName, String lastName) {

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();

    session.beginTransaction();

    Person person = new Person();
    person.setFirstname(firstName);
    person.setLastname(lastName);

    Long personId = (Long) session.save(person);

    session.getTransaction().commit();
    return personId;
  }

  public List listEvents() {

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();

    session.beginTransaction();

    List result = session.createQuery("from Event").list();    

    session.getTransaction().commit();

    return result;
  }

  public void addPersonToEvent(Long personId, Long eventId) {

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();

    Person aPerson = (Person) session.load(Person.class, personId);
    Event anEvent = (Event) session.load(Event.class, eventId);

    aPerson.getEvents().add(anEvent);

    session.getTransaction().commit();
  }

  public void addEmailToPerson(Long personId, String emailAddress) {

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();

    Person aPerson = (Person) session.load(Person.class, personId);

    // The getEmailAddresses() might trigger a lazy load of the collection
    aPerson.getEmailAddresses().add(emailAddress);

    session.getTransaction().commit();
  }
  
  public void addPhoneNumberToPerson(Long personId, PhoneNumber pN) {

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();

    Person aPerson = (Person) session.load(Person.class, personId);
    pN.setPersonId(personId.longValue());
    aPerson.getPhoneNumbers().add(pN);    

    session.getTransaction().commit();
  }
}
