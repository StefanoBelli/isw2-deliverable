package ste.evaluation.component.fesel.impls;

import ste.evaluation.component.fesel.ApplyFeatureSelection;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.GreedyStepwise;

public final class ApplyBackwardSearch implements ApplyFeatureSelection {

    @Override
    public ASSearch getSearch() {
        GreedyStepwise s = new GreedyStepwise();
        s.setSearchBackwards(true);
        return s;
    }

    @Override
    public String getName() {
        return "BackwardSearch";
    }
    
}
