package ste.evaluation;

import ste.Util;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public final class WalkForward {

    private static final class Project {
        private final Instances dataset;
        private final String name;
        private final int maxRelIdx;

        public Project(Instances dataset, String name) {
            this.dataset = dataset;
            this.name = name;
            maxRelIdx = (int) dataset.get(dataset.numAttributes() - 1).value(0);
        }

        public Instances getDataset() {
            return dataset;
        }

        public String getName() {
            return name;
        }

        public int getMaxRelIdx() {
            return maxRelIdx;
        }
    }

    private final Project project;

    private static String getDataSetArffFilename(String proj) {
        return String.format("%s-dataset.arff", proj);
    }

    public WalkForward(String projectName, String projectDatasetCsv) throws Exception {
        String datasetArffFilename = getDataSetArffFilename(projectName);
        Util.csv2Arff(projectDatasetCsv, datasetArffFilename);
        Instances dataset = loadArff(datasetArffFilename);
        project = new Project(dataset, projectName);
    }

    private Instances loadArff(String filename) throws Exception {
        DataSource source = new DataSource(filename);
        return source.getDataSet();
    }

    private Util.Pair<Instances, Instances> getWfSplitAtIterNum(Project project, int iterIdx) {
        Instances origDataset = project.getDataset();

        Instances trainingSet = new Instances(origDataset);
        Instances testingSet = new Instances(origDataset);

        trainingSet.removeIf(t -> (int) t.value(0) >= iterIdx);
        testingSet.removeIf(t -> (int) t.value(0) != iterIdx);

        return new Util.Pair<>(trainingSet, testingSet);
    }

    public void start() {

    }

}
