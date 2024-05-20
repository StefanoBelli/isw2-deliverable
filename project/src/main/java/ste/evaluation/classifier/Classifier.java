package ste.evaluation.classifier;

import ste.evaluation.NamedEvaluationComponent;
import weka.classifiers.AbstractClassifier;

public interface Classifier extends NamedEvaluationComponent {
    AbstractClassifier getClassifier();
}
