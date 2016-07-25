package tech.sisifospage.fraastream.stream;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lorenzorubio on 29/5/16.
 * http://stackoverflow.com/questions/28207711/how-to-send-post-params-using-volley-in-android
 */
@Deprecated
public class HttpPostStringRequest extends StringRequest {

    private Map<String, String> mParams = new HashMap<>();

    public HttpPostStringRequest(int method, String url,
                                 Response.Listener<String> listener, Response.ErrorListener errorListener, Map<String, String> params) {
        super(method, url, listener, errorListener);
        mParams.putAll(params);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }
}
