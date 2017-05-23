// TODO: Add TrackMate measurements to measurement outputs

package wbif.sjx.common.HighContent.Module;

import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTrackerFactory;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import wbif.sjx.common.HighContent.Object.*;
import wbif.sjx.common.MathFunc.CumStat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by sc13967 on 15/05/2017.
 */
public class RunTrackMate extends HCModule {
    public static final String INPUT_IMAGE = "Input image";
    public static final String OUTPUT_SPOT_OBJECTS = "Output spot objects";
    public static final String CALIBRATED_UNITS = "Calibrated radius";
    public static final String DO_SUBPIXEL_LOCALIZATION = "Do sub-pixel localisation";
    public static final String DO_MEDIAN_FILTERING = "Median filtering";
    public static final String RADIUS = "Radius";
    public static final String THRESHOLD = "Threshold";
    public static final String LINKING_MAX_DISTANCE = "Max linking distance";
    public static final String GAP_CLOSING_MAX_DISTANCE = "Gap closing max distance";
    public static final String MAX_FRAME_GAP = "Max frame gap";
    public static final String DO_TRACKING = "Run tracking";
    public static final String CREATE_SUMMARY_OBJECTS = "Create summary objects (one per track)";
    public static final String OUTPUT_SUMMARY_OBJECTS = "Output summary objects";

    @Override
    public String getTitle() {
        return "Run TrackMate";

    }

