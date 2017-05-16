package wbif.sjx.common.HighContent.Process;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import wbif.sjx.common.HighContent.Module.HCModule;
import wbif.sjx.common.HighContent.Object.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by sc13967 on 12/05/2017.
 */
public class HCExporter {
    public static final int XML_EXPORT = 0;
    public static final int XLSX_EXPORT = 1;
    public static final int JSON_EXPORT = 2;

    private int exportMode = XLSX_EXPORT;
    private File rootFolder;
    private boolean verbose = false;


    // CONSTRUCTOR

    public HCExporter(File rootFolder, int exportMode) {
        this.rootFolder = rootFolder;
        this.exportMode = exportMode;

    }


    // PUBLIC METHODS

    public void exportResults(HCWorkspaceCollection workspaces) {
        exportResults(workspaces,null);

    }

    public void exportResults(HCWorkspaceCollection workspaces, HCAnalysis analysis) {
        if (exportMode == XML_EXPORT) {
            exportXML(workspaces,analysis);

        } else if (exportMode == XLSX_EXPORT) {
            exportXLSX(workspaces,analysis);

        } else if (exportMode == JSON_EXPORT) {
            exportJSON(workspaces,analysis);

        }
    }

    private void exportXML(HCWorkspaceCollection workspaces, HCAnalysis analysis) {
        // Initialising DecimalFormat
        DecimalFormat df = new DecimalFormat("0.000E0");

        // Getting modules
        HCModuleCollection modules = analysis.getModules();

        try {
            // Initialising the document
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("ROOT");
            doc.appendChild(root);

            // Getting paramters as Element and adding to the main file
            Element parametersElement = prepareParametersXML(doc,modules);
            root.appendChild(parametersElement);

            // Running through each workspace (each corresponds to a file) adding file information
            for (HCWorkspace workspace:workspaces) {
                Element setElement =  doc.createElement("SET");

                // Adding metadata from the workspace
                HCMetadata metadata = workspace.getMetadata();
                for (String key:metadata.keySet()) {
                    String attrName = key.toUpperCase();
                    Attr attr = doc.createAttribute(attrName);
                    attr.appendChild(doc.createTextNode(metadata.getAsString(key)));
                    setElement.setAttributeNode(attr);

                }

                // Creating new elements for each image in the current workspace with at least one measurement
                for (HCImageName imageName:workspace.getImages().keySet()) {
                    HCImage image = workspace.getImages().get(imageName);

                    if (image.getSingleMeasurements() != null) {
                        Element imageElement = doc.createElement("IMAGE");

                        Attr nameAttr = doc.createAttribute("NAME");
                        nameAttr.appendChild(doc.createTextNode(String.valueOf(imageName.getName())));
                        imageElement.setAttributeNode(nameAttr);

                        for (HCSingleMeasurement measurement : image.getSingleMeasurements().values()) {
                            String attrName = measurement.getName().toUpperCase().replaceAll(" ", "_");
                            Attr measAttr = doc.createAttribute(attrName);
                            String attrValue = df.format(measurement.getValue());
                            measAttr.appendChild(doc.createTextNode(attrValue));
                            imageElement.setAttributeNode(measAttr);
                        }

                        setElement.appendChild(imageElement);

                    }
                }

                // Creating new elements for each object in the current workspace
                for (HCObjectName objectNames:workspace.getObjects().keySet()) {
                    for (HCObject object:workspace.getObjects().get(objectNames).values()) {
                        Element objectElement =  doc.createElement("OBJECT");

                        // Setting the ID number
                        Attr idAttr = doc.createAttribute("ID");
                        idAttr.appendChild(doc.createTextNode(String.valueOf(object.getID())));
                        objectElement.setAttributeNode(idAttr);

                        Attr nameAttr = doc.createAttribute("NAME");
                        nameAttr.appendChild(doc.createTextNode(String.valueOf(objectNames.getName())));
                        objectElement.setAttributeNode(nameAttr);

                        for (HCSingleMeasurement measurement:object.getSingleMeasurements().values()) {
                            Element measElement = doc.createElement("MEAS");

                            String name = measurement.getName().toUpperCase().replaceAll(" ", "_");
                            measElement.setAttribute("NAME",name);

                            String value = df.format(measurement.getValue());
                            measElement.setAttribute("VALUE",value);

                            // Adding the measurement as a child of that object
                            objectElement.appendChild(measElement);

                        }

                        for (HCMultiMeasurement measurement:object.getMultiMeasurements().values()) {
                            Element multiMeasElement = doc.createElement("MULTI_MEAS");

                            String name = measurement.getName().toUpperCase().replaceAll(" ", "_");
                            multiMeasElement.setAttribute("NAME",name);

                            for (Double position:measurement.getValues().keySet()) {
                                Element measElement = doc.createElement("MEAS");

                                measElement.setAttribute("POS",String.valueOf(position));

                                measElement.setAttribute("VALUE",String.valueOf(measurement.getValue(position)));

                                // Adding the measurement as a child of that multi measurement
                                multiMeasElement.appendChild(measElement);

                            }
                        }

                        setElement.appendChild(objectElement);

                    }
                }

                root.appendChild(setElement);

            }

            // Preparing the filepath and filename
            String outPath = rootFolder+"\\"+"output.xml";

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(outPath);

            transformer.transform(source, result);

            if (verbose) System.out.println("Saved "+ outPath);


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private Element prepareParametersXML(Document doc, HCModuleCollection modules) {
        Element parametersElement =  doc.createElement("PARAMETERS");

        // Running through each parameter set (one for each module
        for (HCModule module:modules) {
            LinkedHashMap<String,HCParameter> parameters = module.getActiveParameters().getParameters();

            boolean first = true;
            Element moduleElement =  doc.createElement("MODULE");
            for (HCParameter currParam:parameters.values()) {
                // For the first parameter in a module, adding the name
                if (first) {
                    Attr nameAttr = doc.createAttribute("NAME");
                    nameAttr.appendChild(doc.createTextNode(currParam.getModule().getClass().getName()));
                    moduleElement.setAttributeNode(nameAttr);

                    Attr hashAttr = doc.createAttribute("HASH");
                    hashAttr.appendChild(doc.createTextNode(String.valueOf(currParam.getModule().hashCode())));
                    moduleElement.setAttributeNode(hashAttr);

                    first = false;
                }

                // Adding the name and value of the current parameter
                Element parameterElement =  doc.createElement("PARAMETER");

                Attr nameAttr = doc.createAttribute("NAME");
                nameAttr.appendChild(doc.createTextNode(currParam.getName()));
                parameterElement.setAttributeNode(nameAttr);

                Attr valueAttr = doc.createAttribute("VALUE");
                valueAttr.appendChild(doc.createTextNode(currParam.getValue().toString()));
                parameterElement.setAttributeNode(valueAttr);

                moduleElement.appendChild(parameterElement);

            }

            // Adding current module to parameters
            parametersElement.appendChild(moduleElement);

        }

        return parametersElement;

    }

    private void exportXLSX(HCWorkspaceCollection workspaces, HCAnalysis analysis) {
        // Getting modules
        HCModuleCollection modules = analysis.getModules();

        // Initialising the workbook
        XSSFWorkbook workbook = new XSSFWorkbook();

        // Adding relevant sheets
        prepareParametersXLSX(workbook,modules);
        prepareMetadataXLSX(workbook,workspaces);
        prepareImagesXLSX(workbook,workspaces);
        prepareObjectsXLSX(workbook,workspaces);

        // Writing the workbook to file
        try {
            String outPath = rootFolder+"\\"+"output.xlsx";
            FileOutputStream outputStream = new FileOutputStream(outPath);
            workbook.write(outputStream);
            workbook.close();

            if (verbose) System.out.println("Saved "+ outPath);

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private void prepareParametersXLSX(XSSFWorkbook workbook, HCModuleCollection modules) {
        // Creating a sheet for parameters
        XSSFSheet paramSheet = workbook.createSheet("Parameters");

        // Adding a header row for the parameter titles
        int paramRow = 0;
        int paramCol = 0;
        Row parameterHeader = paramSheet.createRow(paramRow++);

        Cell nameHeaderCell = parameterHeader.createCell(paramCol++);
        nameHeaderCell.setCellValue("PARAMETER");

        Cell valueHeaderCell = parameterHeader.createCell(paramCol++);
        valueHeaderCell.setCellValue("VALUE");

        Cell moduleHeaderCell = parameterHeader.createCell(paramCol);
        moduleHeaderCell.setCellValue("MODULE");

        // Adding a new parameter to each row
        for (HCModule module:modules) {
            LinkedHashMap<String,HCParameter> parameters = module.getActiveParameters().getParameters();

            paramRow++;

            for (HCParameter currParam : parameters.values()) {
                paramCol = 0;
                Row row = paramSheet.createRow(paramRow++);

                Cell nameValueCell = row.createCell(paramCol++);
                nameValueCell.setCellValue(currParam.getName());

                Cell valueValueCell = row.createCell(paramCol++);
                valueValueCell.setCellValue(currParam.getValue().toString());

                Cell moduleValueCell = row.createCell(paramCol);
                moduleValueCell.setCellValue(currParam.getModule().getClass().getName());

            }

        }
    }

    private void prepareMetadataXLSX(XSSFWorkbook workbook, HCWorkspaceCollection workspaces) {
        // Basing column names on the first workspace in the WorkspaceCollection
        HCWorkspace exampleWorkspace = workspaces.get(0);

        if (exampleWorkspace != null) {
            HCMetadata exampleMetadata = exampleWorkspace.getMetadata();

            if (exampleMetadata.size() != 0) {
                // Adding header rows for the metadata sheet.
                XSSFSheet metaSheet = workbook.createSheet("Metadata");

                // Creating the header row
                int metaRow = 0;
                int metaCol = 0;
                Row metaHeaderRow = metaSheet.createRow(metaRow++);

                // Setting the analysis ID.  This is the same value on each sheet
                Cell IDHeaderCell = metaHeaderRow.createCell(metaCol++);
                IDHeaderCell.setCellValue("ANALYSIS_ID");

                // Running through all the metadata values, adding them as new columns
                for (String name : exampleMetadata.keySet()) {
                    Cell metaHeaderCell = metaHeaderRow.createCell(metaCol++);
                    metaHeaderCell.setCellValue(name);

                }

                // Running through each workspace, adding the relevant values.  Metadata is stored as a LinkedHashMap, so values
                // should always come off in the same order for the same analysis
                for (HCWorkspace workspace : workspaces) {
                    HCMetadata metadata = workspace.getMetadata();

                    metaCol = 0;
                    Row metaValueRow = metaSheet.createRow(metaRow++);

                    // Setting the analysis ID.  This is the same value on each sheet
                    Cell metaValueCell = metaValueRow.createCell(metaCol++);
                    metaValueCell.setCellValue(workspace.getID());

                    // Running through all the metadata values, adding them as new columns
                    for (String name : metadata.keySet()) {
                        metaValueCell = metaValueRow.createCell(metaCol++);
                        metaValueCell.setCellValue(metadata.getAsString(name));

                    }
                }
            }
        }
    }

    private void prepareImagesXLSX(XSSFWorkbook workbook, HCWorkspaceCollection workspaces) {
        // Basing column names on the first workspace in the WorkspaceCollection
        HCWorkspace exampleWorkspace = workspaces.get(0);

        if (exampleWorkspace.getImages() != null) {
            // Creating a new sheet for each image.  Each analysed file will have its own row.
            HashMap<HCImageName, XSSFSheet> imageSheets = new HashMap<>();
            HashMap<HCImageName, Integer> imageRows = new HashMap<>();

            // Using the first workspace in the WorkspaceCollection to initialise column headers
            for (HCImageName imageName : exampleWorkspace.getImages().keySet()) {
                HCImage image = exampleWorkspace.getImages().get(imageName);

                if (image.getSingleMeasurements().size() != 0) {
                    // Creating relevant sheet prefixed with "IM"
                    imageSheets.put(imageName, workbook.createSheet("IM_" + imageName.getName()));

                    // Adding headers to each column
                    int col = 0;

                    imageRows.put(imageName, 1);
                    Row imageHeaderRow = imageSheets.get(imageName).createRow(0);

                    // Creating a cell holding the path to the analysed file
                    Cell IDHeaderCell = imageHeaderRow.createCell(col++);
                    IDHeaderCell.setCellValue("ANALYSIS_ID");

                    for (HCSingleMeasurement measurement : image.getSingleMeasurements().values()) {
                        Cell measHeaderCell = imageHeaderRow.createCell(col++);
                        String measurementName = measurement.getName().toUpperCase().replaceAll(" ", "_");
                        measHeaderCell.setCellValue(measurementName);
                    }
                }
            }

            // Running through each Workspace, adding rows
            for (HCWorkspace workspace : workspaces) {
                for (HCImageName imageName : workspace.getImages().keySet()) {
                    HCImage image = exampleWorkspace.getImages().get(imageName);

                    if (image.getSingleMeasurements().size() != 0) {
                        // Adding the measurements from this image
                        int col = 0;

                        Row imageValueRow = imageSheets.get(imageName).createRow(imageRows.get(imageName));
                        imageRows.compute(imageName, (k, v) -> v = v + 1);

                        // Creating a cell holding the path to the analysed file
                        Cell IDValueCell = imageValueRow.createCell(col++);
                        IDValueCell.setCellValue(workspace.getID());

                        for (HCSingleMeasurement measurement : image.getSingleMeasurements().values()) {
                            Cell measValueCell = imageValueRow.createCell(col++);
                            measValueCell.setCellValue(measurement.getValue());
                        }
                    }
                }
            }
        }
    }

    private void prepareObjectsXLSX(XSSFWorkbook workbook,HCWorkspaceCollection workspaces) {
        // Basing column names on the first workspace in the WorkspaceCollection
        HCWorkspace exampleWorkspace = workspaces.get(0);

        if (exampleWorkspace != null) {
            // Creating a new sheet for each object.  Each analysed file will have its own set of rows (one for each object)
            HashMap<HCObjectName, XSSFSheet> objectSheets = new HashMap<>();
            HashMap<HCObjectName, Integer> objectRows = new HashMap<>();

            // Using the first workspace in the WorkspaceCollection to initialise column headers
            for (HCObjectName objectName : exampleWorkspace.getObjects().keySet()) {
                HashMap<Integer, HCObject> objects = exampleWorkspace.getObjects().get(objectName);

                if (objects.values().iterator().next().getSingleMeasurements().size() != 0) {
                    // Creating relevant sheet prefixed with "IM"
                    objectSheets.put(objectName, workbook.createSheet("OBJ_" + objectName.getName()));

                    // Adding headers to each column
                    int col = 0;

                    objectRows.put(objectName, 1);
                    Row objectHeaderRow = objectSheets.get(objectName).createRow(0);

                    // Creating a cell holding the path to the analysed file
                    Cell IDHeaderCell = objectHeaderRow.createCell(col++);
                    IDHeaderCell.setCellValue("ANALYSIS_ID");

                    HCObject object = objects.values().iterator().next();
                    for (HCSingleMeasurement measurement : object.getSingleMeasurements().values()) {
                        Cell measHeaderCell = objectHeaderRow.createCell(col++);
                        String measurementName = measurement.getName().toUpperCase().replaceAll(" ", "_");
                        measHeaderCell.setCellValue(measurementName);
                    }
                }
            }

            // Running through each Workspace, adding rows
            for (HCWorkspace workspace : workspaces) {
                for (HCObjectName objectName : exampleWorkspace.getObjects().keySet()) {
                    HashMap<Integer, HCObject> objects = exampleWorkspace.getObjects().get(objectName);

                    if (objects.values().iterator().next().getSingleMeasurements().size() != 0) {
                        for (HCObject object : objects.values()) {
                            // Adding the measurements from this image
                            int col = 0;

                            Row objectValueRow = objectSheets.get(objectName).createRow(objectRows.get(objectName));
                            objectRows.compute(objectName, (k, v) -> v = v + 1);

                            // Creating a cell holding the path to the analysed file
                            Cell IDValueCell = objectValueRow.createCell(col++);
                            IDValueCell.setCellValue(workspace.getID());

                            for (HCSingleMeasurement measurement : object.getSingleMeasurements().values()) {
                                Cell measValueCell = objectValueRow.createCell(col++);
                                measValueCell.setCellValue(measurement.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    private void exportJSON(HCWorkspaceCollection workspaces, HCAnalysis analysis) {
        System.out.println("[WARN] No JSON export currently implemented.  File not saved.");

    }


    // GETTERS AND SETTERS

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}