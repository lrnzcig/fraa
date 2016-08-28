package tech.sisifospage.fraastream.stream;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import tech.sisifospage.fraastream.StreamingActivity;

/**
 * Created by lorenzorubio on 29/5/16.
 * https://developer.android.com/training/volley/request-custom.html
 */
public class GsonRequest<T, U> extends Request<String> {
    private final Gson gson = new Gson();
    private final Map<String, String> headers;
    private final Response.Listener<U> listener;
    private final T params;
    private final Class responseClass;

    private static final String TAG = "MetaWear.GsonRequest";

    /**
     * GET request and return a parsed object from JSON
     *
     * @param method
     * @param url URL of the request to make
     * @param params Input object of type T
     * @param headers
     * @param listener Listener of the response of type U
     * @param errorListener
     * @param responseClass Needs to be U.class!
     */
    public GsonRequest(int method, String url, T params, Map<String, String> headers,
                       Response.Listener<U> listener, Response.ErrorListener errorListener,
                       Class responseClass) {
        super(method, url, errorListener);
        this.params = params;
        this.headers = headers;
        this.listener = listener;
        this.responseClass = responseClass;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(String response) {
        U r = (U) gson.fromJson(response, responseClass);
        listener.onResponse(r);
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        if (params != null) {
            String obj = gson.toJson(params);
            Log.d(TAG, obj);
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
