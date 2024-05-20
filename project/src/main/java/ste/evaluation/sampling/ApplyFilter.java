package ste.evaluation.sampling;

import weka.filters.Filter;

public interface ApplyFilter {
    public Filter getFilter() throws Exception;
    public String getName(); 
}
