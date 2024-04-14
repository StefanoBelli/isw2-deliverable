package ste.jirarest.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.json.JSONArray;
import org.json.JSONObject;

public final class Parsing {
    public static Object getArray(JSONArray a, Class<?> t) {
        int len = a.length();
        Object o = Array.newInstance(t, len);
        for(int i = 0; i < len; ++i) {
            try {
                Constructor<?> ctor = t.getConstructor(JSONObject.class);
                Array.set(o, i, ctor.newInstance(a.getJSONObject(i)));
            } catch (
                        ArrayIndexOutOfBoundsException | 
                        IllegalArgumentException | 
                        InstantiationException | 
                        IllegalAccessException | 
                        InvocationTargetException | 
                        NoSuchMethodException | 
                        SecurityException e
                    ) {
                throw new RuntimeException(e);
            }
        }

        return o;
    }

    public static final String M_GET_STRING = "getString";
    public static final String M_GET_BOOLEAN = "getBoolean";

    public static Object maybeGet(JSONObject o, String key, String jsonObjectMethod) {
        if(o.has(key)) {
            try {
                Method m = o.getClass().getMethod(jsonObjectMethod,String.class);
                return m.invoke(o, jsonObjectMethod);
            } catch (
                        IllegalAccessException | 
                        InvocationTargetException | 
                        NoSuchMethodException | 
                        SecurityException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
}