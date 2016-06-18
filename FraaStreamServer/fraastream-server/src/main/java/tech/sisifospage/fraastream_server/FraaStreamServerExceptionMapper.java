package tech.sisifospage.fraastream_server;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import tech.sisifospage.fraastream_server.exception.FraaStreamServerAuthenticationException;
import tech.sisifospage.fraastream_server.exception.FraaStreamServerBaseException;


public class FraaStreamServerExceptionMapper implements ExceptionMapper<FraaStreamServerBaseException> {

	@Override
	public Response toResponse(final FraaStreamServerBaseException exception) {
		if (exception instanceof FraaStreamServerAuthenticationException) {
			return Response.status(401).entity(exception.getMessage()).type("text/plain").build();
		}
		return Response.serverError().entity(exception.getMessage()).type("text/plain").build();
	}

}
