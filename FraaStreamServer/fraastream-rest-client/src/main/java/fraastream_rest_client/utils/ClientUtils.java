package fraastream_rest_client.utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;

public class ClientUtils {

	public static Client getClientWithAuthenticationAndJackson() {
		Client client = ClientBuilder.newClient();
		createAndAddAuthenticationFeature(client);
		// Jackson
		client.register(MyObjectMapperProvider.class);
		client.register(JacksonFeature.class);
		return client;
	}
	
	private static void createAndAddAuthenticationFeature(final Client client) {
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().build();
		client.register(feature);
		
	}

	// TODO remove ??
	public static Client getClientNoAuthenticationButJackson() {
		Client client = ClientBuilder.newClient();
		// Jackson
		client.register(MyObjectMapperProvider.class);
		client.register(JacksonFeature.class);
		return client;
	}


}
