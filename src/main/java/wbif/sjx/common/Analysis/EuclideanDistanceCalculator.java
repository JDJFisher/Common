package wbif.sjx.common.Analysis;

import java.util.TreeMap;

/**
 * Created by steph on 15/04/2017.
 */
public class EuclideanDistanceCalculator implements SpatialCalculator {
    public TreeMap<Integer,Double> calculate(int[] f, double[] x, double[] y, double[] z) {
        TreeMap<Integer,Double> dist = new TreeMap<>();

        dist.put(f[0],0d);
        for (int i=0;i<x.length;i++) {
            double dx = x[i]-x[0];
            double dy = y[i]-y[0];
            double dz = z[i]-z[0];

            dist.put(f[i],Math.sqrt(dx*dx + dy*dy + dz*dz));
        }

        return dist;

    }
}
