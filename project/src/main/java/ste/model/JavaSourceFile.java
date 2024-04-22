package ste.model;

import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;

@CsvDescriptor
public final class JavaSourceFile {
    private Release release;
    private String filename;
    private long loc;
    private int avgChgSet;
    private int maxChgSet;
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
    public long getLoc() {
        return loc;
    }

    @CsvColumn(order = 4, name = "LOC added")
    public int getLocAdded() {
        return locAdded;
    }

    @CsvColumn(order = 5, name = "Avg LOC added")
    public int getAvgLocAdded() {
        return avgLocAdded;
    }
    
    @CsvColumn(order = 6, name = "Max LOC added")
    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    @CsvColumn(order = 7, name = "Churn")
    public int getChurn() {
        return churn;
    }

    @CsvColumn(order = 8, name = "Avg churn")
    public int getAvgChurn() {
        return avgChurn;
    }

    @CsvColumn(order = 9, name = "Max churn")
    public int getMaxChurn() {
        return maxChurn;
    }

    @CsvColumn(order = 10, name = "Avg chg set")
    public int getAvgChgSet() {
        return avgChgSet;
    }

    @CsvColumn(order = 11, name = "Max chg set")
    public int getMaxChgSet() {
        return maxChgSet;
    }

    @CsvColumn(order = 12, name = "Num. of authors")
    public int getNumAuthors() {
        return numAuthors;
    }

    @CsvColumn(order = 13, name = "Num. of revs.")
    public int getNumRev() {
        return numRev;
    }

    @CsvColumn(order = 14, name = "Buggy")
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
    
    public void setLoc(long loc) {
        this.loc = loc;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public void setAvgChgSet(int avgChgSet) {
        this.avgChgSet = avgChgSet;
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

    public void setMaxChgSet(int maxChgSet) {
        this.maxChgSet = maxChgSet;
    }

    public static JavaSourceFile build(String filename, Release release) {
        JavaSourceFile jsf = new JavaSourceFile();
        jsf.setFilename(filename);
        jsf.setRelease(release);
        jsf.setBuggy(false);

        return jsf;
    }
}
