package ste.evaluation.component.classifier.impls;

import ste.evaluation.component.classifier.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.RandomForest;

public final class RandomForestClassifier implements Classifier {

    @Override
    public String getName() {
        return "RandomForest";
    }

    @Override
    public AbstractClassifier getClassifier() {
        return new RandomForest();
    }
    
}
