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
   private float tp;
   private float fp;
   private float tn;
   private float fn;
   private float precision;
   private float recall;
   private float auc;
   private float kappa;
   private float nPofB20;

   public Result() {}

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
   public float getTp() {
       return tp;
   }

   @CsvColumn(order = 11, name = "FP")
   public float getFp() {
       return fp;
   }

   @CsvColumn(order = 12, name = "TN")
   public float getTn() {
       return tn;
   }

   @CsvColumn(order = 13, name = "FN")
   public float getFn() {
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

   @CsvColumn(order = 18, name = "NPofB20")
   public float getNPofB20() {
       return nPofB20;
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

   public void setFn(float fn) {
       this.fn = fn;
   }

   public void setFp(float fp) {
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
   
   public void setTn(float tn) {
       this.tn = tn;
   }

   public void setTp(float tp) {
       this.tp = tp;
   }

   public void setNPofB20(float nPofB20) {
       this.nPofB20 = nPofB20;
   }
}
