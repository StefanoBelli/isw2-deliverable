package ste.evaluation.eam;

import java.util.ArrayList;
import java.util.List;

import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;
import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

public final class NPofBx {

    @CsvDescriptor
    public static final class TableEntry {
        private final int entryId;
        private final int size;
        private final float probYes;
        private final boolean actual;

        public TableEntry(int entryId, int size, float probYes, boolean actual) {
            this.entryId = entryId;
            this.size = size;
            this.probYes = probYes;
            this.actual = actual;
        }

        @CsvColumn(order = 1, name = "Id")
        public int getEntryId() {
            return entryId;
        }

        @CsvColumn(order = 2, name = "NormPredict")
        public float getProbYes() {
            return probYes;
        }

        @CsvColumn(order = 3, name = "Size")
        public int getSize() {
            return size;
        }

        @CsvColumn(order = 4, name = "Actual")
        public boolean isActual() {
            return actual;
        }
    }

    private List<TableEntry> entries;
    
    public double indexFor(double x, Instances testingSet, AbstractClassifier classifier) throws Exception {
        entries = new ArrayList<>();
        
        int lastAttrIdx = testingSet.numAttributes() - 1;

        for(int i = 0; i < testingSet.numInstances(); ++i) {
            Instance cur = testingSet.get(i);

            double size = cur.value(0);
            double pred = classifier.classifyInstance(cur);
            boolean actual = cur.toString(lastAttrIdx).equals("yes");

            TableEntry entry = new TableEntry(
                i + 1, 
                (int) size, 
                (float)(pred / size), 
                actual);
            
            entries.add(entry);
        }
    }

    public List<TableEntry> getEntries() {
        return entries;
    }
}
