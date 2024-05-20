package ste.evaluation.sampling.impls;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import ste.evaluation.sampling.ApplyFilter;

public final class ApplySmote implements ApplyFilter {
    @Override
    public Filter getFilter(Instances insts) throws Exception {
        SMOTE smote = new SMOTE();
        smote.setInputFormat(insts);
        return smote;
    }

    @Override
    public String getName() {
        return "SMOTE";
    }
    
}
