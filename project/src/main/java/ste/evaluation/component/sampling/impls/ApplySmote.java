package ste.evaluation.component.sampling.impls;

import ste.Util;
import ste.evaluation.component.sampling.ApplyFilter;
import ste.evaluation.component.sampling.exception.ApplyFilterException;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;

public final class ApplySmote implements ApplyFilter {
    @Override
    public Filter getFilter(Instances insts) throws ApplyFilterException {
        SMOTE smote = new SMOTE();

        try {
            int numPos = Util.numOfPositives(insts);
            int numNeg = Util.numOfNegatives(insts);
            int synthPerc = 100 * (numNeg - numPos) / numPos;
            smote.setPercentage(synthPerc);
            smote.setInputFormat(insts);
        } catch(Exception e) {
            throw new ApplyFilterException(e);
        }

        return smote;
    }

    @Override
    public String getName() {
        return "SMOTE";
    }
    
}
