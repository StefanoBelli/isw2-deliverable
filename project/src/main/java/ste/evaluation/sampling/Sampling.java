package ste.evaluation.sampling;

import ste.Util;
import ste.evaluation.NamedEvaluationComponent;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.Filter;

public final class Sampling implements NamedEvaluationComponent {
    private final AbstractClassifier classifier;
    private final ApplyFilter applyFilter;
    private final Instances trainingSet;

    protected Sampling(AbstractClassifier classifier, ApplyFilter applyFilter, Instances trainingSet) {
        this.classifier = classifier;
        this.applyFilter = applyFilter;
        this.trainingSet = trainingSet;
    }

    public final Util.Pair<AbstractClassifier,Instances> getFilteredClassifierWithSampledTrainingSet() {
        if(applyFilter == null) {
            return new Util.Pair<>(classifier, trainingSet);
        }

        Filter usedFilter;
        FilteredClassifier fc = new FilteredClassifier();
        Instances fi;

        fc.setClassifier(classifier);

        try {
            usedFilter = applyFilter.getFilter(trainingSet);
            fi = Filter.useFilter(trainingSet, usedFilter);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }

        fc.setFilter(usedFilter);

        return new Util.Pair<>(fc, fi);
    }
    
    @Override
    public String getName() {
        if(applyFilter == null) {
            return "none";
        }

        return applyFilter.getName();
    }
}
