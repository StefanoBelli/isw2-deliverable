package ste.evaluation;

import ste.Util;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public final class WalkForward {

    private static final class Project {
        private final Instances dataset;
        private final String name;

        public Project(Instances dataset, String name) {
            this.dataset = dataset;
            this.name = name;
        }

        public Instances getDataset() {
            return dataset;
        }

        public String getName() {
            return name;
        }
    }

    private final Project storm;
    private final Project bookKeeper;

    private static String getDataSetArffFilename(String proj) {
        return String.format("%s-dataset.arff", proj);
    }

    public WalkForward(
            String stormProjectName, 
            String bookKeeperProjectName, 
            String stormDatasetCsv, 
            String bookKeeperDatasetCsv) throws Exception {

        String bookKeeperDatasetArffFilename = getDataSetArffFilename(bookKeeperProjectName);
        String stormDatasetArffFilename = getDataSetArffFilename(stormProjectName);

        Util.csv2Arff(bookKeeperDatasetCsv, bookKeeperDatasetArffFilename);
        Util.csv2Arff(stormDatasetCsv, stormDatasetArffFilename);

        Instances stormDataset = loadArff(stormDatasetArffFilename);
        Instances bookKeeperDataset = loadArff(bookKeeperDatasetArffFilename);

        storm = new Project(stormDataset, stormProjectName);
        bookKeeper = new Project(bookKeeperDataset, bookKeeperProjectName);
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
