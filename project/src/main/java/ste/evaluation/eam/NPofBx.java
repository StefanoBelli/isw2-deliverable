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
        private final float normProbYes;
        private final boolean actual;

        public TableEntry(
                int entryId, 
                int size, 
                float probYes, 
                float normProbYes, 
                boolean actual) {
            this.entryId = entryId;
            this.size = size;
            this.probYes = probYes;
            this.normProbYes = normProbYes;
            this.actual = actual;
        }

        @CsvColumn(order = 1, name = "Id")
        public int getEntryId() {
            return entryId;
        }

        @CsvColumn(order = 2, name = "Size")
        public int getSize() {
            return size;
        }

        @CsvColumn(order = 3, name = "Prob")
        public float getProbYes() {
            return probYes;
        }

        @CsvColumn(order = 4, name = "NormProb")
        public float getNormProbYes() {
            return normProbYes;
        }

        @CsvColumn(order = 5, name = "Actual")
        public boolean isActual() {
            return actual;
        }
    }

    private List<TableEntry> entries;
    
    public float indexFor(int x, Instances testingSet, 
            AbstractClassifier classifier) 
                throws Exception {

        entries = new ArrayList<>();
        
        int lastAttrIdx = testingSet.numAttributes() - 1;
        int totalSize = 0;

        int totalActual = 0;

        for(int i = 0; i < testingSet.numInstances(); ++i) {
            Instance cur = testingSet.get(i);

            double size = cur.value(0);
            double pred = classifier.classifyInstance(cur);
            boolean actual = cur.toString(lastAttrIdx).equals("yes");

            if(actual) {
                ++totalActual;
            }

            TableEntry entry = new TableEntry(
                i + 1, 
                (int) size, 
                (float) pred,
                (float)(pred / size), 
                actual);

            totalSize += (int) size;
            
            entries.add(entry);
        }

        return calcIndexCore(totalSize, x, totalActual);
    }

    public List<TableEntry> getEntries() {
        return entries;
    }

    private float calcIndexCore(int totalSize, int topX, int totalActuallyBuggy) {
        //rank by normalized probability of buggyness
        entries.sort(
            (e1, e2) -> 
                (int) (e1.getNormProbYes() - e2.getNormProbYes()) 
        );

        int topXPercSize = totalSize * (topX / 100);
        int sizeSoFar = 0;

        int topXActuallyBuggy = 0;

        for(TableEntry entry : entries) {
            sizeSoFar += entry.getSize();

            if(sizeSoFar > topXPercSize) {
                break;
            }

            if(entry.isActual()) {
                ++topXActuallyBuggy;
            }
        }

        return (float) topXActuallyBuggy / totalActuallyBuggy;
    }
}
