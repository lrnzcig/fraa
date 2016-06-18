package servertest;

import java.math.BigInteger;
import java.util.Date;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

import fraastream_rest_client.utils.ClientUtils;
import xre.FraaStreamDataUnit;
import xre.FraaStreamHeader;

public class DataUnitPostTest {


	/*
	@Test
	public void postDataUnit() {
		Client client = ClientUtils.getClientNoAuthenticationButJackson();

		FraaStreamDataUnit unit = new FraaStreamDataUnit();
		unit.setIndex(1);
		unit.setX((float) 0.209);
		unit.setY((float) 0.085);
		unit.setZ((float) 0.43);
		Response response = client.target("http://localhost:8080/fraastreamserver/webapi").path("data").request()
				.post(Entity.entity(unit, MediaType.APPLICATION_JSON));

		Assert.assertEquals(200, response.getStatus());
	}
	*/

	@Test
	public void postDataUnitGson() {
		Client client = ClientUtils.getClientNoAuthenticationButJackson();

		// 1. post new header
		FraaStreamHeader header = new FraaStreamHeader();
		header.setCreatedAt(new Date());
		header.setLabel("Sin etiqueta");
		header.setMacAddress("01:23:45:67:89:ab");
		Gson gson = new Gson();
		String obj1 = gson.toJson(header);
		Response response1 = client.target("http://localhost:8080/fraastreamserver/webapi").path("header").request()
				.post(Entity.text(obj1));

		Assert.assertEquals(200, response1.getStatus());
		String r = response1.readEntity(String.class);
		FraaStreamHeader newHeader = gson.fromJson(r, FraaStreamHeader.class);

		
		// 2. post data associated to header id
		FraaStreamDataUnit unit = new FraaStreamDataUnit();
		unit.setHeaderId(newHeader.getId());
		unit.setIndex(BigInteger.valueOf(1));
		unit.setX((float) 0.209);
		unit.setY((float) 0.085);
		unit.setZ((float) 0.43);
		String obj2 = gson.toJson(unit);
		Response response2 = client.target("http://localhost:8080/fraastreamserver/webapi").path("data").request()
				.post(Entity.text(obj2));

		Assert.assertEquals(200, response2.getStatus());
	}

}
