package tech.sisifospage.fraastream_server.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.hibernate.Session;

import tech.sisifospage.fraastream_server.FraaStreamServerContextListener;
import tech.sisifospage.fraastream_server.exception.FraaStreamServerAuthenticationException;


@Path("login")
public class Login {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String login(@Context final SecurityContext securityContext) {
    	if (securityContext.getUserPrincipal() == null) {
    		throw new FraaStreamServerAuthenticationException("body method");
    	}

    	Session session = FraaStreamServerContextListener.getSessionFactory().getCurrentSession();
		session.beginTransaction();

    	//DatabaseUtils.loadUserRankExec(session);

		session.disconnect();
		
    	return "not implemented";
    }
}
