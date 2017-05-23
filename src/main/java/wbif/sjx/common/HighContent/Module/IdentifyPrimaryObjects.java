package wbif.sjx.common.HighContent.Module;

import fiji.threshold.Auto_Threshold;
import ij.ImagePlus;
import ij.plugin.Filters3D;
import inra.ijpb.binary.conncomp.FloodFillComponentsLabeling3D;
import inra.ijpb.segment.Threshold;
import wbif.sjx.common.HighContent.Object.*;
import wbif.sjx.common.HighContent.Object.HCParameterCollection;


/**
 * Created by sc13967 on 02/05/2017.
 */
public class IdentifyPrimaryObjects extends HCModule {
    public static final String INPUT_IMAGE = "Input image";
    public static final String OUTPUT_OBJECT = "Output object";
    public static final String MEDIAN_FILTER_RADIUS = "Median filter radius";
    public static final String THRESHOLD_MULTIPLIER = "Threshold multiplier";

    @Override
    public String getTitle() {
        return "Identify primary objects";

    }

    public void execute(HCWorkspace workspace, boolean verbose) {
        if (verbose) System.out.println("    Running primary object identification");

        // Getting parameters
        double medFiltR = parameters.getValue(MEDIAN_FILTER_RADIUS);
        double thrMult = parameters.getValue(THRESHOLD_MULTIPLIER);
        HCName outputObjectName = parameters.getValue(OUTPUT_OBJECT);

        // Getting image stack
        HCName targetImageName = parameters.getValue(INPUT_IMAGE);
        ImagePlus ipl = workspace.getImages().get(targetImageName).getImagePlus();

        // Applying smoothing filter
        if (verbose) System.out.println("       Applying filter (radius = "+medFiltR+" px)");
        ipl.setStack(Filters3D.filter(ipl.getImageStack(), Filters3D.MEDIAN, (float) medFiltR, (float) medFiltR, (float) medFiltR));

        // Applying threshold
        if (verbose) System.out.println("       Applying thresholding (multplier = "+thrMult+" x)");
        Auto_Threshold auto_threshold = new Auto_Threshold();
        Object[] results1 = auto_threshold.exec(ipl,"Otsu",true,false,true,true,false,true);
        ipl = Threshold.threshold(ipl,(Integer) results1[0]*thrMult,Integer.MAX_VALUE);

        // Applying connected components labelling
        if (verbose) System.out.println("       Applying connected components labelling");
        FloodFillComponentsLabeling3D ffcl3D = new FloodFillComponentsLabeling3D();
        ipl.setStack(ffcl3D.computeLabels(ipl.getImageStack()));

        // Converting image to objects
        if (verbose) System.out.println("       Converting image to objects");
        HCImage tempImage = new HCImage(new HCName("Temp image"),ipl);
        HCObjectSet outputObjects = new ObjectImageConverter().convertImageToObjects(tempImage,outputObjectName);

        // Adding objects to workspace
        if (verbose) System.out.println("       Adding objects ("+outputObjectName.getName()+") to workspace");
        workspace.addObjects(outputObjects);

    }

    @Override
    public HCParameterCollection initialiseParameters() {
        HCParameterCollection parameters = new HCParameterCollection();

        // Setting the input image stack name
        parameters.addParameter(new HCParameter(this,INPUT_IMAGE, HCParameter.INPUT_IMAGE,null));
        parameters.addParameter(new HCParameter(this,OUTPUT_OBJECT, HCParameter.OUTPUT_OBJECTS,null));
        parameters.addParameter(new HCParameter(this,MEDIAN_FILTER_RADIUS, HCParameter.DOUBLE,2.0));
        parameters.addParameter(new HCParameter(this,THRESHOLD_MULTIPLIER, HCParameter.DOUBLE,1.0));

        return parameters;

    }

    @Override
    public HCParameterCollection getActiveParameters() {
        return parameters;
    }

    @Override
    public void addMeasurements(HCMeasurementCollection measurements) {

    }

    @Override
    public void addRelationships(HCRelationshipCollection relationships) {

    }
}
