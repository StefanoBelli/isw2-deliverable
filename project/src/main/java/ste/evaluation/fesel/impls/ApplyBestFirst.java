package ste.evaluation.fesel.impls;

import ste.evaluation.fesel.ApplyFeatureSelection;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.BestFirst;

public final class ApplyBestFirst implements ApplyFeatureSelection {

    @Override
    public ASSearch getSearch() {
        return new BestFirst();
    }

    @Override
    public String getName() {
        return "Best first";
    }
    
}
