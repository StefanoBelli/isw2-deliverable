package ste.evaluation.component.sampling;

import weka.core.Instances;
import weka.filters.Filter;

public interface ApplyFilter {
    public Filter getFilter(Instances i) throws Exception;
    public String getName(); 
}
