package ste.evaluation;

import ste.Util;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public final class WalkForwardSplit {
    private final Instances trainingSet;
    private final Instances testingSet;
    private final int numTrainingRels;

    public WalkForwardSplit(String trainingSetCsv, String testingSetCsv, int fileIdx) throws Exception {
        Util.csv2Arff(trainingSetCsv, String.format("training-tmp-%d.arff", fileIdx));
        Util.csv2Arff(testingSetCsv, String.format("testing-tmp-%d.arff", fileIdx));
        trainingSet = new DataSource(String.format("training-tmp-%d.arff", fileIdx)).getDataSet();
        testingSet = new DataSource(String.format("testing-tmp-%d.arff", fileIdx)).getDataSet();
        numTrainingRels = (int) trainingSet.get(trainingSet.numInstances() - 1).value(0) + 1;
    }

    public int getNumTrainingRels() {
        return numTrainingRels;
    }

    public Instances getTestingSet() {
        return testingSet;
    }

    public Instances getTrainingSet() {
        return trainingSet;
    }
}
