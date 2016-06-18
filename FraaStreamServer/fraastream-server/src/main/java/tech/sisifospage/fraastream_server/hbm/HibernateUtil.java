package tech.sisifospage.fraastream_server.hbm;

import java.net.URL;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
    	
    	URL r1 = HibernateUtil.class.getResource("/hibernate.postgres.cfg.xml");
    	Configuration c = new Configuration().configure(r1);
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            return c.buildSessionFactory();
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
}
