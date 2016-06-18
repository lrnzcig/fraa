package tech.sisifospage.fraastream;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by lorenzorubio on 29/5/16.
 * https://developer.android.com/training/volley/request-custom.html
 * extiende la clase de la respuesta !
 * hacer la entrada genérica
 */
public class GsonRequest extends Request<String> {
    private final Gson gson = new Gson();
    private final Map<String, String> headers;
    private final Response.Listener<String> listener;
    private final FraaStreamDataUnit params;

    /**
     * Make a GET request and return a parsed object from JSON.
     *
     * @param method
     * @param url URL of the request to make
     * @param params Input object
     * @param headers Map of request headers
     */
    public GsonRequest(int method, String url, FraaStreamDataUnit params, Map<String, String> headers,
                       Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.params = params;
        this.headers = headers;
        this.listener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        //Log.i(StreamingActivity.TAG, super.getHeaders().toString());
        //Log.i(StreamingActivity.TAG, headers.toString());
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(String response) {
        listener.onResponse(response);
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        if (params != null) {
            String obj = gson.toJson(params);
            Log.i(StreamingActivity.TAG, obj);
            try {
                return obj.getBytes(getParamsEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding not supported: " + getParamsEncoding(), e);
            }
        }
        return null;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }

}
