package wbif.sjx.common.HighContent.Module;

import wbif.sjx.common.HighContent.Extractor.Extractor;
import wbif.sjx.common.HighContent.Object.HCParameter;
import wbif.sjx.common.HighContent.Object.HCParameterCollection;
import wbif.sjx.common.HighContent.Object.HCMetadata;
import wbif.sjx.common.HighContent.Object.HCWorkspace;


/**
 * Created by sc13967 on 05/05/2017.
 */
public class MetadataExtractor extends HCModule {
    public static final String FILENAME_EXTRACTOR = "Filename extractor";
    public static final String FOLDERNAME_EXTRACTOR = "Foldername extractor";

    @Override
    public void execute(HCWorkspace workspace, boolean verbose) {
        // Getting current result
        HCMetadata metadata = workspace.getMetadata();

        // Getting filename extractor
        Extractor filenameExtractor = parameters.getValue(FILENAME_EXTRACTOR);

        // Getting foldername extractor
        Extractor foldernameExtractor = parameters.getValue(FOLDERNAME_EXTRACTOR);

        // Preparing Result object
        metadata.setFile(workspace.getMetadata().getFile());
        foldernameExtractor.extract(metadata,metadata.getFile().getParent());
        filenameExtractor.extract(metadata,metadata.getFile().getName());

    }

    @Override
    public HCParameterCollection initialiseParameters() {
        HCParameterCollection parameters = new HCParameterCollection();

        parameters.addParameter(new HCParameter(this,FILENAME_EXTRACTOR, HCParameter.OBJECT,null,false));
        parameters.addParameter(new HCParameter(this,FOLDERNAME_EXTRACTOR, HCParameter.OBJECT,null,false));

        return parameters;

    }

    @Override
    public HCParameterCollection getActiveParameters() {
        return parameters;
    }
}

