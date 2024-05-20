package ste.evaluation.component.classifier;

import ste.evaluation.component.NamedEvaluationComponent;
import weka.classifiers.AbstractClassifier;

public interface Classifier extends NamedEvaluationComponent {
    AbstractClassifier getClassifier();
}
