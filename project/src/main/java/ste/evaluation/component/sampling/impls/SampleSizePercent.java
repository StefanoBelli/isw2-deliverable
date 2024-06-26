package ste.evaluation.component.sampling.impls;

import ste.Util;
import weka.core.Instances;

public final class SampleSizePercent {
    private SampleSizePercent() {}

    public static double byInstances(Instances i) {
        int minority = Util.numOfPositives(i);
        int majority = Util.numOfNegatives(i);

        if(majority < minority) {
            int tmp = minority;
            minority = majority;
            majority = tmp;
        }

        return 100f * (majority - minority) / (double) minority;
    }
}
