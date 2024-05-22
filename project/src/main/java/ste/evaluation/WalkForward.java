package ste.evaluation;

import java.util.ArrayList;
import java.util.List;

import me.tongfei.progressbar.ProgressBar;
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
    }

    private Instances loadArff(String filename) throws Exception {
        DataSource source = new DataSource(filename);
        return source.getDataSet();
    }

    private final Classifier[] classifiers = {
        new NaiveBayesClassifier(),
        new RandomForestClassifier(),
        new IBkClassifier()
    };

    private final FeatureSelection[] featureSelections = {
        new FeatureSelection(null),
        new FeatureSelection(new ApplyBestFirst()),
        new FeatureSelection(new ApplyBackwardSearch())
    };

    private final Sampling[] samplings = {
        new Sampling(null),
        new Sampling(new ApplyOversampling()),
        new Sampling(new ApplyUndersampling()),
        new Sampling(new ApplySmote()),
    };

    private final CostSensitivity[] costSensitivities = {
        new CostSensitivity(Sensitive.NONE),
        new CostSensitivity(Sensitive.THRESHOLD),
        new CostSensitivity(Sensitive.LEARNING)   
    };

    private List<Result> results;

    public void start() {
        String msg = String.format("Evaluating project %s...", project.getName());

        int nRels = project.getMaxRelIdx();
        int nCl = classifiers.length;
        int nFs = featureSelections.length;
        int nSm = samplings.length;
        int nCs = costSensitivities.length;

        ProgressBar pb = Util.buildProgressBar(msg, nRels * nCl * nFs * nSm * nCs);
        doStart(pb);

        pb.close();
    }

    private void doStart(ProgressBar pb) {
        results = new ArrayList<>();

        for(int i = 1; i <= project.getMaxRelIdx(); ++i) {
            var curDataset = copyWfSplitAndInit(i);
            Result earlyResult = setEarlyMetricsForResult(i, curDataset);

            for(Classifier classifier : classifiers) {

                for(FeatureSelection featureSelection : featureSelections) {

                    for(Sampling sampling : samplings) {

                        for(CostSensitivity costSensitivity : costSensitivities) {
 
                            EvaluationProfile profile = new EvaluationProfile();
                            profile.setClassifier(classifier);
                            profile.setFeatureSelection(featureSelection);
                            profile.setSampling(sampling);
                            profile.setCostSensitivity(costSensitivity);

                            Result finalResult = setConfigForResult(earlyResult, profile);

                            Evaluation evaluation = evaluate(profile, curDataset);

                            addResultingEvaluation(finalResult, evaluation);
                            
                            pb.setExtraMessage(profile.toString());
                            pb.step();
                        }
                    }
                }
            }
        }
    }

    public List<Result> getResults() {
        return results;
    }

    
    private static final class EvaluationProfile {
        private Classifier classifier;
        private FeatureSelection featureSelection;
        private Sampling sampling;
        private CostSensitivity costSensitivity;

        public Classifier getClassifier() {
            return classifier;
        }

        public CostSensitivity getCostSensitivity() {
            return costSensitivity;
        }

        public FeatureSelection getFeatureSelection() {
            return featureSelection;
        }

        public Sampling getSampling() {
            return sampling;
        }

        public void setClassifier(Classifier classifier) {
            this.classifier = classifier;
        }

        public void setCostSensitivity(CostSensitivity costSensitivity) {
            this.costSensitivity = costSensitivity;
        }

        public void setFeatureSelection(FeatureSelection featureSelection) {
            this.featureSelection = featureSelection;
        }

        public void setSampling(Sampling sampling) {
            this.sampling = sampling;
        }

        @Override
        public String toString() {
            return String.format("%c with %c, %c, %c", 
                classifier.getName().toLowerCase().charAt(0), 
                featureSelection.getName().toLowerCase().charAt(0), 
                sampling.getName().toLowerCase().charAt(0), 
                costSensitivity.getName().toLowerCase().charAt(0));
        }
    }

    private void addResultingEvaluation(Result currentResult, Evaluation evaluation) {
        if(evaluation != null) {
            setPerfMetricsForResult(currentResult, evaluation);
            results.add(currentResult);
        }
    }

    private static Evaluation evaluate(EvaluationProfile profile, Util.Pair<Instances, Instances> datasets) {
        try {
            var resultingPair = obtainClassifierWithFilteredTestingSet(
                    profile,
                    datasets.getFirst(),
                    datasets.getSecond());

            var trainingSet = resultingPair.getSecond();
            var classifier = resultingPair.getFirst();

            Evaluation eval = new Evaluation(trainingSet);
            eval.evaluateModel(classifier, trainingSet);

            return eval;
        } catch (Exception e) {
            return null;
        }
    }

    private Result setEarlyMetricsForResult(int wfIter, Util.Pair<Instances, Instances> datasets) {
        var trainingSet = datasets.getFirst();
        var testingSet = datasets.getSecond();

        Result currentResult = new Result();

        currentResult.setDataset(project.getName());

        currentResult.setNumTrainingRelease(wfIter);

        float percDefTest = (Util.numOfPositives(testingSet)*100) / (float) testingSet.size();
        currentResult.setPercDefectiveInTesting(percDefTest);

        float percDefTrain = (Util.numOfPositives(trainingSet)*100) / (float) trainingSet.size();
        currentResult.setPercDefectiveInTraining(percDefTrain);

        float percTrain = (trainingSet.size() * 100) / (float) project.getDataset().size();
        currentResult.setPercTrainingData(percTrain);

        return currentResult;
    }

    private static Result setConfigForResult(
            Result orig, EvaluationProfile evaluationProfile) {

        Result configResult = new Result(orig);

        configResult.setBalancing(evaluationProfile.getSampling().getName());
        configResult.setClassifier(evaluationProfile.getClassifier().getName());
        configResult.setFeatureSelection(evaluationProfile.getFeatureSelection().getName());
        configResult.setSensitivity(evaluationProfile.getCostSensitivity().getName());

        return configResult;
    }

    private static void setPerfMetricsForResult(
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

    private Util.Pair<Instances, Instances> getWfSplitAtIterNum(int iterIdx) {
        Instances origDataset = project.getDataset();

        Instances trainingSet = new Instances(origDataset);
        Instances testingSet = new Instances(origDataset);

        trainingSet.removeIf(t -> (int) t.value(0) >= iterIdx);
        testingSet.removeIf(t -> (int) t.value(0) != iterIdx);

        return new Util.Pair<>(trainingSet, testingSet);
    }

    private Util.Pair<Instances, Instances> copyWfSplitAndInit(int i) {
        var curDataset = getWfSplitAtIterNum(i);

        var curTrainingSet = new Instances(curDataset.getFirst());
        var curTestingSet = new Instances(curDataset.getSecond());

        curTrainingSet.deleteAttributeAt(0);
        curTrainingSet.deleteAttributeAt(0);
        curTestingSet.deleteAttributeAt(0);
        curTestingSet.deleteAttributeAt(0);

        curTrainingSet.setClassIndex(curTrainingSet.numAttributes() - 1);
        curTestingSet.setClassIndex(curTestingSet.numAttributes() - 1);

        return new Util.Pair<>(curTrainingSet, curTestingSet);
    }

    private static Util.Pair<AbstractClassifier, Instances> obtainClassifierWithFilteredTestingSet(
            EvaluationProfile evaluationProfile,
            Instances trainingSet,
            Instances testingSet) throws Exception {

        var curFilteredDataset = evaluationProfile.getFeatureSelection().getFilteredDataSets(
                trainingSet, testingSet);

        var curFilteredTrainingSet = curFilteredDataset.getFirst();
        var curFilteredTestingSet = curFilteredDataset.getSecond();

        curFilteredTrainingSet.setClassIndex(curFilteredTrainingSet.numAttributes() - 1);
        curFilteredTestingSet.setClassIndex(curFilteredTestingSet.numAttributes() - 1);

        var curSamplingResult = evaluationProfile.getSampling().getFilteredClassifierWithSampledTrainingSet(
                evaluationProfile.getClassifier().buildClassifier(), curFilteredTrainingSet);

        var curCostSensitiveClassifier = evaluationProfile.getCostSensitivity().getCostSensititiveClassifier(
                curSamplingResult.getFirst());

        curCostSensitiveClassifier.buildClassifier(curSamplingResult.getSecond());

        return new Util.Pair<>(curCostSensitiveClassifier, curFilteredTestingSet);
    }
}
