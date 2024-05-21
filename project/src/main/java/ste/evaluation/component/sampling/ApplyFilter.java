package ste.evaluation.component.sampling;

import ste.evaluation.component.sampling.exception.ApplyFilterException;
import weka.core.Instances;
import weka.filters.Filter;

public interface ApplyFilter {
    public Filter getFilter(Instances i) throws ApplyFilterException;
    public String getName(); 
}
