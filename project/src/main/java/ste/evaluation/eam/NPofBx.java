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
        private long size;
        private double probYes;
        private double normProbYes;
        private boolean actual;

        public void setActual(boolean actual) {
            this.actual = actual;
        }

        public void setEntryId(int entryId) {
            this.entryId = entryId;
        }

        public void setNormProbYes(double normProbYes) {
            this.normProbYes = normProbYes;
        }

        public void setProbYes(double probYes) {
            this.probYes = probYes;
        }

        public void setSize(long size) {
            this.size = size;
        }

        @CsvColumn(order = 1, name = "ID")
        public int getEntryId() {
            return entryId;
        }

        @CsvColumn(order = 2, name = "Size")
        public long getSize() {
            return size;
        }

        @CsvColumn(order = 3, name = "Predicted")
        public double getProbYes() {
            return probYes;
        }

        @CsvColumn(order = 4, name = "Actual")
        public String getActualAsYesOrNo() {
            return actual ? "YES" : "NO";
        }
 
        public double getNormProbYes() {
            return normProbYes;
        }

        public boolean isActual() {
            return actual;
        }
    }

    private List<TableEntry> entries;
    
    public double indexFor(int x, Instances testingSet, Instances originalTestingSet,
            AbstractClassifier classifier) throws Exception {

        entries = new ArrayList<>();
        
        int origLastAttrIdx = originalTestingSet.numAttributes() - 1;
        int totalSize = 0;

        int totalActual = 0;

        for(int i = 0; i < testingSet.numInstances(); ++i) {
            Instance cur = testingSet.get(i);
            Instance origCur = originalTestingSet.get(i);

            double size = origCur.value(0);
            double pred = getPredictionPercForYesLabel(cur, classifier);
            boolean actual = origCur.toString(origLastAttrIdx).equals("yes");

            if(actual) {
                ++totalActual;
            }

            TableEntry entry = new TableEntry();
            entry.setActual(actual);
            entry.setSize(Math.round(size));
            entry.setProbYes(pred);
            entry.setNormProbYes(pred / size);
            entry.setEntryId(i);

            totalSize += (int) size;
            
            entries.add(entry);
        }

        return calcIndexCore(totalSize, x, totalActual);
    }

    public List<TableEntry> getEntries() {
        return entries;
    }

    private double calcIndexCore(int totalSize, int topX, int totalActuallyBuggy) {
        //rank by normalized probability of buggyness
        List<TableEntry> tents = new ArrayList<>(entries);
        tents.sort(
            (e1, e2) -> {
                double npy1 = e1.getNormProbYes();
                double npy2 = e2.getNormProbYes();
                int cmp = Double.compare(npy1, npy2);

                if (cmp < 0) {
                    return 1;
                } else if(cmp == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }
        );

        int topXPercSize = Math.round(totalSize * (topX / 100f));
        int sizeSoFar = 0;

        int topXActuallyBuggy = 0;

        for(TableEntry entry : tents) {
            sizeSoFar += entry.getSize();

            if(sizeSoFar > topXPercSize) {
                break;
            }

            if(entry.isActual()) {
                ++topXActuallyBuggy;
            }
        }

        return totalActuallyBuggy > 0 ? (double) topXActuallyBuggy / totalActuallyBuggy : 0f;
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
