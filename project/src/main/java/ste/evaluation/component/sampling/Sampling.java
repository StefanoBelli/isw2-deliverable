package ste.evaluation.component.sampling;

import ste.evaluation.component.NamedEvaluationComponent;
import weka.core.Instances;
import weka.filters.Filter;

public final class Sampling implements NamedEvaluationComponent {
    private final ApplyFilter applyFilter;

    public Sampling(ApplyFilter applyFilter) {
        this.applyFilter = applyFilter;
    }

    public final Instances getFilteredTrainingSet(Instances trainingSet) {
        if(applyFilter == null) {
            return trainingSet;
        }

        try {
            return Filter.useFilter(trainingSet, applyFilter.getFilter(trainingSet));
        } catch(Exception e) {
            return null;
        }
    }
    
    @Override
    public String getName() {
        if(applyFilter == null) {
            return "None";
        }

        return applyFilter.getName();
    }
}
