package ste.evaluation.component.classifier.impls;

import ste.evaluation.component.classifier.MyClassifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.RandomForest;

public final class RandomForestClassifier implements MyClassifier {

    @Override
    public String getName() {
        return "RandomForest";
    }

    @Override
    public AbstractClassifier buildClassifier() {
        return new RandomForest();
    }
    
}
