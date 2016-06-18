package servertest;

import java.util.Date;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

import fraastream_rest_client.utils.ClientUtils;
import xre.FraaStreamHeader;

public class HeaderTest {



	@Test
	public void postDataUnitGson() {
		Client client = ClientUtils.getClientNoAuthenticationButJackson();

		FraaStreamHeader header = new FraaStreamHeader();
		header.setCreatedAt(new Date());
		header.setLabel("Sin etiqueta");
		header.setMacAddress("01:23:45:67:89:ab");
		Gson gson = new Gson();
		String obj = gson.toJson(header);
		Response response = client.target("http://localhost:8080/fraastreamserver/webapi").path("header").request()
				.post(Entity.text(obj));

		Assert.assertEquals(200, response.getStatus());
		String r = response.readEntity(String.class);
		FraaStreamHeader newHeader = gson.fromJson(r, FraaStreamHeader.class);
		System.out.println(newHeader.getId());
	}

}
