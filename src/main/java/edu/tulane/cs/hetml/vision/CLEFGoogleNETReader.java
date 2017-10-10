package edu.tulane.cs.hetml.vision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CLEFGoogleNETReader {

    public List<Image> allImages;
    public List<Image> trainImages;
    public List<Image> testImages;
    public List<Segment> allSegments;
    private Hashtable<String, String> segRefExp = new Hashtable<String, String>();

    public CLEFGoogleNETReader(String directory) throws IOException {

        File d = new File(directory);
        if (!d.exists()) {
            throw new IOException(directory + " does not exist!");
        }

        if (!d.isDirectory()) {
            throw new IOException(directory + " is not a directory!");
        }

        allImages = new ArrayList<>();
        trainImages = new ArrayList<>();
        testImages = new ArrayList<>();
        allSegments = new ArrayList<>();

        loadImages(directory + "/W2CtrainImages.txt", true);

        loadImages(directory + "/W2CtestImages.txt", false);

        getReferitText(directory);

        getFeatures(directory);

        segRefExp.clear();
    }

    private void loadImages(String file, Boolean isTrain) throws IOException {
        File f = new File(file);
        if (f.exists()) {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            line = reader.readLine();
            String[] imageIDs = (line.trim()).split(",");
            for (String id : imageIDs) {
                Image i = new Image(id.trim(), id.trim());
                if (isTrain)
                    trainImages.add(i);
                else
                    testImages.add(i);
            }
            reader.close();
        }
    }

    private void getFeatures(String directory) throws IOException {
        String file = directory + "/ImageSegmentsFeatures.txt";
        File f = new File(file);
        if (f.exists()) {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                String[] row = (line.trim()).split(",");
                Image i = new Image(row[0].trim(), row[0].trim());
                allImages.add(i);
                String key = row[0].trim() + "_" + row[1].trim() + ".jpg";
                Segment s = new Segment(row[0].trim(), Integer.parseInt(row[1].trim()), row[2].trim(),segRefExp.get(key));
                allSegments.add(s);
            }
            reader.close();
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
                segRefExp.put(segReferitText[0], segReferitText[1]);

            } else {
                segRefExp.put(segReferitText[0], " ");
            }
        }
    }
}
