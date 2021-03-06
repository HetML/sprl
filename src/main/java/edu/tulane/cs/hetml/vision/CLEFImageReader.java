package edu.tulane.cs.hetml.vision;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLDouble;
import edu.tulane.cs.hetml.nlp.BaseTypes.Document;
import edu.tulane.cs.hetml.nlp.Xml.NlpXmlReader;
import sun.awt.image.ToolkitImage;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Reads CLEF Image dataset, given a directory
 *
 * @author Umar Manzoor
 */
public class CLEFImageReader {

    private final String trainFilePath;
    private final String testFilePath;
    private String path;
    private Boolean readFullData;
    private List<String> trainingData;
    private List<String> testData;

    public List<Image> trainingImages;
    public List<Segment> trainingSegments;
    public List<SegmentRelation> trainingRelations;

    public List<Image> testImages;
    public List<Segment> testSegments;
    public List<SegmentRelation> testRelations;

    public List<Segment> allSegments;

    private boolean useRedefinedRelations;

    private Hashtable<Integer, String> MapCode2Concept = new Hashtable<Integer, String>();
    private Hashtable<String, String> segmentReferitText = new Hashtable<String, String>();
    private Hashtable<String, String> segmentOntology = new Hashtable<String, String>();
    private Hashtable<String, String> redefindedRelations = new Hashtable<String, String>();
    private Hashtable<String, Rectangle2D> segmentBoxes = new Hashtable<String, Rectangle2D>();

    PrintWriter printWriterTest;

    public CLEFImageReader(String directory, String trainFilePath, String testFilePath, Boolean readFullData, Boolean useRedefinedRelations) throws IOException {

        this.useRedefinedRelations = useRedefinedRelations;
        this.trainFilePath = trainFilePath;
        this.testFilePath = testFilePath;
        File d = new File(directory);

        if (!d.exists()) {
            throw new IOException(directory + " does not exist!");
        }

        if (!d.isDirectory()) {
            throw new IOException(directory + " is not a directory!");
        }
        trainingData = new ArrayList<>();
        testData = new ArrayList<>();

        this.readFullData = readFullData;

        // Training Data
        trainingImages = new ArrayList<>();
        trainingSegments = new ArrayList<>();
        trainingRelations = new ArrayList<>();
        // Test Data
        testImages = new ArrayList<>();
        testSegments = new ArrayList<>();
        testRelations = new ArrayList<>();

        // all Segment
        allSegments = new ArrayList<>();

        path = directory;
        // Load segment Boxes Information
        getSegmentsBox(directory);
        // Load redefined segment relations
        getRedefinedRelations(directory);
        // Load Concepts
        getConcepts(directory);

        // Load Training
        getTrainingImages();
        // Load Testing
        getTestImages();
        // Load all Images
        getallImages(directory);

        System.out.println("Total Train Data " + trainingData.size());

        System.out.println("Total Test Data " + testData.size());

        System.out.println("Total Train Images " + trainingImages.size());

        System.out.println("Total Test Images " + testImages.size());

        System.out.println("Total Train Segments " + trainingSegments.size());

        System.out.println("Total Test Segments " + testSegments.size());

        System.out.println("Total Train Relations " + trainingRelations.size());

        System.out.println("Total Test Relations " + testRelations.size());

    }

    /*****************************************/
    // Takes object code as input and returns
    // object concept

    /*****************************************/
    private String MappingCode2Concept(int code) {
        return getMapCode2Concept().get(code);
    }

    /*******************************************************/
    // Loading Image Codes and its Corresponding Concept
    // Storing information in HashTable for quick retrieval

