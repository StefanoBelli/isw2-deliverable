package ste.jirarest.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
                throw new RuntimeException();
            }
        }

        return o;
    }
}
