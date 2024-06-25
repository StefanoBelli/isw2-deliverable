package ste.evaluation;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class WalkForwardSplitIterator implements Iterator<WalkForwardSplit> {
    private final List<WalkForwardSplit> walkForwardSplits;
    private final int numOfTotalWalkForwardSplits;
    private int curIdx = 0;

    public WalkForwardSplitIterator(List<WalkForwardSplit> walkForwardSplits) {
        this.walkForwardSplits = walkForwardSplits;
        this.numOfTotalWalkForwardSplits = walkForwardSplits.size();
    }
    
    @Override
    public boolean hasNext() {
        return curIdx + 1 <= numOfTotalWalkForwardSplits; 
    }

    @Override
    public WalkForwardSplit next() {
        if(curIdx >= numOfTotalWalkForwardSplits) {
            throw new NoSuchElementException();
        }

        WalkForwardSplit nextSplit = walkForwardSplits.get(curIdx);
        ++curIdx;

        return nextSplit;
    }

    public int getNumOfTotalWalkForwardSplits() {
        return numOfTotalWalkForwardSplits;
    }
    
}
