package ste.evaluation.component.fesel.impls;

import org.slf4j.LoggerFactory;

import ste.evaluation.component.fesel.ApplyFeatureSelection;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.BestFirst;

public final class ApplyBestFirstBackward implements ApplyFeatureSelection {

    @Override
    public ASSearch getSearch() {
        BestFirst bestFirst = new BestFirst();
        try {
            bestFirst.setOptions(new String[]{ "-D", "0" });
        } catch(Exception e) {
            LoggerFactory
                .getLogger(getClass())
                .warn(String.format("setOptions exception: %s", e.getMessage()));
            return null;
        }

        return bestFirst;
    }

    @Override
    public String getName() {
        return "BestFirstBackward";
    }
    
}
