package ste.evaluation.sampling.impls;

import ste.Util;
import ste.evaluation.sampling.ApplyFilter;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;

public final class ApplyOversampling implements ApplyFilter {

    @Override
    public Filter getFilter(Instances insts) throws Exception {
        Resample resampler = new Resample();
        resampler.setInputFormat(insts);
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
