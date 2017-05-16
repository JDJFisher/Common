// TODO: Add methods for XLSX and JSON data export

package wbif.sjx.common.HighContent.Process;

import wbif.sjx.common.HighContent.Object.*;
import wbif.sjx.common.System.FileCrawler;

import java.io.File;

/**
 * Created by sc13967 on 21/10/2016.
 */
public class BatchProcessor extends FileCrawler {
    private boolean verbose = false;


    // CONSTRUCTORS

    public BatchProcessor(File root_folder) {
        super(root_folder);

    }

    public BatchProcessor() {

    }


    // PUBLIC METHODS

    public HCWorkspaceCollection runAnalysisOnStructure(HCAnalysis analysis, HCExporter exporter) {
        int num_valid_files = getNumberOfValidFilesInStructure();
        resetIterator();

        HCWorkspaceCollection workspaces = new HCWorkspaceCollection();

        folder = rootFolder;
        File next = getNextValidFileInStructure();

        int iter = 1;

        if (analysis != null) {
            while (next != null) {
                System.out.println("Processing file: " + next.getName() + " (file " + iter++ + " of " + num_valid_files + ")");

                // Running the analysis
                HCWorkspace workspace = workspaces.getNewWorkspace(next);
                analysis.execute(workspace,verbose);

                // Clearing images from the workspace to prevent memory leak
                workspace.clearAllImages(true);

                next = getNextValidFileInStructure();

                // Adding a blank line to the output
                if (verbose) System.out.println(" ");

            }
        }

        // Saving the results
        exporter.exportResults(workspaces,analysis.getModules());

        return workspaces;

    }


    // GETTERS AND SETTERS

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
