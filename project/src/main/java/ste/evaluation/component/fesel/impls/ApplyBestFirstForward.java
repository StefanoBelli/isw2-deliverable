package ste.evaluation.component.fesel.impls;

import ste.evaluation.component.fesel.ApplyFeatureSelection;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.BestFirst;

public final class ApplyBestFirstForward implements ApplyFeatureSelection {

    @Override
    public ASSearch getSearch() {
        BestFirst bestFirst = new BestFirst();
        try {
            bestFirst.setOptions(new String[] { "-D", "1"});
        } catch (Exception e) {
            return null;
        }

        return bestFirst;
    }

    @Override
    public String getName() {
        return "BestFirstForward";
    }
    
}
