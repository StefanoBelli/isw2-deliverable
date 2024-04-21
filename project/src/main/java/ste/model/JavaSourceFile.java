package ste.model;

import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;

@CsvDescriptor
public final class JavaSourceFile {
    private Release release;
    private String filename;
    private int loc;
    private int locTouched;
    private int numRev;
    private int numAuthors;
    private int locAdded;
    private int maxLocAdded;
    private int avgLocAdded;
    private int churn;
    private int maxChurn;
    private int avgChurn;
    private boolean buggy;

    @CsvColumn(order = 1, name = "Version")
    public String getVersionByRelease() {
        return release.getVersion();
    }
    
    @CsvColumn(order = 2, name = "Relative path")
    public String getFilename() {
        return filename;
    }
    
    @CsvColumn(order = 3, name = "LOC")
    public int getLoc() {
        return loc;
    }

    @CsvColumn(order = 4, name = "LOC added")
    public int getLocAdded() {
        return locAdded;
    }

    @CsvColumn(order = 5, name = "LOC touched")
    public int getLocTouched() {
        return locTouched;
    }
    
    @CsvColumn(order = 6, name = "Avg LOC added")
    public int getAvgLocAdded() {
        return avgLocAdded;
    }
    
    @CsvColumn(order = 7, name = "Max LOC added")
    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    @CsvColumn(order = 8, name = "Churn")
    public int getChurn() {
        return churn;
    }

    @CsvColumn(order = 9, name = "Avg churn")
    public int getAvgChurn() {
        return avgChurn;
    }

    @CsvColumn(order = 10, name = "Max churn")
    public int getMaxChurn() {
        return maxChurn;
    }

    @CsvColumn(order = 11, name = "Num. of authors")
    public int getNumAuthors() {
        return numAuthors;
    }

    @CsvColumn(order = 12, name = "Num. of revs.")
    public int getNumRev() {
        return numRev;
    }

    @CsvColumn(order = 13, name = "Buggy")
    public String getPrettyIsBuggy() {
        return buggy ? "yes" : "no";
    }

    public Release getRelease() {
        return release;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public void setAvgChurn(int avgChurn) {
        this.avgChurn = avgChurn;
    }

    public void setAvgLocAdded(int avgLocAdded) {
        this.avgLocAdded = avgLocAdded;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public void setLoc(int loc) {
        this.loc = loc;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public void setLocTouched(int locTouched) {
        this.locTouched = locTouched;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public void setNumAuthors(int numAuthors) {
        this.numAuthors = numAuthors;
    }

    public void setNumRev(int numRev) {
        this.numRev = numRev;
    }

    public void setRelease(Release release) {
        this.release = release;
    }
}
