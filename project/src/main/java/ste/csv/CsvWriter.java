package ste.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ste.Util;
import ste.Util.Threeple;
import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;

public final class CsvWriter {
    private final StringBuilder csvBuilder;
    private final List<Method> valueGetters;
    private final String filename;

    private CsvWriter(String filename, Class<?> c) throws CsvException {
        if(!c.isAnnotationPresent(CsvDescriptor.class)) {
            throw new CsvException();
        }

        List<Util.Threeple<Integer, String, Method>> intermediate 
            = new ArrayList<>();

        for(Method m : c.getMethods()) {
            CsvColumn a = m.getAnnotation(CsvColumn.class);
            if(a != null) {
                intermediate.add(new Util.Threeple<>(a.order(), a.name(), m));
            }
        }

        intermediate.sort(new Comparator<Util.Threeple<Integer, String, Method>>() {
            @Override
            public int compare(Threeple<Integer, String, Method> o1, Threeple<Integer, String, Method> o2) {
                return o1.getFirst() - o2.getFirst();
            } 
        });

        valueGetters = new ArrayList<>();
        csvBuilder = new StringBuilder();

        int sz = intermediate.size();

        for(int i = 0; i < sz; ++i) {
            Util.Threeple<Integer, String, Method> tmp = intermediate.get(i);
            valueGetters.add(tmp.getThird());
            csvBuilder
                .append(tmp.getSecond())
                .append(i == sz - 1 ? "\n" : ",");
        }

        this.filename = filename;
    }

    private void addEntry(Object any) {
        int nMethods = valueGetters.size();
        for(int i = 0; i < nMethods; ++i) {
            try {
                csvBuilder
                    .append(valueGetters.get(i).invoke(any))
                    .append(i == nMethods - 1 ? '\n' : ',');
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void write() 
            throws FileNotFoundException, IOException {

        File f = new File(filename);
        f.getParentFile().mkdirs();
        f.createNewFile();

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(csvBuilder.toString().getBytes());
        }
    }


    public static <T> void writeAll(String filename, Class<T> cls, List<T> elems) 
            throws CsvException, FileNotFoundException, IOException {
        CsvWriter csv = new CsvWriter(filename, cls);
        for(T elem : elems) {
            csv.addEntry(elem);
        }
        csv.write();
    }
}
