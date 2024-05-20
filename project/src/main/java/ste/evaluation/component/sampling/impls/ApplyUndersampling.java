package ste.evaluation.component.sampling.impls;

import ste.evaluation.component.sampling.ApplyFilter;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.SpreadSubsample;

public final class ApplyUndersampling implements ApplyFilter {

    @Override
    public Filter getFilter(Instances insts) throws Exception {
        SpreadSubsample spreadSubsample = new SpreadSubsample();
        spreadSubsample.setInputFormat(insts);
        String[] opts = new String[] { "-M", "1.0" };
        spreadSubsample.setOptions(opts);

        return spreadSubsample;
    }

    @Override
    public String getName() {
        return "undersampling";
    }
    
}
