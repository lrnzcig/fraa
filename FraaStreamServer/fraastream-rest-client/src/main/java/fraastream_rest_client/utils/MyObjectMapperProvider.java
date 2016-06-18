package fraastream_rest_client.utils;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
public class MyObjectMapperProvider implements ContextResolver<ObjectMapper> {

    final ObjectMapper defaultObjectMapper;
    
    public MyObjectMapperProvider() {
        defaultObjectMapper = createDefaultMapper();
    }
 
	public ObjectMapper getContext(final Class<?> arg0) {
		return defaultObjectMapper;
	}

    private static ObjectMapper createDefaultMapper() {
        final ObjectMapper result = new ObjectMapper();
        // avoid a failure when a property is not known
        // (e.g. json tweet contains much more fields than the ones defined for com.sisifo.twitter_model.tweet.Tweet)
        result.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return result;
    }
}
