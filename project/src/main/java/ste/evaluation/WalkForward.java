package ste.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import me.tongfei.progressbar.ProgressBar;
import ste.Util;
import ste.csv.CsvWriter;
import ste.evaluation.component.classifier.MyClassifier;
import ste.evaluation.component.classifier.impls.IBkClassifier;
import ste.evaluation.component.classifier.impls.NaiveBayesClassifier;
import ste.evaluation.component.classifier.impls.RandomForestClassifier;
import ste.evaluation.component.cost.CostSensitivity;
import ste.evaluation.component.cost.Sensitive;
import ste.evaluation.component.fesel.FeatureSelection;
import ste.evaluation.component.fesel.impls.ApplyBestFirstBackward;
import ste.evaluation.component.sampling.Sampling;
import ste.evaluation.component.sampling.impls.ApplySmote;
import ste.evaluation.component.sampling.impls.ApplyUndersampling;
import ste.evaluation.eam.NPofBx;
import ste.model.Result;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.classifiers.Classifier;

public final class WalkForward {

    private final String projectName;
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

    public WalkForward(String projectName, WalkForwardSplitIterator wfSplitsIterator) {
        this.projectName = projectName;
        this.wfSplitsIterator = wfSplitsIterator;
    }

    private static final class Configuration {
        private FeatureSelection featureSelection;
        private Sampling sampling;
        private CostSensitivity costSensitivity;

        public Configuration(
            FeatureSelection featureSelection, 
            Sampling sampling, 
            CostSensitivity costSensitivity) {
            
            this.featureSelection = featureSelection;
            this.sampling = sampling;
            this.costSensitivity = costSensitivity;
        }
    }

    private final MyClassifier[] classifiers = {
        new NaiveBayesClassifier(),
        new RandomForestClassifier(),
        new IBkClassifier()
    };
    
    /*
     * Evaluating profiles 
     */
    private final Configuration[] configurations = {
        new Configuration(
            new FeatureSelection(null),
            new Sampling(null),
            new CostSensitivity(Sensitive.NONE)
        ), 
        new Configuration(
            new FeatureSelection(null),
            new Sampling(null),
            new CostSensitivity(Sensitive.LEARNING)
        ),
        new Configuration(
            new FeatureSelection(new ApplyBestFirstBackward()),
            new Sampling(null),
            new CostSensitivity(Sensitive.NONE)
        ),
        new Configuration(
            new FeatureSelection(new ApplyBestFirstBackward()),
            new Sampling(null),
            new CostSensitivity(Sensitive.LEARNING)
        ),
        new Configuration(
            new FeatureSelection(new ApplyBestFirstBackward()),
            new Sampling(new ApplyUndersampling()),
            new CostSensitivity(Sensitive.NONE)
        ),
        new Configuration(
            new FeatureSelection(new ApplyBestFirstBackward()),
            new Sampling(new ApplySmote()),
            new CostSensitivity(Sensitive.NONE)
        ),
        new Configuration(
            new FeatureSelection(null),
            new Sampling(new ApplyUndersampling()),
            new CostSensitivity(Sensitive.NONE)
        ),
        new Configuration(
            new FeatureSelection(null),
            new Sampling(new ApplySmote()),
            new CostSensitivity(Sensitive.NONE)
        ),
    };

    private List<Result> results;

    public void start() {
        String msg = String.format("Evaluating project %s...", projectName);

        int nWfCount = wfSplitsIterator.getNumOfTotalWalkForwardSplits();
        int nCl = classifiers.length;
        int nConf = configurations.length;

        ProgressBar pb = Util.buildProgressBar(msg, nWfCount * nCl * nConf);
        doStart(pb);

        pb.close();
    }

    private void doStart(ProgressBar pb) {
        results = new ArrayList<>();

        while(wfSplitsIterator.hasNext()) {
            WalkForwardSplit wfSplit = wfSplitsIterator.next();

            for (MyClassifier classifier : classifiers) {

                for (Configuration configuration : configurations) {
                    var curDataset = copyWfSplitAndInit(wfSplit);
                    Result earlyResult = setEarlyMetricsForResult(wfSplit.getNumTrainingRels(), curDataset);

                    EvaluationProfile profile = new EvaluationProfile();
                    profile.setClassifier(classifier);
                    profile.setFeatureSelection(configuration.featureSelection);
                    profile.setSampling(configuration.sampling);
                    profile.setCostSensitivity(configuration.costSensitivity);

                    Result finalResult = setConfigForResult(earlyResult, profile);

                    var evaluation = evaluate(wfSplit.getNumTrainingRels(), profile, curDataset);

                    addResultingEvaluation(finalResult, evaluation);

                    pb.setExtraMessage(profile.toString());
                    pb.step();
                }
            }
        }
    }

    public List<Result> getResults() {
        return results;
    }

    
    private static final class EvaluationProfile {
        private MyClassifier classifier;
        private FeatureSelection featureSelection;
        private Sampling sampling;
        private CostSensitivity costSensitivity;

