package ste.evaluation.fesel.impls;

import ste.evaluation.fesel.ApplyFeatureSelection;
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
        return "Backward search";
    }
    
}
