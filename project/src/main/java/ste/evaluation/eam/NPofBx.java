package ste.evaluation.eam;

import java.util.ArrayList;
import java.util.List;

import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;
import ste.evaluation.eam.exception.NPofBxException;
import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

public final class NPofBx {

    // Compatability with ACUME tool
    @CsvDescriptor
    public static final class TableEntry {
        private int entryId;
        private int size;
        private float probYes;
        private float normProbYes;
        private boolean actual;

        public void setActual(boolean actual) {
            this.actual = actual;
        }

        public void setEntryId(int entryId) {
            this.entryId = entryId;
        }

        public void setNormProbYes(float normProbYes) {
            this.normProbYes = normProbYes;
        }

        public void setProbYes(float probYes) {
            this.probYes = probYes;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @CsvColumn(order = 1, name = "ID")
        public int getEntryId() {
            return entryId;
        }

        @CsvColumn(order = 2, name = "Size")
        public int getSize() {
            return size;
        }

        @CsvColumn(order = 3, name = "Predicted")
        public float getProbYes() {
            return probYes;
        }

        @CsvColumn(order = 4, name = "Actual")
        public String getActualAsYesNo() {
            return actual ? "YES" : "NO";
        }
 
        public float getNormProbYes() {
            return normProbYes;
        }

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
            double pred = getPredictionPercForYesLabel(cur, classifier);
            boolean actual = cur.toString(lastAttrIdx).equals("yes");

            if(actual) {
                ++totalActual;
            }

            TableEntry entry = new TableEntry();
            entry.setActual(actual);
            entry.setSize((int) size);
            entry.setProbYes((float) pred);
            entry.setNormProbYes((float) (pred / size));
            entry.setEntryId(i);

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
            (e1, e2) -> {
                float npy1 = e1.getNormProbYes();
                float npy2 = e2.getNormProbYes();

                if (npy1 < npy2) {
                    return 1;
                } else if(npy1 == npy2) {
                    return 0;
                } else {
                    return -1;
                }
            }
        );

        int topXPercSize = Math.round(totalSize * (topX / 100f));
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

        return totalActuallyBuggy > 0 ? (float) topXActuallyBuggy / totalActuallyBuggy : 0f;
    }

    private static double getPredictionPercForYesLabel(Instance inst, AbstractClassifier classifier) 
            throws Exception {
        double[] predDist = classifier.distributionForInstance(inst);

        for(int i = 0; i < predDist.length; ++i) {
            if(inst.classAttribute().value(i).equals("yes")) {
                return predDist[i];
            }
        }

        throw new NPofBxException();
    }
}
