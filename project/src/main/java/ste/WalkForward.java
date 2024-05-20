package ste;

public final class WalkForward {

    private static String getTrainSetArffFilename(String proj) {
        return String.format("%s-train-tmp.arff", proj);
    }

    private static String getTestSetArffFilename(String proj) {
        return String.format("%s-test-tmp.arff", proj);
    }

    private final String stormDataset;
    private final String bookKeeperDataset;
    private final String stormProjectName;
    private final String bookKeeperProjectName;

    private final 

    public WalkForward(
            String stormProjectName, 
            String bookKeeperProjectName, 
            String stormDataset, 
            String bookKeeperDataset) {

        this.stormDataset = stormDataset;
        this.bookKeeperDataset = bookKeeperDataset;
        this.stormProjectName = stormProjectName;
        this.bookKeeperProjectName = bookKeeperProjectName;
    }

    public void start() {

    }
}
