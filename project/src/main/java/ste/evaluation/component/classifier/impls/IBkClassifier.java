package ste.evaluation.component.classifier.impls;

import ste.evaluation.component.classifier.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.lazy.IBk;

public final class IBkClassifier implements Classifier {

    @Override
    public String getName() {
        return "IBk";
    }

    @Override
    public AbstractClassifier buildClassifier() {
        return new IBk();
    }
    
}
