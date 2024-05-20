package ste.evaluation.classifier.impls;

import ste.evaluation.classifier.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;

public class NaiveBayesClassifier implements Classifier {

    @Override
    public String getName() {
        return "NaiveBayes";
    }

    @Override
    public AbstractClassifier getClassifier() {
        return new NaiveBayes();
    }
    
}
