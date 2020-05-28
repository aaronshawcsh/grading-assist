import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;

/**
 * This program is meant for University graders using the Moodle website to intake assignment solutions which have
 * grader-generated feedback files, unlike overwriting submission files.
 *
 * This program works by generating lists for feedback folders and files, matching these to each other, and subsequently
 * placing the right files into the right folders. By hand, this task usually takes around 15 minutes to complete. With
 * this program, it's ~20ms. Yes, milliseconds.
 *
 * Special thanks:
 * Apache Commons for creating FileUtils
 *
 * WARNING: DEVELOPED FOR NON-COMMERCIAL USE ONLY. CHECK LICENSE FILE FOR MORE DETAILS ON USAGE RIGHTS.
 *
 * @author Aaron Shaw
 * @version 1.0
 */
public class GradingAssist {
    /**
     * calls two subsequent methods that accomplish different tasks:
     * task A: generate empty folders from the submissions directory
     * task B: filling the generated folders with the correctly matched feedback files
     * @param args default parameter
     */
    public static void main(String[] args) {
        final String SUBMISSIONS_FOLDER_PATH = generateEmptyFolders();
        fillFeedbackFolders(SUBMISSIONS_FOLDER_PATH);
    }

    /**
     * reads folder names from the submissions folder, then generates empty folders with the SAME names in a new directory - without their contained submissions files
     * @return full file path of a generated text file containing names of every folder in the submissions folder
     */
    public static String generateEmptyFolders() {
        String filePath = null;
        filePath = getFolderPath("submissions");

        final String FILE_NAME = "folderNames.txt";
        File folderNamesFile = null;
        folderNamesFile = generateFile(filePath, FILE_NAME);

        final String DESTINATION_PARENT_FOLDER_PATH = filePath.substring(0, filePath.lastIndexOf('\\'));
        final String DESTINATION_PARENT_FOLDER_NAME = "Feedback Folders";
        makeDestinationParentFolder(DESTINATION_PARENT_FOLDER_PATH, DESTINATION_PARENT_FOLDER_NAME);

        final String DESTINATION_FOLDER_PATH = DESTINATION_PARENT_FOLDER_PATH + "\\" + DESTINATION_PARENT_FOLDER_NAME;
        makeDestinationFolders(folderNamesFile, DESTINATION_FOLDER_PATH);

        return filePath;
    }

    /**
     * creates the empty directory parallel to the submissions folder that will contain the generated empty feedback folders
     * @param folderPath full path of the folder right before the submissions folder
     * @param folderName name of the folder to be created
     */
    public static void makeDestinationParentFolder(String folderPath, String folderName) {
        File destinationFolder = new File(folderPath + "\\" + folderName);
        destinationFolder.mkdir();
    }

    /**
     * creates the empty feedback folders
     * @param folderNameSource the file containing the names of the folders to be generated
     * @param destinationFolderPath the path of the directory that will contain the generated feedback folders
     */
    public static void makeDestinationFolders(File folderNameSource, String destinationFolderPath) {
        ArrayList<String> destination_folder_names = readFolderNames(folderNameSource);
        for(int i = 0; i < destination_folder_names.size(); i++) {
            new File(destinationFolderPath + "\\" + destination_folder_names.get(i)).mkdir();
        }
    }

    /**
     * creates a file containing the names of every single file/folder in the folder specified, in the folder specified - wrap your head around that one :D
     * @param filePath the path of the directory whose file/folder names are to be read and documented
     * @param fileName the name of the file in which names of all the files in the folder will be documented
     * @return the file containing the names of all the files in the folder, and yes, this file shall include it's own name - you needn't worry
     */
    public static File generateFile(String filePath, String fileName) {
        final String COMMAND = "cd \"" + filePath + "\" && dir /B /O:G > \"" + filePath + "\\" +  fileName + "\"";
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", COMMAND);
        builder.redirectErrorStream(true);
        try {
            Process p = builder.start();
            p.waitFor();
        } catch(IOException e) {
            System.out.println("Error generating file. Please check read and write permissions for destination folders.");
            System.exit(0);
        } catch(InterruptedException e) {
            System.out.println("Error reading folder names using CMD. Please check read and write permissions along with the matching the formatting of the provided path.");
            System.exit(0);
        }
        return new File(filePath + "\\" + fileName);
    }

