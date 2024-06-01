package ste.evaluation.component.sampling.impls;

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

        resampler.setNoReplacement(false);
        resampler.setBiasToUniformClass(1f);
        resampler.setSampleSizePercent(SampleSizePercent.byInstances(insts));

        return resampler;
    }

    @Override
    public String getName() {
        return "Oversampling";
    }
    
}
