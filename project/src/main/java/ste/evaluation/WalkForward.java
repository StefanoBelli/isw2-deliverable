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
            //new RandomForestClassifier(),
            new IBkClassifier() 
        };

        featureSelections = new FeatureSelection[]{
            new FeatureSelection(null),
            //new FeatureSelection(new ApplyBestFirst()),
            //new FeatureSelection(new ApplyBackwardSearch())
        };

        samplings = new Sampling[]{
            new Sampling(null),
            new Sampling(new ApplyOversampling()),
            new Sampling(new ApplyUndersampling()),
            //new Sampling(new ApplySmote()),
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

        System.out.println(project.getMaxRelIdx());

        for(int i = 1; i < project.getMaxRelIdx(); ++i) {
            System.out.println("cycle " + i);
            var curDataset = getWfSplitAtIterNum(i);

            var curTrainingSet = new Instances(curDataset.getFirst());
            var curTestingSet = new Instances(curDataset.getSecond());

            curTrainingSet.deleteAttributeAt(0);
            curTrainingSet.deleteAttributeAt(0);
            curTestingSet.deleteAttributeAt(0);
            curTestingSet.deleteAttributeAt(0);

            curTrainingSet.setClassIndex(curTrainingSet.numAttributes() - 1);
            curTestingSet.setClassIndex(curTestingSet.numAttributes() - 1);
            System.out.println(curTrainingSet);
            System.out.println(curTestingSet);

            Result earlyResult = setEarlyMetricsForResult(i, curTrainingSet, curTestingSet);
            for(Classifier classifier : classifiers) {
                System.out.println("new classifier");

                AbstractClassifier vanillaClassifier = classifier.getClassifier();

                for(FeatureSelection featureSelection : featureSelections) {
                    System.out.println("new fs");

                    for(Sampling sampling : samplings) {
                        System.out.println("new sampling");

                        for(CostSensitivity costSensitivity : costSensitivities) {
                            System.out.println("new cs");

                            Result finalResult = 
                                setConfigForResult(earlyResult, classifier, 
                                        featureSelection, sampling, costSensitivity);

                            var curFilteredDataset = featureSelection.getFilteredDataSets(
                                    curTrainingSet, curTestingSet);
                            
                            var curFilteredTrainingSet = curFilteredDataset.getFirst();
                            var curFilteredTestingSet = curFilteredDataset.getSecond();

                            curFilteredTrainingSet.setClassIndex(curFilteredTrainingSet.numAttributes() - 1);
                            curFilteredTestingSet.setClassIndex(curFilteredTestingSet.numAttributes() - 1);

                            var curSamplingResult = sampling.getFilteredClassifierWithSampledTrainingSet(
                                    vanillaClassifier, curFilteredTrainingSet);
                            
                            var curCostSensitiveClassifier = costSensitivity.getCostSensititiveClassifier(
                                    curSamplingResult.getFirst());

                            curCostSensitiveClassifier.buildClassifier(curSamplingResult.getSecond());

                            Evaluation eval = new Evaluation(curFilteredTestingSet);
                            eval.evaluateModel(curCostSensitiveClassifier, curFilteredTestingSet);

                            setPerfMetricsForResult(finalResult, eval);
                            results.add(finalResult);
                            System.out.println("DONE");
                        }
                    }
                }
            }
        }
    }

    public List<Result> getResults() {
        return results;
    }

    private Result setEarlyMetricsForResult(int wfIter, Instances trainingSet, Instances testingSet) {
        Result currentResult = new Result();

        currentResult.setDataset(project.getName());

        currentResult.setNumTrainingRelease(wfIter);

        float percDefTest = (Util.numOfPositives(testingSet)*100) / (float) trainingSet.size();
        currentResult.setPercDefectiveInTesting(percDefTest);

        float percDefTrain = (Util.numOfPositives(trainingSet)*100) / (float) testingSet.size();
        currentResult.setPercDefectiveInTraining(percDefTrain);

        float percTrain = (trainingSet.size() * 100) / (float) project.getDataset().size();
        currentResult.setPercTrainingData(percTrain);

        return currentResult;
    }

    private Result setConfigForResult(
            Result orig, Classifier classif, FeatureSelection fs, 
            Sampling sampl, CostSensitivity costSens) {

        Result configResult = new Result(orig);

        configResult.setBalancing(sampl.getName());
        configResult.setClassifier(classif.getName());
        configResult.setFeatureSelection(fs.getName());
        configResult.setSensitivity(costSens.getName());

        return configResult;
    }

    private void setPerfMetricsForResult(
            Result orig, Evaluation eval) {

        orig.setAuc((float)eval.areaUnderROC(1));
        orig.setKappa((float)eval.kappa());
        orig.setFn((float)eval.numFalseNegatives(1));
        orig.setFp((float)eval.numFalsePositives(1));
        orig.setTp((float)eval.numTruePositives(1));
        orig.setTn((float)eval.numTrueNegatives(1));
        orig.setPrecision((float)eval.precision(1));
        orig.setRecall((float)eval.recall(1));
    }
}
