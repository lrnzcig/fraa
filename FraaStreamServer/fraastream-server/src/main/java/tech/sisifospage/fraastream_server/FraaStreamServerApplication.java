package tech.sisifospage.fraastream_server;

import javax.ws.rs.Priorities;

import org.glassfish.jersey.server.ResourceConfig;



public class FraaStreamServerApplication extends ResourceConfig {

	public FraaStreamServerApplication() {
		super();
		
		register(FraaStreamServerAuthenticationRequestFilter.class, Priorities.AUTHENTICATION);
		register(FraaStreamServerExceptionMapper.class);
		register(FraaStreamServerContainerResponseFilter.class);
		
		packages("tech.sisifospage.fraastream_server.resources");
	}


}
