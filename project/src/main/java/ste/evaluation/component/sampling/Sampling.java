package ste.evaluation.component.sampling;

import ste.evaluation.component.NamedEvaluationComponent;
import weka.core.Instances;
import weka.filters.Filter;

public final class Sampling implements NamedEvaluationComponent {
    private final ApplyFilter applyFilter;

    public Sampling(ApplyFilter applyFilter) {
        this.applyFilter = applyFilter;
    }

    public final Instances getFilteredTrainingSet(Instances trainingSet) throws Exception {
        if(applyFilter == null) {
            return trainingSet;
        }

        return Filter.useFilter(trainingSet, applyFilter.getFilter(trainingSet));
    }
    
    @Override
    public String getName() {
        if(applyFilter == null) {
            return "None";
        }

        return applyFilter.getName();
    }
}