    @Override
    public void execute(HCWorkspace workspace, boolean verbose) {
        if (verbose) System.out.println("   Running TrackMate detection");

        // Loading input image
        HCName targetImageName = parameters.getValue(INPUT_IMAGE);
        if (verbose) System.out.println("        Loading image ("+targetImageName.getName()+") into workspace");
        ImagePlus ipl = workspace.getImages().get(targetImageName).getImagePlus();

        // Storing, then removing calibration.  This will be reapplied after the detection.
        Calibration calibration = ipl.getCalibration();
        ipl.setCalibration(null);

        // Getting parameters
        boolean calibratedUnits = parameters.getValue(CALIBRATED_UNITS);
        boolean subpixelLocalisation = parameters.getValue(DO_SUBPIXEL_LOCALIZATION);
        double radius = parameters.getValue(RADIUS);
        double threshold = parameters.getValue(THRESHOLD);
        boolean medianFiltering = parameters.getValue(DO_MEDIAN_FILTERING);
        double maxLinkDist = parameters.getValue(LINKING_MAX_DISTANCE);
        double maxGapDist = parameters.getValue(GAP_CLOSING_MAX_DISTANCE);
        int maxFrameGap = parameters.getValue(MAX_FRAME_GAP);

        // Applying conversion to parameters
        if (calibratedUnits) {
            radius = calibration.getRawX(radius);
            maxLinkDist = calibration.getRawX(maxLinkDist);
            maxGapDist = calibration.getRawX(maxGapDist);

        }

        // Getting name of output objects
        HCName outputObjectsName = parameters.getValue(OUTPUT_SPOT_OBJECTS);
        HCObjectSet outputObjects = new HCObjectSet(outputObjectsName);

        // Getting name of output summary objects (if required)
        boolean createSummary = parameters.getValue(CREATE_SUMMARY_OBJECTS);
        HCName outputSummaryObjectsName;
        HCObjectSet summaryObjects = null;
        if (createSummary) {
            outputSummaryObjectsName = parameters.getValue(OUTPUT_SUMMARY_OBJECTS);
            summaryObjects = new HCObjectSet(outputSummaryObjectsName);

        }

        // Initialising settings for TrackMate
        Settings settings = new Settings();

        settings.setFrom(ipl);
        settings.detectorFactory = new LogDetectorFactory();
        settings.detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", subpixelLocalisation);
        settings.detectorSettings.put("DO_MEDIAN_FILTERING", medianFiltering);
        settings.detectorSettings.put("RADIUS", radius);
        settings.detectorSettings.put("THRESHOLD", threshold);
        settings.detectorSettings.put("TARGET_CHANNEL", 1);

        settings.trackerFactory  = new SparseLAPTrackerFactory();
        settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap();
        settings.trackerSettings.put("ALLOW_TRACK_SPLITTING", false);
        settings.trackerSettings.put("ALLOW_TRACK_MERGING", false);
        settings.trackerSettings.put("LINKING_MAX_DISTANCE", maxLinkDist);
        settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE", maxGapDist);
        settings.trackerSettings.put("MAX_FRAME_GAP",maxFrameGap);

        // Initialising TrackMate
        Model model = new Model();
        model.setLogger(Logger.VOID_LOGGER);
        TrackMate trackmate = new TrackMate(model, settings);

        // Running TrackMate
        if (!trackmate.checkInput()) {
            IJ.log(trackmate.getErrorMessage());
        }

        if (!trackmate.process()) {
            IJ.log(trackmate.getErrorMessage());
        }

        // Converting tracks to local track model
        int ID = 1;

        TrackModel trackModel = model.getTrackModel();
        Set<Integer> trackIDs = trackModel.trackIDs(false);
        for (Integer trackID:trackIDs) {
            // If necessary, creating a new summary object for the track
            HCObject summaryObject = null;
            if (createSummary) {
                summaryObject = new HCObject(trackID);
                summaryObject.setGroupID(trackID);

                // Adding calibration information
                summaryObject.addCalibration(HCObject.X,calibration.getX(1));
                summaryObject.addCalibration(HCObject.Y,calibration.getY(1));
                summaryObject.addCalibration(HCObject.Z,calibration.getZ(1));
                summaryObject.addCalibration(HCObject.C,1);
                summaryObject.addCalibration(HCObject.T,1);
                summaryObject.setCalibratedUnits(calibration.getUnits());

            }

            Set<Spot> spots = trackModel.trackSpots(trackID);

            // Creating an array to store the radius measurements for the summary object
            CumStat radiusAv = null;
            if (createSummary) radiusAv = new CumStat(1);

            // Getting x,y,f and 2-channel spot intensities from TrackMate results
            for (Spot spot:spots) {
                // Initialising a new HCObject to store this track and assigning a unique ID and group (track) ID.
                HCObject object = new HCObject(ID++);
                object.setGroupID(trackID);

                // Getting coordinates
                int x = (int) spot.getDoublePosition(0);
                int y = (int) spot.getDoublePosition(1);
                int z = (int) (spot.getDoublePosition(2)*calibration.getZ(1)/calibration.getX(1));
                int t = (int) Math.round(spot.getFeature(Spot.FRAME));

                // Adding coordinates to the instance objects
                object.addCoordinate(HCObject.X,x);
                object.addCoordinate(HCObject.Y,y);
                object.addCoordinate(HCObject.Z,z);
                object.addCoordinate(HCObject.T,t);

                // If necessary, adding coordinates to the summary objects
                if (createSummary) {
                    summaryObject.addCoordinate(HCObject.X,x);
                    summaryObject.addCoordinate(HCObject.Y,y);
                    summaryObject.addCoordinate(HCObject.Z,z);
                    summaryObject.addCoordinate(HCObject.T,t);

                }

                // Adding radius measurement using the same coordinate system as HCObject (XYCZT)
                HCMeasurement radiusMeasure = new HCMeasurement(HCMeasurement.RADIUS,spot.getFeature(Spot.RADIUS));
                radiusMeasure.setSource(this);
                object.addMeasurement(radiusMeasure);
                if (createSummary) radiusAv.addMeasure(spot.getFeature(Spot.RADIUS));

                // Adding calibration values to the HCObject (physical distance per pixel)
                object.addCalibration(HCObject.X,calibration.getX(1));
                object.addCalibration(HCObject.Y,calibration.getY(1));
                object.addCalibration(HCObject.Z,calibration.getZ(1));
                object.addCalibration(HCObject.C,1);
                object.addCalibration(HCObject.T,1);
                object.setCalibratedUnits(calibration.getUnits());

                // Adding the connection between instance and summary objects
                if (createSummary) {
                    object.setParent(summaryObject);
                    summaryObject.addChild(outputObjectsName,object);

                }

                // Adding the instance object to the relevant collection
                outputObjects.put(object.getID(),object);

            }

            // Taking average measurements for the summary object
            if (createSummary) {
                HCMeasurement radiusMeasure = new HCMeasurement(HCMeasurement.RADIUS,radiusAv.getMean()[0]);
                radiusMeasure.setSource(this);
                summaryObject.addMeasurement(radiusMeasure);

                summaryObjects.put(summaryObject.getID(), summaryObject);

            }
        }

        // Displaying the number of objects detected
        if (verbose) {
            System.out.println("        "+outputObjects.size()+" spots detected");

            if (createSummary) {
                System.out.println("        "+summaryObjects.size()+" tracks detected");

            }
        }

        // Adding objects to the workspace
        if (verbose) System.out.println("        Adding objects ("+outputObjectsName.getName()+") to workspace");
        workspace.addObjects(outputObjects);

        if (createSummary) workspace.addObjects(summaryObjects);

        // Reapplying calibration to input image
        ipl.setCalibration(calibration);

    }