        public MyClassifier getClassifier() {
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

        public void setClassifier(MyClassifier classifier) {
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

    private void addResultingEvaluation(Result currentResult, Util.Pair<Evaluation, Double> evaluation) {
        if(evaluation != null) {
            setPerfMetricsForResult(currentResult, evaluation.getFirst());
            currentResult.setNPofB20(evaluation.getSecond());
            results.add(currentResult);
        }
    }

    private Util.Pair<Evaluation, Double> evaluate(
            int numTrainingRels, EvaluationProfile profile, Util.Pair<Instances, Instances> datasets) {
        
        Instances originalTestingSet = new Instances(datasets.getSecond());

        try {
            var resultingPair = obtainTrainedClassifier(
                    profile,
                    datasets.getFirst(),
                    datasets.getSecond());

            var testingSet = resultingPair.getSecond();
            var classifier = resultingPair.getFirst();

            Evaluation eval = new Evaluation(testingSet);
            eval.evaluateModel(classifier, testingSet);
            
            posClassIdx = testingSet.classAttribute().indexOfValue("yes");

            NPofBx npofbx = new NPofBx();
            double nPofBxIndex = npofbx.indexFor(20, testingSet, originalTestingSet, classifier);
            CsvWriter.writeAll(
                getNPofBxFilename(numTrainingRels, profile), 
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

    private Result setEarlyMetricsForResult(int numTrainRels, Util.Pair<Instances, Instances> datasets) {
        var trainingSet = datasets.getFirst();
        var testingSet = datasets.getSecond();

        Result currentResult = new Result();

        currentResult.setDataset(projectName);

        currentResult.setNumTrainingRelease(numTrainRels);

        float percDefTest = (Util.numOfPositives(testingSet)*100) / (float) testingSet.size();
        currentResult.setPercDefectiveInTesting(percDefTest);

        float percDefTrain = (Util.numOfPositives(trainingSet)*100) / (float) trainingSet.size();
        currentResult.setPercDefectiveInTraining(percDefTrain);

        float percTrain = 100 * (trainingSet.numInstances() / (float) (trainingSet.numInstances() + testingSet.numInstances()));
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

    private Util.Pair<Classifier, Instances> obtainTrainedClassifier(
            EvaluationProfile evaluationProfile,
            Instances trainingSet,
            Instances testingSet) throws Exception {

        /*
         * perform feature selection on training set (get best features out of it),
         * make training set and testing set compatible: cut-out same non-selected features
         * from FS search algorithm (applied on TRAINING SET only) on both of them.
         * IMPORTANT: if feature selection is *disabled* in current configuration
         *            curFilteredTrainingSet, curFilteredTestingSet are equal to the original
         *            trainingSet and testingSet, with following instructions having no effect
         *            (classIndex has already been set on testingSet and trainingSet)
         */
        var curFilteredDataset = evaluationProfile.getFeatureSelection().getFilteredDataSets(
            trainingSet, testingSet);

        var curFilteredTrainingSet = curFilteredDataset.getFirst();
        var curFilteredTestingSet = curFilteredDataset.getSecond();

        curFilteredTrainingSet.setClassIndex(curFilteredTrainingSet.numAttributes() - 1);
        curFilteredTestingSet.setClassIndex(curFilteredTestingSet.numAttributes() - 1);

        /*
         * Get base classifier from current evaluation configuration
         */
        var curBaseClassifier = evaluationProfile.getClassifier().buildClassifier();

        /*
         * If sampling enabled, then build a sampling-capable classifier, 
         * otherwise get the same base classifier being passed to this method 
         * (depends on current evaluationProfile)
         */
        var curIntermediateClassifier = evaluationProfile.getSampling().getFilteredClassifier(
            curBaseClassifier, curFilteredTrainingSet);

        /*
         * If cost sensitivity is enabled, then build a cost-sensitive classifier,
         * otherwise get the same intermediate classifier being passed to this method
         * (depends on current evaluationProfile).
         */
        var curFinalClassifier = evaluationProfile.getCostSensitivity().getCostSensititiveClassifier(
            curIntermediateClassifier);

        /*
         * curFinalClassifier's configuration depends on current profile
         * this allows us to experiment easier just by changing/adding configuration profiles
         * (see the configurations array in this class, 
         * just search text for "private final Configuration[] configurations = {")
         * 
         * For sure this enables also "wrong" configurations like SAMPLING+COST SENSITIVITY 
         * (WE DID *NOT* ENABLE BOTH AT THE SAME TIME IN CONFIG)
         * at developer's discretion, however enables easy experimentation
         * 
         * IF NO FS, NO SAMPLING, NO COST SENSITIVITY THEN curFinalClassifier = curBaseClassifier
         * IF YES FS, NO SAMPLING, NO COST SENSITIVITY THEN curFinalClassifier = curBaseClassifier
         * IF NO FS, YES SAMPLING, NO COST SENSITIIVITY THEN curFinalClassifier = FilteredClassifier(wraps curBaseClassifier, enables sampling)
         * IF YES FS, YES SAMPLING, NO COST SENSITIVITY THEN curFinalClassifier = FilteredClassifier(wraps curBaseClassifier, enables sampling)
         * IF NO FS, NO SAMPLING, YES COST SENSITIVITY THEN curFinalClassifier = CostSensitiveClassifier(wraps curBaseClassifier, enables cost-sensitivity)
         * IF YES FS, NO SAMPLING, YES COST SENSITIVITY THEN curFinalClassifier = CostSensitiveClassifier(wraps curBaseClassifier, enables cost-sensitivity)
         * and so on...
         */
        curFinalClassifier.buildClassifier(curFilteredTrainingSet);

        return new Util.Pair<>(curFinalClassifier, curFilteredTestingSet);
    }
}
