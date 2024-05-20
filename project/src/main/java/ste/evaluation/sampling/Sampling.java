package ste.evaluation.sampling;

import ste.evaluation.NamedEvaluationComponent;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.meta.FilteredClassifier;

public final class Sampling implements NamedEvaluationComponent {
    private final AbstractClassifier classifier;
    private final ApplyFilter applyFilter;

    protected Sampling(AbstractClassifier classifier, ApplyFilter applyFilter) {
        this.classifier = classifier;
        this.applyFilter = applyFilter;
    }

    public final AbstractClassifier getSampling() {
        if(applyFilter == null) {
            return classifier;
        }

        FilteredClassifier fc = new FilteredClassifier();
        fc.setClassifier(classifier);
        try {
            fc.setFilter(applyFilter.getFilter());
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }

        return fc;
    }
    
    @Override
    public String getName() {
        if(applyFilter == null) {
            return "none";
        }

        return applyFilter.getName();
    }
}
