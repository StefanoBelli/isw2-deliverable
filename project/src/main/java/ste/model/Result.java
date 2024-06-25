package ste.model;

import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;

@CsvDescriptor
public final class Result {
   private String dataset;
   private int numTrainingRelease;
   private float percTrainingData;
   private float percDefectiveInTraining;
   private float percDefectiveInTesting;
   private String classifier;
   private String balancing;
   private String featureSelection;
   private String sensitivity;
   private int tp;
   private int fp;
   private int tn;
   private int fn;
   private float precision;
   private float recall;
   private float auc;
   private float kappa;
   private float f1score;
   private float nPofB20;

   public Result() {}

   //deepcopy-enabled constructor
   public Result(Result old) {
       auc = old.auc;
       balancing = String.valueOf(old.balancing);
       classifier = String.valueOf(old.classifier);
       dataset = String.valueOf(old.dataset);
       featureSelection = String.valueOf(old.featureSelection);
       fn = old.fn;
       fp = old.fp;
       kappa = old.kappa;
       nPofB20 = old.nPofB20;
       numTrainingRelease = old.numTrainingRelease;
       percDefectiveInTesting = old.percDefectiveInTesting;
       percDefectiveInTraining = old.percDefectiveInTraining;
       percTrainingData = old.percTrainingData;
       precision = old.precision;
       recall = old.recall;
       sensitivity = String.valueOf(old.sensitivity);
       tn = old.tn;
       tp = old.tp;
       f1score = old.f1score;
   }
   
   @CsvColumn(order = 1, name = "Dataset")
   public String getDataset() {
       return dataset;
   }

   @CsvColumn(order = 2, name = "#TrainingReleases")
   public int getNumTrainingRelease() {
       return numTrainingRelease;
   }

   @CsvColumn(order = 3, name = "%TrainingData")
   public float getPercTrainingData() {
       return percTrainingData;
   }

   @CsvColumn(order = 4, name = "%DefectiveTraining")
   public float getPercDefectiveInTraining() {
       return percDefectiveInTraining;
   }
   
   @CsvColumn(order = 5, name = "%DefectiveTesting")
   public float getPercDefectiveInTesting() {
       return percDefectiveInTesting;
   }

   @CsvColumn(order = 6, name = "Classifier")
   public String getClassifier() {
       return classifier;
   }
   
   @CsvColumn(order = 7, name = "FeatureSelection")
   public String getFeatureSelection() {
       return featureSelection;
   }

   @CsvColumn(order = 8, name = "Balancing")
   public String getBalancing() {
       return balancing;
   }

   @CsvColumn(order = 9, name = "Sensitivity")
   public String getSensitivity() {
       return sensitivity;
   }

   @CsvColumn(order = 10, name = "TP")
   public int getTp() {
       return tp;
   }

   @CsvColumn(order = 11, name = "FP")
   public int getFp() {
       return fp;
   }

   @CsvColumn(order = 12, name = "TN")
   public int getTn() {
       return tn;
   }

   @CsvColumn(order = 13, name = "FN")
   public int getFn() {
       return fn;
   }

   @CsvColumn(order = 14, name = "Precision")
   public float getPrecision() {
       return precision;
   }
   
   @CsvColumn(order = 15, name = "Recall")
   public float getRecall() {
       return recall;
   }

   @CsvColumn(order = 16, name = "AUC")
   public float getAuc() {
       return auc;
   }

   @CsvColumn(order = 17, name = "Kappa")
   public float getKappa() {
       return kappa;
   }

   @CsvColumn(order = 18, name = "F1score")
   public float getF1score() {
       return f1score;
   }

   @CsvColumn(order = 19, name = "NPofB20")
   public float getNPofB20() {
       return nPofB20;
   }

   @CsvColumn(order = 20, name = "Cost")
   public int getCost() { 
       return 10 * fn + 1 * fp; 
   }

   public void setAuc(float auc) {
       this.auc = auc;
   }

   public void setBalancing(String balancing) {
       this.balancing = balancing;
   }

   public void setClassifier(String classifier) {
       this.classifier = classifier;
   }

   public void setDataset(String dataset) {
       this.dataset = dataset;
   }

   public void setFeatureSelection(String featureSelection) {
       this.featureSelection = featureSelection;
   }

   public void setFn(int fn) {
       this.fn = fn;
   }

   public void setFp(int fp) {
       this.fp = fp;
   }

   public void setKappa(float kappa) {
       this.kappa = kappa;
   }

   public void setNumTrainingRelease(int numTrainingRelease) {
       this.numTrainingRelease = numTrainingRelease;
   }

   public void setPercDefectiveInTesting(float percDefectiveInTesting) {
       this.percDefectiveInTesting = percDefectiveInTesting;
   }

   public void setPercDefectiveInTraining(float percDefectiveInTraining) {
       this.percDefectiveInTraining = percDefectiveInTraining;
   }

   public void setPercTrainingData(float percTrainingData) {
       this.percTrainingData = percTrainingData;
   }

   public void setPrecision(float precision) {
       this.precision = precision;
   }

   public void setRecall(float recall) {
       this.recall = recall;
   }

   public void setSensitivity(String sensitivity) {
       this.sensitivity = sensitivity;
   }
   
   public void setTn(int tn) {
       this.tn = tn;
   }

   public void setTp(int tp) {
       this.tp = tp;
   }

   public void setNPofB20(float nPofB20) {
       this.nPofB20 = nPofB20;
   }

   public void setF1score(float f1score) {
       this.f1score = f1score;
   }
}
