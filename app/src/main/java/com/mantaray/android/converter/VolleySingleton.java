package com.mantaray.android.converter;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by hadi on 12/22/16.
 */
public class VolleySingleton {
    private static VolleySingleton ourInstance;
    private RequestQueue mRequestQueue;
    private static Context mCtx;

    public static VolleySingleton getInstance(Context ctx) {
        if ( ourInstance == null ) {
            ourInstance = new VolleySingleton(ctx);
        }
        return ourInstance;
    }

    private VolleySingleton(Context ctx) {
        mCtx = ctx;
        mRequestQueue = getRequestQueue();
    }

    public RequestQueue getRequestQueue() {
        if ( mRequestQueue == null ) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
