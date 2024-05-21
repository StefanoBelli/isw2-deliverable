package ste.evaluation.component.sampling.impls;

import ste.Util;
import ste.evaluation.component.sampling.ApplyFilter;
import ste.evaluation.component.sampling.exception.ApplyFilterException;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;

public final class ApplyOversampling implements ApplyFilter {

    @Override
    public Filter getFilter(Instances insts) throws ApplyFilterException {
        Resample resampler = new Resample();

        try {
            resampler.setInputFormat(insts);
        } catch(Exception e) {
            throw new ApplyFilterException(e);
        }

        resampler.setBiasToUniformClass(1.0f);

        int numPos = Util.getNumOfPositiveInstances(insts);
        double percPosInsts = (numPos * 100) / ((float) insts.size());
        resampler.setSampleSizePercent(percPosInsts * 2);

        return resampler;
    }

    @Override
    public String getName() {
        return "oversampling";
    }
    
}
