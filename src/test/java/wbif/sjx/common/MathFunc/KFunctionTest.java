package wbif.sjx.common.MathFunc;

import org.junit.Ignore;
import org.junit.Test;
import wbif.sjx.common.Analysis.KFunctionCalculator;
import wbif.sjx.common.ExpectedObjects.Clusters2D;
import wbif.sjx.common.ExpectedObjects.Clusters3D;
import wbif.sjx.common.Object.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class KFunctionTest {
    private double tolerance = 1E-4;


    // GENERAL TESTS

    @Test
    public void testCalculateMaximumPointSeparation2D() throws Exception {
        ArrayList<Point<Double>> centroids = Clusters2D.getCentroids();
        assertNotNull(centroids);

        double actual = KFunctionCalculator.calculateMaximumPointSeparation(centroids);
        double expected = 102.03;

        assertEquals(expected,actual,tolerance*expected);

    }

    @Test
    public void testCalculateRegionSize2D() throws Exception {
        ArrayList<Point<Double>> centroids = Clusters2D.getCentroids();
        assertNotNull(centroids);

        double actual = KFunctionCalculator.calculateRegionSize(centroids,true);
        double expected = 9177.659336;

        assertEquals(expected,actual,tolerance*expected);

    }

    @Test
    public void testCalculateStepSize2D() throws Exception {
        double regionSize = 9177.659336;
        int nBins = 97;

        double actual = KFunctionCalculator.calculateStepSize(regionSize,nBins,true);
        double expected = 0.329209969;

        assertEquals(expected,actual,tolerance*expected);

    }


    // TESTS FOR 2D OBJECTS

    /**
     * Compared to values from RipleyGUI with a tolerance of 2% the expected value.
     * @throws Exception
     */
    @Test
    public void testGetKFunction2DWithoutCorrection() throws Exception {
        ArrayList<Point<Double>> centroids = Clusters2D.getCentroids();
        assertNotNull(centroids);

        int nBins = 100;

        KFunctionCalculator calculator = new KFunctionCalculator(centroids,nBins,true,false);
        TreeMap<Double,Double> actual = calculator.getKFunction();
        TreeMap<Double,Double> expected = Clusters2D.getKFunctionWithoutCorrection();

        assertNotNull(expected);

        Iterator<Double> actualIterator = actual.keySet().iterator();

        for (Double expectedTs : expected.keySet()) {
            double actualTs = actualIterator.next();

            assertEquals(expectedTs, actualTs, tolerance);
            assertEquals(expected.get(expectedTs), actual.get(actualTs), expected.get(expectedTs)*0.02);

        }
    }

    /**
     * Compared to values from RipleyGUI with a tolerance of 10% the expected value.  The tolerance is larger here
     * because edge correction approaches differ between the two pieces of software.
     * @throws Exception
     */
    @Test
    public void testGetKFunction2DWithCorrection() throws Exception {
        ArrayList<Point<Double>> centroids = Clusters2D.getCentroids();
        assertNotNull(centroids);

        int nBins = 100;

        KFunctionCalculator calculator = new KFunctionCalculator(centroids,nBins,true,true);
        TreeMap<Double,Double> actual = calculator.getKFunction();
        TreeMap<Double,Double> expected = Clusters2D.getKFunctionWithCorrection();

        assertNotNull(expected);

        Iterator<Double> actualIterator = actual.keySet().iterator();

        for (Double expectedTs : expected.keySet()) {
            double actualTs = actualIterator.next();

            assertEquals(expectedTs, actualTs, tolerance);
            assertEquals(expected.get(expectedTs), actual.get(actualTs), expected.get(expectedTs)*0.1);

        }
    }

    /**
     * Compared to values from CrimeStat IV with a tolerance of 2% the expected value.
     * @throws Exception
     */
    @Test
    public void testGetLFunction2DWithoutCorrection() throws Exception {
        ArrayList<Point<Double>> centroids = Clusters2D.getCentroids();
        assertNotNull(centroids);

        int nBins = 100;

        KFunctionCalculator calculator = new KFunctionCalculator(centroids,nBins,true,false);
        TreeMap<Double,Double> actual = calculator.getLFunction(true);
        TreeMap<Double,Double> expected = Clusters2D.getLFunctionWithoutCorrection();

        assertNotNull(expected);

        Iterator<Double> actualIterator = actual.keySet().iterator();

        for (Double expectedTs : expected.keySet()) {
            double actualTs = actualIterator.next();

            assertEquals(expectedTs, actualTs, tolerance);
            assertEquals(expected.get(expectedTs), actual.get(actualTs), Math.abs(expected.get(expectedTs))*0.02);

        }
    }

    /**
     * Compared to values from CrimeStat IV with a tolerance of 2% the expected value.
     * @throws Exception
     */
    @Test
    public void testGetLFunction2DWithCorrection() throws Exception {
        // Don't currently have any data to compare this to, as I've yet to find software the explicitly implements
        // the same correction approach.  CrimeStat has an additional log transform and also appears to use different
        // equations.
        ArrayList<Point<Double>> centroids = Clusters2D.getCentroids();
        assertNotNull(centroids);

        int nBins = 100;

        KFunctionCalculator calculator = new KFunctionCalculator(centroids,nBins,true,true);
        TreeMap<Double,Double> actual = calculator.getLFunction(true);
        TreeMap<Double,Double> expected = Clusters2D.getLFunctionWithCorrection();

        assertNotNull(expected);

        Iterator<Double> actualIterator = actual.keySet().iterator();

        for (Double expectedTs : expected.keySet()) {
            double actualTs = actualIterator.next();

            assertEquals(expectedTs, actualTs, tolerance);
            assertEquals(expected.get(expectedTs), actual.get(actualTs), Math.abs(expected.get(expectedTs))*0.1);

        }
    }


    // TESTS FOR 3D OBJECTS

    /**
     * Compared to values from RipleyGUI with a tolerance of 2% the expected value.
     * @throws Exception
     */
    @Test
    public void testGetKFunction3DWithoutCorrection() throws Exception {
        // Don't currently have any data to compare this to, as I've yet to find software the explicitly implements
        // the same correction approach.  CrimeStat has an additional log transform and also appears to use different
        // equations.
        ArrayList<Point<Double>> centroids = Clusters3D.getCentroids();
        assertNotNull(centroids);

        int nBins = 97;
        double minBin = 1;
        double maxBin = 25;

        KFunctionCalculator calculator = new KFunctionCalculator(centroids,nBins,minBin,maxBin,false,false);
        TreeMap<Double,Double> actual = calculator.getKFunction();
        TreeMap<Double,Double> expected = Clusters3D.getKFunctionWithoutCorrection();

        assertNotNull(expected);

        Iterator<Double> actualIterator = actual.keySet().iterator();

        for (Double expectedTs : expected.keySet()) {
            double actualTs = actualIterator.next();

            assertEquals(expectedTs, actualTs, tolerance);
            assertEquals(expected.get(expectedTs), actual.get(actualTs), expected.get(expectedTs)*0.02);

        }
    }

    /**
     * Compared to values from RipleyGUI with a tolerance of 10% the expected value.  The
     * @throws Exception
     */
    @Test @Ignore
    public void testGetKFunction3DWithCorrection() throws Exception {
        ArrayList<Point<Double>> centroids = Clusters3D.getCentroids();
        assertNotNull(centroids);

        int nBins = 97;
        double minBin = 1;
        double maxBin = 25;

        KFunctionCalculator calculator = new KFunctionCalculator(centroids,nBins,minBin,maxBin,false,true);
        TreeMap<Double,Double> actual = calculator.getKFunction();
        TreeMap<Double,Double> expected = Clusters3D.getKFunctionWithCorrection();

        assertNotNull(expected);

        Iterator<Double> actualIterator = actual.keySet().iterator();

        for (Double expectedTs : expected.keySet()) {
            double actualTs = actualIterator.next();

            System.out.println(expectedTs+"_"+expected.get(expectedTs)+"_"+actual.get(actualTs));

            assertEquals(expectedTs, actualTs, tolerance);
            assertEquals(expected.get(expectedTs), actual.get(actualTs), expected.get(expectedTs)*0.1);

        }
    }
}