    /**
     * interacts with you :D to get the path to a specified folder
     * @param descriptor the description of the specified folder
     * @return the path of the folder you entered
     */
    public static String getFolderPath(String descriptor) {
        String folderPath = "?";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while(folderPath.equals("?")) {
                System.out.println("Please enter the full path of your " + descriptor + " folder with \\ as delimiter (including drive volume label):");
                folderPath = br.readLine();
                if(!validateFolderPath(folderPath)) folderPath = "?";
            }
        } catch(IOException e) {
            System.out.println("Error reading path input, make sure path is represented like this: C:\\some-folder\\some-other-folder");
            System.exit(0);
        }
        return folderPath;
    }

    /**
     * validates the folder path you entered for you :D - folder path is invalid if it contains any of the following characters:  &lt; &gt; : " / | ? *
     * @param folderPath the path to validate
     * @return true if path is valid, false otherwise
     */
    public static boolean validateFolderPath(String folderPath) {
        if(folderPath == null || folderPath.length() < 5) return false;
        char[] invalidCharacters = { '<', '>', '\"', '|', '?', '*' };
        if(!(Character.isUpperCase(folderPath.charAt(0)) && folderPath.substring(1, 3).equals(":\\"))) return false;
        for(int i = 0; i < invalidCharacters.length; i++)
            if(folderPath.contains(invalidCharacters[i] + "")) return false;
        return true;
    }

    /**
     * fills created empty feedback folders with correctly matched files
     * @param submissionsFolderPath the full path to the submissions folder including the drive label and the folder name "submissions"
     */
    public static void fillFeedbackFolders(String submissionsFolderPath) {
        String fileName = "folderNames.txt";
        File folderNamesFile = new File(submissionsFolderPath + "\\" + fileName);

        ArrayList<String> foldersToFill = readFolderNames(folderNamesFile);
        ArrayList<String>[] folderNames = convertFoldersForMatching(foldersToFill);

        final String FEEDBACK_FOLDER_PATH = getFolderPath("feedback files");
        fileName = "fileNames.txt";
        File fileNamesFile = generateFile(FEEDBACK_FOLDER_PATH, fileName);

        ArrayList<String> filesToMove = readFileNames(fileNamesFile);
        ArrayList<String>[] fileNames = convertFilesForMatching(filesToMove);

        boolean[] filesMoved = new boolean[filesToMove.size()];

        for(int i = 0; i < folderNames.length; i++) {
            final String FOLDER_NAME = foldersToFill.get(i);
            ArrayList<String> folderName = folderNames[i];
            int numberOfNames = folderName.size();
            ArrayList<Integer>[] matches = new ArrayList[numberOfNames];

            //if the number of matches reduces, that means there may still be more than one name to check
            int numberOfMatches = 0;
            for(int j = 0; j < numberOfNames; j++) {
                matches[j] = initializeMatches(filesMoved);
                int divisionFactor;
                do {
                    int oldSize = matches[j].size();
                    matches[j] = filter(fileNames, matches[j], folderName.get(j));
                    divisionFactor = oldSize - matches[j].size();
                } while(divisionFactor > 0);
                numberOfMatches += matches[j].size();
            }

            if(numberOfMatches == 0)
                System.out.println("MATCH_NOT_FOUND_ERROR: " + FOLDER_NAME.substring(0, FOLDER_NAME.indexOf('_')));
            else {
                int maxScoreMatch = -1;
                Map<Integer, Integer> matchesScore = new HashMap<>();
                int x = 0;
                while(maxScoreMatch == -1) {
                    maxScoreMatch = matches[x].size() > 0 ? matches[x].get(0) : maxScoreMatch;
                    x++;
                }
                for(ArrayList<Integer> nameMatch : matches) {
                    for(int j = 0; j < nameMatch.size(); j++) {
                        int key = nameMatch.get(j);
                        matchesScore.putIfAbsent(key, 0);

                        int score = matchesScore.get(key);
                        matchesScore.replace(key, score + 1);

                        int maxScore = matchesScore.get(maxScoreMatch);
                        maxScoreMatch = score > maxScore ? key : maxScoreMatch;
                    }
                }
                filesMoved[maxScoreMatch] = true;
                final String FILE_NAME = filesToMove.get(maxScoreMatch);
                moveFile(FILE_NAME, FOLDER_NAME, FEEDBACK_FOLDER_PATH, submissionsFolderPath);
            }
        }

        deleteFiles(folderNamesFile, fileNamesFile);
    }

    public static void deleteFiles(File folderNamesFile, File fileNamesFile) {
        try {
            FileUtils.forceDelete(folderNamesFile);
            FileUtils.forceDelete(fileNamesFile);
        } catch(IOException e) {
            System.out.println("Error. Cannot delete on or more generated files. I/O Exception thrown.");
            System.exit(0);
        }
    }

    public static void deleteFile(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch(IOException e) {
            System.out.println("Error. Cannot delete on or more generated files. I/O Exception thrown.");
            System.exit(0);
        }
    }

    /**
     * creates an ArrayList with all feedback folder names read from a file
     * @param folderNamesFile the file containing the names of all submission folders
     * @return an ArrayList containing the names of every feedback folder to be filled
     */
    public static ArrayList<String> readFolderNames(File folderNamesFile) {
        ArrayList<String> foldersToFill = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(folderNamesFile));
            String folderToFill = br.readLine();
            while(folderToFill != null) {
                if(!folderToFill.contains(".") && folderToFill.contains("_")) foldersToFill.add(folderToFill);
                folderToFill = br.readLine();
            }
            br.close();
        } catch(IOException e) {
            System.out.println("Error reading feedback folder names file. Please check read and write permissions for the destination folder.");
            System.exit(0);
        } finally {
            deleteFile(folderNamesFile);
        }

        return foldersToFill;
    }

    /**
     * converts the feedback folder names into delimited ArrayLists for convenient matching
     * @param foldersToFill the ArrayList containing the names of the feedback folders as solid Strings
     * @return an array of ArrayLists containing delimited(separated) feedback folder names
     */
    public static ArrayList<String>[] convertFoldersForMatching(ArrayList<String> foldersToFill) {
        ArrayList<String>[] folderNames = new ArrayList[foldersToFill.size()];

        for(int i = 0; i < folderNames.length; i++) {
            folderNames[i] = new ArrayList<String>();
            String folderName = foldersToFill.get(i);

            while(folderName.contains(" ")) {
                int splitIndex = folderName.indexOf(' ');
                folderNames[i].add(folderName.substring(0, splitIndex));
                folderName = folderName.substring(splitIndex + 1);
            }
            folderNames[i].add(folderName.substring(0, folderName.indexOf('_')));
        }

        return folderNames;
    }

    /**
     * creates an ArrayList with all feedback file names read from a file
     * @param fileNamesFile the file containing the names of all feedback files
     * @return an ArrayList containing the names of every feedback file
     */
    public static ArrayList<String> readFileNames(File fileNamesFile) {
        ArrayList<String> filesToMove = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileNamesFile));
            String fileToMove = br.readLine();
            while(fileToMove != null) {
                if(!fileToMove.contains("(")) filesToMove.add(fileToMove);
                fileToMove = br.readLine();
            }
            br.close();
        } catch(IOException e) {
            System.out.println("Error reading feedback folder names file. Please check read and write permissions for the destination folder.");
            System.exit(0);
        } finally {
            deleteFile(fileNamesFile);
        }

        return filesToMove;
    }

    /**
     * converts the feedback file names into delimited ArrayLists for convenient matching
     * @param filesToMove the ArrayList containing the names of the feedback files as solid Strings
     * @return an array of ArrayLists containing delimited(separated) feedback file names
     */
    public static ArrayList<String>[] convertFilesForMatching(ArrayList<String> filesToMove) {
        ArrayList<String>[] fileNames = new ArrayList[filesToMove.size()];

        for(int i = 0; i < fileNames.length; i++) {
            fileNames[i] = new ArrayList<String>();
            String feedbackFileName = filesToMove.get(i);
            feedbackFileName = feedbackFileName.substring(0, feedbackFileName.indexOf('.'));

            while(feedbackFileName.contains("_")) {
                int splitIndex = feedbackFileName.lastIndexOf('_');
                fileNames[i].add(feedbackFileName.substring(splitIndex + 1));
                feedbackFileName = feedbackFileName.substring(0, splitIndex);
            }
            if(feedbackFileName.length() > 1 && !feedbackFileName.contains("(")) fileNames[i].add(feedbackFileName);
        }

        return fileNames;
    }

    /**
     * initializes the list of unmatched feedback files - this is speed
     * @param filesMoved information on which feedback files have been matched and moved
     * @return an ArrayList containing the index numbers of every unmatched feedback file
     */
    public static ArrayList<Integer> initializeMatches(boolean[] filesMoved) {
        ArrayList<Integer> initializedMatches = new ArrayList<Integer>(filesMoved.length);
        for(int i = 0; i < filesMoved.length; i++) if(!filesMoved[i]) initializedMatches.add(i);
        return initializedMatches;
    }

    /**
     * moves a feedback file into its corresponding feedback folder once they have been matched
     * @param fileName the name of the feedback file to move
     * @param folderName the name of the feedback folder to which the file will be moved
     * @param feedbackFolderPath the path of the created empty feedback folders
     * @param submissionsFolderPath the path of the original submissions folder
     */
    public static void moveFile(String fileName, String folderName, String feedbackFolderPath, String submissionsFolderPath) {
        File feedbackFile = new File(feedbackFolderPath + "\\" + fileName);
        final String DESTINATION_PARENT_FOLDER_PATH = submissionsFolderPath.substring(0, submissionsFolderPath.lastIndexOf('\\')) + "\\Feedback Folders\\";
        feedbackFile.renameTo(new File(DESTINATION_PARENT_FOLDER_PATH + folderName + "\\" + fileName));
    }

    /**
     * filters matches using feedback file and folder names
     * @param fileNames the delimited names of all the feedback files
     * @param matches the array of currently active matches
     * @param folderName a part of the name of the folder for which matches are being found
     * @return the list of corresponding matches
     */
    public static ArrayList<Integer> filter(ArrayList<String>[] fileNames, ArrayList<Integer> matches, String folderName) {
        for(int i = 0; i < matches.size(); i++) {
            int indexToCheck = matches.get(i);

            ArrayList<String> feedbackFileName = fileNames[indexToCheck];

            if(!feedbackFileName.contains(folderName)) {
                matches.remove(i);
                i--;
            }
        }
        return matches;
    }
}