    /*******************************************************/
    private void getConcepts(String directory) throws IOException {
        String file = directory + "/wlist.txt";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] CodesInfo = line.split("\\t");
            if (CodesInfo.length > 1) {
                getMapCode2Concept().put(Integer.parseInt(CodesInfo[0]), CodesInfo[1]);
            } else {
                getMapCode2Concept().put(Integer.parseInt(CodesInfo[0]), " ");
            }
        }
    }

    /*******************************************************/
    // Loading Referit Text for CLEF Segments
    // Storing information in HashTable for quick retrieval

    /*******************************************************/
    private void getReferitText(String directory) throws IOException {
        String file = directory + "/ReferGames.txt";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] segReferitText = line.split("\\~");
            if (segReferitText.length > 1) {
                segmentReferitText.put(segReferitText[0], segReferitText[1]);
            } else {
                segmentReferitText.put(segReferitText[0], " ");
            }
        }
    }

    /*******************************************************/
    // Loading Segment Box Information for CLEF Segments
    // Storing information in HashTable for quick retrieval

    /*******************************************************/
    private void getSegmentsBox(String directory) throws IOException {
        String file = directory + "/SegmentBoxes/segmentBoxes.txt";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] segBoxInfo = line.split(" ");
            String key = segBoxInfo[0] + "-" + segBoxInfo[1];
            Rectangle2D rec = RectangleHelper.parseRectangle(segBoxInfo[2], "-");
            segmentBoxes.put(key, rec);
        }
    }


    private void getRedefinedRelations(String directory) throws IOException {
        directory = directory + "/relations";
        File d = new File(directory);

        if (!d.exists()) {
            throw new IOException(directory + " does not exist!");
        }

        if (!d.isDirectory()) {
            throw new IOException(directory + " is not a directory!");
        }
        for (File f : d.listFiles()) {
            if (f.isFile()) {
                String name = f.getName();
                String imageFile = directory + "/" + name;
                String[] fileName = name.split("-");
                String[] imageId = fileName[1].split("\\.");
                String line;
                BufferedReader reader = new BufferedReader(new FileReader(imageFile));
                while ((line = reader.readLine()) != null) {
                    String[] relationInfo = line.split(",");
                    String key = (imageId[0]).trim() + "-" + (relationInfo[1]).trim() + "-" + (relationInfo[3]).trim() + "-" + (relationInfo[5]).trim();
                    redefindedRelations.put(key, relationInfo[0]);
                }
                reader.close();
            }
        }
    }

    /*******************************************************/
    // Load all Images in the CLEF Dataset

    /*******************************************************/
    private void getallImages(String directory) throws IOException {
        File d = new File(directory);

        if (!d.exists()) {
            throw new IOException(directory + " does not exist!");
        }

        if (!d.isDirectory()) {
            throw new IOException(directory + " is not a directory!");
        }

        for (File f : d.listFiles()) {
            if (f.isDirectory()) {

                String mainFolder = directory + "/" + f.getName();
                System.out.println(mainFolder);
                //Load all images
                String imageFolder = mainFolder + "/images";
                getImages(imageFolder);

                //Load all segments
                String ontologyfile = mainFolder + "/ontology_path.txt";
                getSegmentsOntology(ontologyfile);

                //Load all segments
                String file = mainFolder + "/features.txt";
                getSegments(file);

                //Load all relations
                String spatialRelations = mainFolder + "/spatial_rels";
                getSegmentsRelations(spatialRelations);
            }
        }
    }

    /*******************************************************/
    // Loading Images

    /*******************************************************/
    private void getImages(String folder) throws IOException {
        File d = new File(folder);

        if (d.exists()) {

            for (File f : d.listFiles()) {
                String label = f.getName();
                String[] split = label.split("\\.");
                ToolkitImage image = (ToolkitImage) Toolkit.getDefaultToolkit().getImage(f.getAbsolutePath());
                int width = image.getWidth();
                int height = image.getHeight();
                if (trainingData.contains(split[0]))
                    trainingImages.add(new Image(label, split[0], width, height));
                if (testData.contains(split[0]))
                    testImages.add(new Image(label, split[0],  width, height));
            }
        }
    }
    /*******************************************************/
    // Loading Segments

    /*******************************************************/
    private void getSegments(String file) throws IOException {
        File d = new File(file);

        if (d.exists()) {

            String line;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                String[] segmentInfo = line.split("\\t");
                if (segmentInfo.length == 4) {
                    String imageId = segmentInfo[0];
                    int segmentId = Integer.parseInt(segmentInfo[1]);
                    int segmentCode = Integer.parseInt(segmentInfo[3]);
                    String segmentConcept = MappingCode2Concept(segmentCode);

                    if (segmentConcept != null) {
                        String segmentFeatures = segmentInfo[2];
                        segmentFeatures = segmentFeatures.trim().replaceAll(" +", " ");
                        if (trainingData.contains(imageId)) {
                            String key = imageId + "-" + segmentId;
                            trainingSegments.add(new Segment(imageId, segmentId, segmentCode, segmentFeatures, segmentConcept, segmentBoxes.get(key)));
                            allSegments.add(new Segment(imageId, segmentId, segmentCode, null, segmentConcept, segmentBoxes.get(key)));
                        }
                        if (testData.contains(imageId)) {
                            String key = imageId + "-" + segmentId;
                            testSegments.add(new Segment(imageId, segmentId, segmentCode, segmentFeatures, segmentConcept, segmentBoxes.get(key)));
                            allSegments.add(new Segment(imageId, segmentId, segmentCode, null, segmentConcept, segmentBoxes.get(key)));
                        }
                    }
                }
            }
            reader.close();
        }
    }

    /*******************************************************/
    // Loading Segment Relations

    /*******************************************************/
    private void getSegmentsRelations(String spatial_rels) throws IOException {
        File d = new File(spatial_rels);

        if (d.exists()) {
            for (File f : d.listFiles()) {
                String spatial_file = spatial_rels + "/" + f.getName();
                MatFileReader matFileReader = new MatFileReader(spatial_file);
                int val;
                int firstSegmentId;
                int secondSegmentId;
                String[] s = f.getName().split("\\.");
                String imgId = s[0];
                String rel;
                double[][] topo = ((MLDouble) matFileReader.getMLArray("topo")).getArray();
                double[][] xRels = ((MLDouble) matFileReader.getMLArray("x_rels")).getArray();
                double[][] yRels = ((MLDouble) matFileReader.getMLArray("y_rels")).getArray();
                /**************************************************/
                // Exemptional case
                // Sometimes mat file is returning only one value
                /**************************************************/
                if (topo.length > 1) {
                    // Finding Relationships
                    for (int x = 0; x < topo[0].length; x++)
                        for (int y = 0; y < topo[1].length; y++) {
                            //Ignoring same indexes
                            if (x != y) {
                                firstSegmentId = x + 1;
                                secondSegmentId = y + 1;
                                val = (int) topo[x][y];
                                if (val == 1)
                                    rel = "adjacent";
                                else if (val == 2)
                                    rel = "disjoint";
                                else
                                    rel = null;

                                if (rel != null) {
                                    if (trainingData.contains(imgId)) {
                                        //Creating new Relation between segments
                                        trainingRelations.add(new SegmentRelation(imgId, firstSegmentId, secondSegmentId, rel));
                                    }
                                    if (testData.contains(imgId)) {
                                        testRelations.add(new SegmentRelation(imgId, firstSegmentId, secondSegmentId, rel));
                                    }
                                }
                                val = (int) xRels[x][y];
                                if (val == 3)
                                    rel = "beside";
                                else if (val == 4) {
                                    if (!useRedefinedRelations)
                                        // Original "x-aligned"
                                        rel = "x-aligned";
                                    else {
                                        String key = imgId + "-" + firstSegmentId + "-" + secondSegmentId + "-" + "x-aligned";
                                        rel = redefindedRelations.get(key);
                                    }
                                }

                                if (rel != null) {
                                    if (trainingData.contains(imgId)) {
                                        //Creating new Relation between segments
                                        trainingRelations.add(new SegmentRelation(imgId, firstSegmentId, secondSegmentId, rel));
                                    }
                                    if (testData.contains(imgId)) {
                                        testRelations.add(new SegmentRelation(imgId, firstSegmentId, secondSegmentId, rel));
                                    }
                                }

                                val = (int) yRels[x][y];
                                if (val == 5)
                                    rel = "above";
                                else if (val == 6)
                                    rel = "below";
                                else if (val == 7) {
                                    if (!useRedefinedRelations)
                                        // Original "y-aligned"
                                        rel = "y-aligned";
                                    else {
                                        String key = imgId + "-" + firstSegmentId + "-" + secondSegmentId + "-" + "x-aligned";
                                        rel = redefindedRelations.get(key);
                                    }
                                }

                                if (rel != null) {
                                    if (trainingData.contains(imgId)) {
                                        //Creating new Relation between segments
                                        trainingRelations.add(new SegmentRelation(imgId, firstSegmentId, secondSegmentId, rel));
                                    }
                                    if (testData.contains(imgId)) {
                                        testRelations.add(new SegmentRelation(imgId, firstSegmentId, secondSegmentId, rel));
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

    /*******************************************************/
    // Loading Training Images

    /*******************************************************/
    private void getTrainingImages() throws IOException {
        if (readFullData) {
            getMatData(path + "/training.mat", true, "training");
            getMatData(path + "/validation.mat", true, "validation");
            ;
        } else {
            getXMLImages(trainFilePath, true);
        }
    }

    /*******************************************************/
    // Loading Testing Images

    /*******************************************************/
    private void getTestImages() throws IOException {
        if (readFullData) {
            getMatData(path + "/testing.mat", false, "testing");
        } else {
            getXMLImages(testFilePath, false);
        }
    }

    /*******************************************************/
    // Loading data from XML file
    // if choose = true, trainData will be populated
    // if choose = false, testData will be populated

    /*******************************************************/
    private void getXMLImages(String file, Boolean choose) throws IOException {

        File f = new File(file);

        if (!f.exists()) {
            throw new IOException(file + " does not exist!");
        }
        NlpXmlReader reader = new NlpXmlReader(file, "SCENE", "SENTENCE", null, null);
        reader.setIdUsingAnotherProperty("SCENE", "DOCNO");

        List<Document> documentList = reader.getDocuments();

        for (Document d : documentList) {
            String name = d.getPropertyFirstValue("IMAGE");
            String s = name.substring(name.lastIndexOf("/") + 1);
            String[] label = s.split("\\.");
            if (choose)
                trainingData.add(label[0]);
            else
                testData.add(label[0]);
        }
    }
    /*******************************************************/
    // Loading data from Mat file
    // if choose = true, trainData will be populated
    // if choose = false, testData will be populated

    /*******************************************************/
    private void getMatData(String file, Boolean choose, String name) throws IOException {

        File f = new File(file);

        if (!f.exists()) {
            throw new IOException(file + " does not exist!");
        }
        MatFileReader matReader = new MatFileReader(file);

        double[][] data;

        if (choose) {
            data = ((MLDouble) matReader.getMLArray(name)).getArray();
        } else
            data = ((MLDouble) matReader.getMLArray(name)).getArray();

        if (data.length > 1) {
            for (int i = 0; i < data.length; i++) {
                int imageId = (int) data[i][0];
                if (choose)
                    trainingData.add(Integer.toString(imageId));
                else
                    testData.add(Integer.toString(imageId));
            }
        }
    }

    /*******************************************************/
    // Loading Segments Ontology

    /*******************************************************/
    private void getSegmentsOntology(String file) throws IOException {

        File d = new File(file);

        if (d.exists()) {

            String line;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                String[] segmentOntologyInfo = line.split("\\t");
                if (segmentOntologyInfo.length == 3) {
                    String key = segmentOntologyInfo[0] + "-" + Integer.parseInt(segmentOntologyInfo[1]);
                    segmentOntology.put(key, segmentOntologyInfo[2].replaceAll("_", ""));
                }
            }
        }
    }

    public Hashtable<Integer, String> getMapCode2Concept() {
        return MapCode2Concept;
    }
}
