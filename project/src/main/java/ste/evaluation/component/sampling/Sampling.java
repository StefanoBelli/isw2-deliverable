package ste.evaluation.component.sampling;

import ste.evaluation.component.NamedEvaluationComponent;
import ste.evaluation.component.sampling.exception.ApplyFilterException;
import weka.classifiers.Classifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;

public final class Sampling implements NamedEvaluationComponent {
    private final ApplyFilter applyFilter;

    public Sampling(ApplyFilter applyFilter) {
        this.applyFilter = applyFilter;
    }

    public final Classifier getFilteredClassifier(Classifier orig, Instances trainingSet) throws ApplyFilterException {
        if(applyFilter == null) {
            return orig;
        }

        FilteredClassifier filteredClassifier = new FilteredClassifier();
        filteredClassifier.setClassifier(orig);
        filteredClassifier.setFilter(applyFilter.getFilter(trainingSet));
        
        return filteredClassifier;
    }

    
    @Override
    public String getName() {
        if(applyFilter == null) {
            return "None";
        }

        return applyFilter.getName();
    }
}
