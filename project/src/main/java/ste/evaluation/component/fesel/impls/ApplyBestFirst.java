package ste.evaluation.component.fesel.impls;

import ste.evaluation.component.fesel.ApplyFeatureSelection;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.BestFirst;

public final class ApplyBestFirst implements ApplyFeatureSelection {

    @Override
    public ASSearch getSearch() {
        return new BestFirst();
    }

    @Override
    public String getName() {
        return "BestFirst";
    }
    
}
