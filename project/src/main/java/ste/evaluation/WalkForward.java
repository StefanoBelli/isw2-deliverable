package ste.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import me.tongfei.progressbar.ProgressBar;
import ste.Util;
import ste.csv.CsvWriter;
import ste.evaluation.component.classifier.Classifier;
import ste.evaluation.component.classifier.impls.IBkClassifier;
import ste.evaluation.component.classifier.impls.NaiveBayesClassifier;
import ste.evaluation.component.classifier.impls.RandomForestClassifier;
import ste.evaluation.component.cost.CostSensitivity;
import ste.evaluation.component.cost.Sensitive;
import ste.evaluation.component.fesel.FeatureSelection;
import ste.evaluation.component.fesel.impls.ApplyGreedyBackwards;
import ste.evaluation.component.fesel.impls.ApplyBestFirst;
import ste.evaluation.component.sampling.Sampling;
import ste.evaluation.component.sampling.impls.ApplyOversampling;
import ste.evaluation.component.sampling.impls.ApplySmote;
import ste.evaluation.component.sampling.impls.ApplyUndersampling;
import ste.evaluation.eam.NPofBx;
import ste.model.Result;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public final class WalkForward {

    private final String projectName;
    private final int wholeDatasetSize;
    private final WalkForwardSplitIterator wfSplitsIterator;

    private String getNPofBxFilename(int wfRunNo, EvaluationProfile profile) {
        return String.format("csv_output/npofbx/%s/%s_%s_%s_%s_%s_%d.csv",
                                projectName,
                                projectName.toUpperCase(),
                                profile.getClassifier().getName().toUpperCase(),
                                profile.getFeatureSelection().getName().toUpperCase(),
                                profile.getSampling().getName().toUpperCase(),
                                profile.getCostSensitivity().getName().toUpperCase(),
                                wfRunNo);
    }

    public WalkForward(String projectName, int wholeDatasetSize, WalkForwardSplitIterator wfSplitsIterator) {
        this.projectName = projectName;
        this.wholeDatasetSize = wholeDatasetSize;
        this.wfSplitsIterator = wfSplitsIterator;
    }

    private final Classifier[] classifiers = {
        new NaiveBayesClassifier(),
        new RandomForestClassifier(),
        new IBkClassifier()
    };

    private final FeatureSelection[] featureSelections = {
        new FeatureSelection(null),
        new FeatureSelection(new ApplyBestFirst()),
        new FeatureSelection(new ApplyGreedyBackwards())
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
        String msg = String.format("Evaluating project %s...", projectName);

        int nWfCount = wfSplitsIterator.getNumOfTotalWalkForwardSplits();
        int nCl = classifiers.length;
        int nFs = featureSelections.length;
        int nSm = samplings.length;
        int nCs = costSensitivities.length;

        ProgressBar pb = Util.buildProgressBar(msg, nWfCount * nCl * nFs * nSm * nCs);
        doStart(pb);

        pb.close();
    }

    private void doStart(ProgressBar pb) {
        results = new ArrayList<>();

        for(int i = 1; wfSplitsIterator.hasNext(); ++i) {
            var curDataset = copyWfSplitAndInit(wfSplitsIterator.next());

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

                            var evaluation = evaluate(i, profile, curDataset);

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

    private void addResultingEvaluation(Result currentResult, Util.Pair<Evaluation, Float> evaluation) {
        if(evaluation != null) {
            setPerfMetricsForResult(currentResult, evaluation.getFirst());
            currentResult.setNPofB20(evaluation.getSecond() * 100);
            results.add(currentResult);
        }
    }

    private Util.Pair<Evaluation, Float> evaluate(
            int wfRun, EvaluationProfile profile, Util.Pair<Instances, Instances> datasets) {
        try {
            var resultingPair = obtainClassifierWithFilteredTestingSet(
                    profile,
                    datasets.getFirst(),
                    datasets.getSecond());

            var testingSet = resultingPair.getSecond();
            var classifier = resultingPair.getFirst();

            Evaluation eval = new Evaluation(testingSet);
            eval.evaluateModel(classifier, testingSet);

            NPofBx npofbx = new NPofBx();
            float nPofBxIndex = npofbx.indexFor(20, testingSet, classifier);
            CsvWriter.writeAll(
                getNPofBxFilename(wfRun, profile), 
                NPofBx.TableEntry.class, 
                npofbx.getEntries());

            return new Util.Pair<>(eval, nPofBxIndex);
        } catch (Exception e) {
            LoggerFactory
                .getLogger(WalkForward.class.getName())
                .warn(e.getMessage());

            return null;
        }
    }

    private Result setEarlyMetricsForResult(int wfIter, Util.Pair<Instances, Instances> datasets) {
        var trainingSet = datasets.getFirst();
        var testingSet = datasets.getSecond();

        Result currentResult = new Result();

        currentResult.setDataset(projectName);

        currentResult.setNumTrainingRelease(wfIter);

        float percDefTest = (Util.numOfPositives(testingSet)*100) / (float) testingSet.size();
        currentResult.setPercDefectiveInTesting(percDefTest);

        float percDefTrain = (Util.numOfPositives(trainingSet)*100) / (float) trainingSet.size();
        currentResult.setPercDefectiveInTraining(percDefTrain);

        float percTrain = (trainingSet.size() * 100) / (float) wholeDatasetSize;
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

    private void setPerfMetricsForResult(
            Result orig, Evaluation eval) {
        orig.setAuc((float)eval.areaUnderROC(posClassIdx));
        orig.setKappa((float)eval.kappa());
        orig.setFn((int)eval.numFalseNegatives(posClassIdx));
        orig.setFp((int)eval.numFalsePositives(posClassIdx));
        orig.setTp((int)eval.numTruePositives(posClassIdx));
        orig.setTn((int)eval.numTrueNegatives(posClassIdx));
        orig.setPrecision((float)eval.precision(posClassIdx));
        orig.setRecall((float)eval.recall(posClassIdx));
        orig.setF1score((float)eval.fMeasure(posClassIdx));
    }

    private Util.Pair<Instances, Instances> copyWfSplitAndInit(WalkForwardSplit split) {
        var curDataset = new Util.Pair<>(split.getTrainingSet(), split.getTestingSet());

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

    private int posClassIdx;

    private Util.Pair<AbstractClassifier, Instances> obtainClassifierWithFilteredTestingSet(
            EvaluationProfile evaluationProfile,
            Instances trainingSet,
            Instances testingSet) throws Exception {

        var curFilteredDataset = evaluationProfile.getFeatureSelection().getFilteredDataSets(
                trainingSet, testingSet);

        var curFilteredTrainingSet = curFilteredDataset.getFirst();
        var curFilteredTestingSet = curFilteredDataset.getSecond();

        curFilteredTrainingSet.setClassIndex(curFilteredTrainingSet.numAttributes() - 1);
        curFilteredTestingSet.setClassIndex(curFilteredTestingSet.numAttributes() - 1);

        posClassIdx = curFilteredTestingSet.classAttribute().indexOfValue("yes");

        var curSamplingResult = evaluationProfile.getSampling().getFilteredClassifierWithSampledTrainingSet(
                evaluationProfile.getClassifier().buildClassifier(), curFilteredTrainingSet);

        var curCostSensitiveClassifier = evaluationProfile.getCostSensitivity().getCostSensititiveClassifier(
                curSamplingResult.getFirst());

        curCostSensitiveClassifier.buildClassifier(curSamplingResult.getSecond());

        return new Util.Pair<>(curCostSensitiveClassifier, curFilteredTestingSet);
    }
}
