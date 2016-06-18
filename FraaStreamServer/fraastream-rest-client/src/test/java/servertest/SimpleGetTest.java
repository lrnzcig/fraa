package servertest;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import fraastream_rest_client.utils.ClientUtils;


public class SimpleGetTest {

	@Test
	public void getAsString() {
		Client client = ClientUtils.getClientNoAuthenticationButJackson();

		/*Response response = client.target("http://localhost:8080/almadraba/webapi").path("data").request()
				.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "kk")
				.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "pass")
				.get();*/

		Response response = client.target("http://localhost:8080/fraastreamserver/webapi").path("data").request()
				.get();

		Assert.assertEquals(200, response.getStatus());
		String pa = response.readEntity(String.class);
		System.out.println(pa);
	}
}
