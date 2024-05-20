package ste.evaluation.fesel;

import weka.attributeSelection.ASSearch;

public interface ApplyFeatureSelection {
    public ASSearch getSearch();
    public String getName(); 
}
