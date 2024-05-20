package ste.evaluation.component.cost;

import ste.evaluation.component.NamedEvaluationComponent;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.meta.CostSensitiveClassifier;

public final class CostSensitivity implements NamedEvaluationComponent {
    private final AbstractClassifier classifier;
    private final Sensitive sens;

    public CostSensitivity(AbstractClassifier classifier, Sensitive sens) {
        this.classifier = classifier;
        this.sens = sens;
    }

    public AbstractClassifier getCostSensititiveClassifier() {
        if(sens == Sensitive.NONE) {
            return classifier;
        }

        CostSensitiveClassifier csc = new CostSensitiveClassifier();
        csc.setClassifier(classifier);
        csc.setMinimizeExpectedCost(sens == Sensitive.THRESHOLD);
        csc.setCostMatrix(buildCostMatrix(10.0, 1.0));

        return csc;
    }

    @Override
    public String getName() {
        switch(sens) {
            case LEARNING:
            return "learning";
            case NONE:
            return "none";
            case THRESHOLD:
            return "threshold";
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
