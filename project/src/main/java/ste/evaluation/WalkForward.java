package ste.evaluation;

import java.util.ArrayList;
import java.util.List;

import ste.Util;
import ste.evaluation.component.classifier.Classifier;
import ste.evaluation.component.classifier.impls.IBkClassifier;
import ste.evaluation.component.classifier.impls.NaiveBayesClassifier;
import ste.evaluation.component.classifier.impls.RandomForestClassifier;
import ste.evaluation.component.cost.CostSensitivity;
import ste.evaluation.component.cost.Sensitive;
import ste.evaluation.component.fesel.FeatureSelection;
import ste.evaluation.component.fesel.impls.ApplyBackwardSearch;
import ste.evaluation.component.fesel.impls.ApplyBestFirst;
import ste.evaluation.component.sampling.Sampling;
import ste.evaluation.component.sampling.impls.ApplyOversampling;
import ste.evaluation.component.sampling.impls.ApplySmote;
import ste.evaluation.component.sampling.impls.ApplyUndersampling;
import ste.model.Result;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
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
            maxRelIdx = (int) dataset.get(dataset.numInstances() - 1).value(0);
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
        initEvalConfigs();
    }

    private Instances loadArff(String filename) throws Exception {
        DataSource source = new DataSource(filename);
        return source.getDataSet();
    }

    private Util.Pair<Instances, Instances> getWfSplitAtIterNum(int iterIdx) {
        Instances origDataset = project.getDataset();

        Instances trainingSet = new Instances(origDataset);
        Instances testingSet = new Instances(origDataset);

        trainingSet.removeIf(t -> (int) t.value(0) >= iterIdx);
        testingSet.removeIf(t -> (int) t.value(0) != iterIdx);

        return new Util.Pair<>(trainingSet, testingSet);
    }

    private void initEvalConfigs() {
        classifiers = new Classifier[]{
            new NaiveBayesClassifier(),
            new RandomForestClassifier(),
            new IBkClassifier() 
        };

        featureSelections = new FeatureSelection[]{
            new FeatureSelection(null),
            new FeatureSelection(new ApplyBestFirst()),
            new FeatureSelection(new ApplyBackwardSearch())
        };

        samplings = new Sampling[]{
            new Sampling(null),
            new Sampling(new ApplyOversampling()),
            new Sampling(new ApplyUndersampling()),
            new Sampling(new ApplySmote()),
        };
 
        costSensitivities = new CostSensitivity[]{
            new CostSensitivity(Sensitive.NONE),
            new CostSensitivity(Sensitive.THRESHOLD),
            new CostSensitivity(Sensitive.LEARNING)   
        };
    }

    private Classifier[] classifiers;
    private FeatureSelection[] featureSelections;
    private Sampling[] samplings;
    private CostSensitivity[] costSensitivities;

    private List<Result> results;

    public void start() throws Exception {
        results = new ArrayList<>();

        for(int i = 0; i < project.getMaxRelIdx(); ++i) {
            Util.Pair<Instances,Instances> curDataset = getWfSplitAtIterNum(i);

            for(Classifier classifier : classifiers) {
                Instances curTrainingSet = new Instances(curDataset.getFirst());
                Instances curTestingSet = new Instances(curDataset.getSecond());

                /*
                curTrainingSet.deleteAttributeAt(0);
                curTrainingSet.deleteAttributeAt(1);

                curTestingSet.deleteAttributeAt(0);
                curTestingSet.deleteAttributeAt(1);
                */

                curTrainingSet.setClassIndex(curTrainingSet.numAttributes() - 1);
                curTestingSet.setClassIndex(curTestingSet.numAttributes() - 1);

                AbstractClassifier vanillaClassifier = classifier.getClassifier();

                for(FeatureSelection featureSelection : featureSelections) {

                    for(Sampling sampling : samplings) {

                        for(CostSensitivity costSensitivity : costSensitivities) {

                            Util.Pair<Instances, Instances> curFilteredDataset = 
                                featureSelection.getFilteredDataSets(
                                    curTrainingSet, curTestingSet);

                            curFilteredDataset.getFirst().setClassIndex(
                                curFilteredDataset.getFirst().numAttributes() - 1);
                            curFilteredDataset.getSecond().setClassIndex(
                                curFilteredDataset.getSecond().numAttributes() - 1);

                            Util.Pair<AbstractClassifier, Instances> curSamplingResult = 
                                sampling.getFilteredClassifierWithSampledTrainingSet(
                                    vanillaClassifier, curFilteredDataset.getFirst());
                            
                            AbstractClassifier curCostSensitiveClassifier =
                                costSensitivity.getCostSensititiveClassifier(
                                    curSamplingResult.getFirst());

                            curCostSensitiveClassifier.buildClassifier(curSamplingResult.getSecond());

                            Evaluation eval = new Evaluation(curFilteredDataset.getSecond());
                            eval.evaluateModel(curCostSensitiveClassifier, curFilteredDataset.getSecond());
                        }
                    }
                }
            }
        }
    }

    public List<Result> getResults() {
        return results;
    }
}
