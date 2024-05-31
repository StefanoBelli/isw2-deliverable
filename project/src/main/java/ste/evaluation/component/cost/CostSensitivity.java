package ste.evaluation.component.cost;

import ste.evaluation.component.NamedEvaluationComponent;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.meta.CostSensitiveClassifier;

public final class CostSensitivity implements NamedEvaluationComponent {
    private final Sensitive sens;

    public CostSensitivity(Sensitive sens) {
        this.sens = sens;
    }

    public AbstractClassifier getCostSensititiveClassifier(AbstractClassifier classifier) {
        if(sens == Sensitive.NONE) {
            return classifier;
        }

        CostSensitiveClassifier csc = new CostSensitiveClassifier();
        csc.setClassifier(classifier);
        csc.setMinimizeExpectedCost(sens == Sensitive.THRESHOLD);
        csc.setCostMatrix(buildCostMatrix(1f, 10f));

        return csc;
    }

    @Override
    public String getName() {
        switch(sens) {
            case LEARNING:
            return "Learning";
            case NONE:
            return "None";
            case THRESHOLD:
            return "Threshold";
        }

        return "";
    }

    private static CostMatrix buildCostMatrix(double fp, double fn) {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, fp);
        costMatrix.setCell(0, 1, fn);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }
}
