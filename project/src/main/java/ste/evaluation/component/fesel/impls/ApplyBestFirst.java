package ste.evaluation.component.fesel.impls;

import ste.evaluation.component.fesel.ApplyFeatureSelection;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.BestFirst;

abstract class ApplyBestFirst implements ApplyFeatureSelection {
    protected abstract String[] getBestFirstOptions();
    public abstract String getName();

    @Override
    public final ASSearch getSearch() {
        BestFirst bestFirst = new BestFirst();
        try {
            bestFirst.setOptions(getBestFirstOptions());
        } catch (Exception e) {
            return null;
        }

        return bestFirst;
    }
}
