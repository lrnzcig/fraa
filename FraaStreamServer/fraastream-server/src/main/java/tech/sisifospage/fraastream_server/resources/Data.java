package tech.sisifospage.fraastream_server.resources;

import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.hibernate.Criteria;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.criterion.Property;

import com.google.gson.Gson;

import tech.sisifospage.fraastream_server.FraaStreamServerContextListener;
import tech.sisifospage.fraastream_server.hbm.AccData;
import tech.sisifospage.fraastream_server.hbm.AccDataId;
import xre.FraaStreamData;
import xre.FraaStreamDataUnit;


@Path("data")
public class Data {

   
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public UUID postStreamData(@Context final SecurityContext securityContext, 
    		@Context final ContainerRequestContext requestContext,
    		final String dataS) {
    	
    	// TODO uncomment when security is in place
    	//if (securityContext.getUserPrincipal() == null) {
    	//	throw new FraaStreamServerAuthenticationException("body method");
    	//}
    	
    	Gson gson = new Gson();
    	FraaStreamData data = gson.fromJson(dataS, FraaStreamData.class);
    	
    	
    	Session session = FraaStreamServerContextListener.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		
		for (FraaStreamDataUnit unit : data.getDataUnits()) {
	    	AccData item = new AccData(new AccDataId(data.getHeaderId(), unit.getIndex()), unit.getX(), unit.getY(), unit.getZ());
	    	//System.out.println(unit.getIndex() + ": (" + unit.getX() + ", " + unit.getY() + ", " + unit.getZ() + ")");
	    	try {
	    		session.save(item);
	    	} catch (NonUniqueObjectException e) {
	    		// check values are the same
	    		Criteria criteria = session.createCriteria(AccData.class)
	    				.add(Property.forName("id.headerId").eq(data.getHeaderId()))
	    				.add(Property.forName("id.id").eq(unit.getIndex()));
	    		AccData result = (AccData) criteria.list().get(0);
	    		if (result.getX() != unit.getX()
	    				|| result.getY() != unit.getY()
	    				|| result.getZ() != unit.getZ()) {
		    		System.out.println("Non unique, and values different than previously stored:" + data.getHeaderId()  + ", " + unit.getIndex());
	    			throw(e);
	    		}
	    	}
		}
		session.getTransaction().commit();

    	
    	System.out.println(requestContext.getHeaders().toString());
    	return data.getRequestId();
    }


}
