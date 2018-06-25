package Utilities;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

/**
 * Created by Gerard on 12/15/2017.
 */

public class Requests {

    public static StringRequest getStringRequest(String url, final RequestFinishedListener listener){
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> listener.onRequestFinished(response), error -> {
                });
        request.setShouldCache(false);
        return request;
    }

    public interface RequestFinishedListener {
        void onRequestFinished(String response);
    }


}
