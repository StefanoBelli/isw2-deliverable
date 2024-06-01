package ste.evaluation.component.fesel.impls;

public final class ApplyBestFirstForward extends ApplyBestFirst {

    @Override
    protected String[] getBestFirstOptions() {
        return new String[] { "-D", "1" };
    }

    @Override
    public String getName() {
        return "BestFirstForward";
    }
    
}
