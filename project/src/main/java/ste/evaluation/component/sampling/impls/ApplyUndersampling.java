package ste.evaluation.component.sampling.impls;

import ste.evaluation.component.sampling.ApplyFilter;
import ste.evaluation.component.sampling.exception.ApplyFilterException;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.SpreadSubsample;

public final class ApplyUndersampling implements ApplyFilter {

    @Override
    public Filter getFilter(Instances insts) throws ApplyFilterException {
        SpreadSubsample spreadSubsample = new SpreadSubsample();

        try {
            spreadSubsample.setInputFormat(insts);
        } catch (Exception e) {
            throw new ApplyFilterException(e);
        }

        String[] opts = new String[] { "-M", "1.0" };

        try {
            spreadSubsample.setOptions(opts);
        } catch(Exception e) {
            throw new ApplyFilterException(e);
        }

        return spreadSubsample;
    }

    @Override
    public String getName() {
        return "Undersampling";
    }
    
}
