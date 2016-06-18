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
import tech.sisifospage.fraastream_server.hbm.Header;
import xre.FraaStreamHeader;

@Path("header")
public class HandleHeader {
	
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String getNewHeader(@Context final SecurityContext securityContext, 
    		@Context final ContainerRequestContext requestContext,
    		final String headerS) {
    	
    	Gson gson = new Gson();
    	FraaStreamHeader header = gson.fromJson(headerS, FraaStreamHeader.class);

    	Header data = new Header(null, header.getCreatedAt(), header.getMacAddress(), header.getLabel());
    	Session session = FraaStreamServerContextListener.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		session.save(data);
		session.getTransaction().commit();
    	
		header.setId(data.getId());
    	return gson.toJson(header);
    }

}
