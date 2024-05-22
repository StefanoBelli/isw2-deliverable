package ste.evaluation.component.classifier.impls;

import ste.evaluation.component.classifier.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;

public class NaiveBayesClassifier implements Classifier {

    @Override
    public String getName() {
        return "NaiveBayes";
    }

    @Override
    public AbstractClassifier buildClassifier() {
        return new NaiveBayes();
    }
    
}
