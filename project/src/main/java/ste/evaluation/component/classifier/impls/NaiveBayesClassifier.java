package ste.evaluation.component.classifier.impls;

import ste.evaluation.component.classifier.MyClassifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;

public class NaiveBayesClassifier implements MyClassifier {

    @Override
    public String getName() {
        return "NaiveBayes";
    }

    @Override
    public AbstractClassifier buildClassifier() {
        return new NaiveBayes();
    }
    
}
