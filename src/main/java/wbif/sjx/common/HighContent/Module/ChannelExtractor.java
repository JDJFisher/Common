package wbif.sjx.common.HighContent.Module;

import ij.ImagePlus;
import ij.plugin.SubHyperstackMaker;
import wbif.sjx.common.HighContent.Object.*;

/**
 * Created by sc13967 on 08/05/2017.
 */
public class ChannelExtractor extends HCModule {
    public static final String INPUT_IMAGE = "Input image";
    public static final String OUTPUT_IMAGE = "Output image1";
    public static final String CHANNEL_TO_EXTRACT = "Channel to extract";
    public static final String SHOW_IMAGE = "Show output image";

    @Override
    public String getTitle() {
        return "Channel extractor";

    }

    @Override
    public void execute(HCWorkspace workspace, boolean verbose) {
        if (verbose) System.out.println("   Running channel extractor");

        // Loading input image
        HCName inputImageName = parameters.getValue(INPUT_IMAGE);
        if (verbose) System.out.println("       Loading image ("+inputImageName.getName()+") into workspace");
        ImagePlus ipl = workspace.getImages().get(inputImageName).getImagePlus();

        // Getting parameters
        HCName outputImageName = parameters.getValue(OUTPUT_IMAGE);
        int channel = parameters.getValue(CHANNEL_TO_EXTRACT);

        // Getting selected channel
        if (verbose) System.out.println("       Extracting channel "+channel);
        ImagePlus outputChannelImagePlus = SubHyperstackMaker.makeSubhyperstack(ipl,String.valueOf(channel),"1-"+ipl.getNSlices(),"1-"+ipl.getNFrames());

        // Adding image to workspace
        if (verbose) System.out.println("       Adding image ("+outputImageName.getName()+") to workspace");
        workspace.addImage(new HCImage(outputImageName,outputChannelImagePlus));

        // (If selected) displaying the loaded image
        boolean showImage = parameters.getValue(SHOW_IMAGE);
        if (showImage) {
            if (verbose) System.out.println("       Displaying extracted image");
            outputChannelImagePlus.show();
        }
    }

    @Override
    public HCParameterCollection initialiseParameters() {
        HCParameterCollection parameters = new HCParameterCollection();

        parameters.addParameter(new HCParameter(this,INPUT_IMAGE, HCParameter.INPUT_IMAGE,null));
        parameters.addParameter(new HCParameter(this,OUTPUT_IMAGE, HCParameter.OUTPUT_IMAGE,null));
        parameters.addParameter(new HCParameter(this,CHANNEL_TO_EXTRACT, HCParameter.INTEGER,1));
        parameters.addParameter(new HCParameter(this,SHOW_IMAGE, HCParameter.BOOLEAN,false));

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
