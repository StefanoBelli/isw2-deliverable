package ste.evaluation.component.fesel.impls;

public final class ApplyBestFirstBackward extends ApplyBestFirst {
    @Override
    protected String[] getBestFirstOptions() {
        return new String[] { "-D", "0" };
    }

    @Override
    public String getName() {
        return "BestFirstBackward";
    }
}

