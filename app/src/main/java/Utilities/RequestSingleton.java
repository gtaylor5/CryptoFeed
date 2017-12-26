package Utilities;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Gerard on 12/12/2017.
 */

public class RequestSingleton {
    public static RequestSingleton instance;
    public RequestQueue requestQueue;
    public static Context con;

    private RequestSingleton(Context context){
        con = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized RequestSingleton getInstance(Context context){
        if (instance == null){
            instance = new RequestSingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if(requestQueue == null) {
            requestQueue = Volley.newRequestQueue(con.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> request){
        getRequestQueue().add(request);
    }

}
