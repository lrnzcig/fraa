package servertest;

import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

import fraastream_rest_client.utils.ClientUtils;
import xre.FraaStreamData;
import xre.FraaStreamDataUnit;
import xre.FraaStreamHeader;

public class DataPostTest {

	@Test
	public void postDataGson() {
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
		FraaStreamData data = new FraaStreamData();
		data.setHeaderId(newHeader.getId());
		UUID requestId = UUID.randomUUID();
		data.setRequestId(requestId);
		FraaStreamDataUnit unit = new FraaStreamDataUnit();
		unit.setIndex(BigInteger.valueOf(1));
		unit.setX((float) 0.209);
		unit.setY((float) 0.085);
		unit.setZ((float) 0.43);
		data.addDataUnit(unit);
		unit = new FraaStreamDataUnit();
		unit.setIndex(BigInteger.valueOf(2));
		unit.setX((float) 0.207);
		unit.setY((float) 0.088);
		unit.setZ((float) 0.439);
		data.addDataUnit(unit);
		String obj2 = gson.toJson(data);
		Response response2 = client.target("http://localhost:8080/fraastreamserver/webapi").path("data").request()
				.post(Entity.text(obj2));

		Assert.assertEquals(200, response2.getStatus());
		UUID headerResponse = response2.readEntity(UUID.class);
		Assert.assertEquals(data.getRequestId(), headerResponse);
	}

}
