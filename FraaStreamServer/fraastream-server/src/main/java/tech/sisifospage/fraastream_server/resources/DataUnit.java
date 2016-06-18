package tech.sisifospage.fraastream_server.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.hibernate.Session;

import com.google.gson.Gson;

import tech.sisifospage.fraastream_server.FraaStreamServerContextListener;
import tech.sisifospage.fraastream_server.hbm.AccData;
import tech.sisifospage.fraastream_server.hbm.AccDataId;
import xre.FraaStreamDataUnit;


@Path("data")
public class DataUnit {

   
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String postStreamDataUnit(@Context final SecurityContext securityContext, 
    		@Context final ContainerRequestContext requestContext,
    		final String unitS) {
    	
    	// TODO uncomment when security is in place
    	//if (securityContext.getUserPrincipal() == null) {
    	//	throw new FraaStreamServerAuthenticationException("body method");
    	//}
    	
    	Gson gson = new Gson();
    	FraaStreamDataUnit unit = gson.fromJson(unitS, FraaStreamDataUnit.class);
    	
    	AccData data = new AccData(new AccDataId(unit.getHeaderId(), unit.getIndex()), unit.getX(), unit.getY(), unit.getZ());
    	
    	Session session = FraaStreamServerContextListener.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		session.save(data);
		session.getTransaction().commit();

    	
    	System.out.println(requestContext.getHeaders().toString());
    	System.out.println(unit.getIndex() + ": (" + unit.getX() + ", " + unit.getY() + ", " + unit.getZ() + ")");
    	return "allright";
    }


}