    @Override
    public HCParameterCollection initialiseParameters() {
        HCParameterCollection parameters = new HCParameterCollection();

        parameters.addParameter(new HCParameter(this,INPUT_IMAGE,HCParameter.INPUT_IMAGE,null));
        parameters.addParameter(new HCParameter(this, OUTPUT_SPOT_OBJECTS,HCParameter.OUTPUT_OBJECTS,new HCName("Spots")));

        parameters.addParameter(new HCParameter(this,CALIBRATED_UNITS,HCParameter.BOOLEAN,false));
        parameters.addParameter(new HCParameter(this,DO_SUBPIXEL_LOCALIZATION,HCParameter.BOOLEAN,true));
        parameters.addParameter(new HCParameter(this,DO_MEDIAN_FILTERING,HCParameter.BOOLEAN,false));
        parameters.addParameter(new HCParameter(this,RADIUS,HCParameter.DOUBLE,2.0));
        parameters.addParameter(new HCParameter(this,THRESHOLD,HCParameter.DOUBLE,5000.0));

        parameters.addParameter(new HCParameter(this,DO_TRACKING,HCParameter.BOOLEAN,true));
        parameters.addParameter(new HCParameter(this,LINKING_MAX_DISTANCE,HCParameter.DOUBLE,2.0));
        parameters.addParameter(new HCParameter(this,GAP_CLOSING_MAX_DISTANCE,HCParameter.DOUBLE,2.0));
        parameters.addParameter(new HCParameter(this,MAX_FRAME_GAP,HCParameter.INTEGER,3));

        parameters.addParameter(new HCParameter(this,CREATE_SUMMARY_OBJECTS,HCParameter.BOOLEAN,true));
        parameters.addParameter(new HCParameter(this,OUTPUT_SUMMARY_OBJECTS,HCParameter.OUTPUT_OBJECTS,new HCName("Tracks")));

        return parameters;

    }

    @Override
    public HCParameterCollection getActiveParameters() {
        HCParameterCollection returnedParameters = new HCParameterCollection();
        returnedParameters.addParameter(parameters.getParameter(INPUT_IMAGE));
        returnedParameters.addParameter(parameters.getParameter(OUTPUT_SPOT_OBJECTS));

        returnedParameters.addParameter(parameters.getParameter(CALIBRATED_UNITS));
        returnedParameters.addParameter(parameters.getParameter(DO_SUBPIXEL_LOCALIZATION));
        returnedParameters.addParameter(parameters.getParameter(DO_MEDIAN_FILTERING));
        returnedParameters.addParameter(parameters.getParameter(RADIUS));
        returnedParameters.addParameter(parameters.getParameter(THRESHOLD));

        returnedParameters.addParameter(parameters.getParameter(DO_TRACKING));
        if (parameters.getValue(DO_TRACKING)) {
            returnedParameters.addParameter(parameters.getParameter(LINKING_MAX_DISTANCE));
            returnedParameters.addParameter(parameters.getParameter(GAP_CLOSING_MAX_DISTANCE));
            returnedParameters.addParameter(parameters.getParameter(MAX_FRAME_GAP));

            returnedParameters.addParameter(parameters.getParameter(CREATE_SUMMARY_OBJECTS));
            if (parameters.getValue(CREATE_SUMMARY_OBJECTS)) {
                returnedParameters.addParameter(parameters.getParameter(OUTPUT_SUMMARY_OBJECTS));

            }
        }

        return returnedParameters;

    }

    @Override
    public void addMeasurements(HCMeasurementCollection measurements) {

    }

    @Override
    public void addRelationships(HCRelationshipCollection relationships) {
        relationships.addRelationship(parameters.getValue(OUTPUT_SUMMARY_OBJECTS),parameters.getValue(OUTPUT_SPOT_OBJECTS));

    }
}
