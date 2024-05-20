package ste.evaluation.classifier.impls;

import ste.evaluation.classifier.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.lazy.IBk;

public final class IBkClassifier implements Classifier {

    @Override
    public String getName() {
        return "IBk";
    }

    @Override
    public AbstractClassifier getClassifier() {
        return new IBk();
    }
    
}
