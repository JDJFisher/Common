package wbif.sjx.common.Analysis.SpatialCalculators;

import wbif.sjx.common.Object.Track;

import java.util.TreeMap;

/**
 * Created by sc13967 on 20/01/2018.
 */
public interface SpatialCalculator {
    public default TreeMap<Integer,Double> calculate(Track track) {
        return calculate(track.getX(),track.getY(),track.getZ(),track.getF());
    }

    public TreeMap<Integer,Double> calculate(double[] x, double[] y, double[] z, int[] f);
}
