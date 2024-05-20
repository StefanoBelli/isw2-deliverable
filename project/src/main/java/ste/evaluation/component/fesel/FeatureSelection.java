package ste.evaluation.component.fesel;

import weka.attributeSelection.CfsSubsetEval;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

import ste.Util;
import ste.evaluation.component.NamedEvaluationComponent;

public final class FeatureSelection implements NamedEvaluationComponent {
    private final ApplyFeatureSelection applyFeatureSelection;

    public FeatureSelection(ApplyFeatureSelection applyFeatureSelection) {
        this.applyFeatureSelection = applyFeatureSelection;
    }
    
    public Util.Pair<Instances, Instances> getFilteredDataSets(Instances training, Instances testing) throws Exception {
        if(applyFeatureSelection == null) {
            return new Util.Pair<>(training, testing);
        }

        AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval evaluator = new CfsSubsetEval();

        filter.setEvaluator(evaluator);
        filter.setSearch(applyFeatureSelection.getSearch());
        
        filter.setInputFormat(training);

        Instances filteredTraining = Filter.useFilter(training, filter);
        Instances filteredTesting = Filter.useFilter(testing, filter);

        int numAttrFiltered = filteredTraining.numAttributes();
        filteredTraining.setClassIndex(numAttrFiltered - 1);
        filteredTesting.setClassIndex(numAttrFiltered - 1);

        return new Util.Pair<>(filteredTraining, filteredTesting);
    }

    @Override
    public String getName() {
        if(applyFeatureSelection == null) {
            return "none";
        }

        return applyFeatureSelection.getName();
    }
}
