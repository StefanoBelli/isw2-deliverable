package ste.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ste.Util;
import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;

public final class CsvWriter {
    private final StringBuilder csvBuilder;
    private final List<Method> valueGetters;
    private final String filename;

    private CsvWriter(String filename, Class<?> c) throws CsvWriterException {
        if(!c.isAnnotationPresent(CsvDescriptor.class)) {
            throw new CsvWriterException("Class must have @CsvDescriptor annotation");
        }

        List<Util.Threeple<Integer, String, Method>> intermediate 
            = new ArrayList<>();

        for(Method m : c.getMethods()) {
            CsvColumn a = m.getAnnotation(CsvColumn.class);
            if(a != null) {
                intermediate.add(new Util.Threeple<>(a.order(), a.name(), m));
            }
        }

        intermediate.sort((o1, o2) -> o1.getFirst() - o2.getFirst());

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
                throw new CsvWriterInvokeException(e);
            }
        }
    }

    private void write() 
            throws IOException {

        File f = new File(filename);
        f.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(csvBuilder.toString().getBytes());
            fos.flush();
        }
    }


    public static <T> void writeAll(String filename, Class<T> cls, List<T> elems) 
            throws CsvWriterException, IOException {
        CsvWriter csv = new CsvWriter(filename, cls);
        for(T elem : elems) {
            csv.addEntry(elem);
        }
        csv.write();
    }
}
