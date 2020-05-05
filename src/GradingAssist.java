import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;

public class GradingAssist {
    public static void main(String[] args) throws IOException, InterruptedException {
        final String SUBMISSIONS_FOLDER_PATH = generateEmptyFolders();
        fillFeedbackFolders(SUBMISSIONS_FOLDER_PATH);
    }

    private static void fillFeedbackFolders(String submissionsFolderPath) throws IOException, InterruptedException {
        String fileName = "folderNames.txt";
        File folderNamesFile = new File(submissionsFolderPath + "\\" + fileName);

        ArrayList<String> foldersToFill = new ArrayList<String>();
        Scanner sc = new Scanner(folderNamesFile);
        while(sc.hasNextLine()) {
            String folderToFill = sc.nextLine();
            if(!folderToFill.contains(".") && folderToFill.contains("_")) foldersToFill.add(folderToFill);
        }
        sc.close();

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

        final String FEEDBACK_FOLDER_PATH = getFolderPath("feedback files");
        fileName = "fileNames.txt";
        File fileNamesFile = generateFile(FEEDBACK_FOLDER_PATH, fileName);

        ArrayList<String> filesToMove = new ArrayList<String>();
        sc = new Scanner(fileNamesFile);
        while(sc.hasNextLine()) {
            String fileToMove = sc.nextLine();
            if(!fileToMove.contains("(")) filesToMove.add(fileToMove);
        }
        sc.close();

        ArrayList<String>[] fileNames = new ArrayList[filesToMove.size()];
        for(int i = 0; i < fileNames.length; i++) {
            fileNames[i] = new ArrayList<String>();
            String feedbackFileName = filesToMove.get(i);
            feedbackFileName = feedbackFileName.substring(0, feedbackFileName.indexOf('.'));

            while(feedbackFileName.contains("_")) {
                int splitIndex = feedbackFileName.indexOf('_');
                fileNames[i].add(feedbackFileName.substring(0, splitIndex));
                feedbackFileName = feedbackFileName.substring(splitIndex + 1);
            }
            if(feedbackFileName.length() > 1 && !feedbackFileName.contains("(")) fileNames[i].add(feedbackFileName);
        }

        boolean[] filesMoved = new boolean[filesToMove.size()];

        //come up with a better system to find matches - preferably scanning across multiple names simultaneously
        //go through folderNames in outer loop, there are less feedback folders than feedback files
        //filter by one name, filter by another only if there is more than one match
        long startTime = System.currentTimeMillis();

        //this is fast
        /*
        for(int i = 0; i < folderNames.length; i++) {
            final String FOLDER_NAME = foldersToFill.get(i);
            ArrayList<String> folderName = folderNames[i];

            ArrayList<Integer> matches = new ArrayList<Integer>();
            for(int a = 0; a < fileNames.length; a++) matches.add(a);

            int name = 0;
            do {
                matches = filter(filesToMove, matches, folderName.get(name++));
            } while(matches.size() > 1 && name < folderName.size());

            int numberOfMatches = matches.size();
            label: switch(numberOfMatches) {
                case 0 : {
                    ArrayList<Integer> altMatches = new ArrayList<Integer>();
                    for(int a = 0; a < fileNames.length; a++) altMatches.add(a);

                    int altName = folderName.size()-1;
                    do {
                        altMatches = filter(filesToMove, altMatches, folderName.get(altName--));
                    } while(altMatches.size() > 1 && altName >= 0);

                    int numberOfAltMatches = altMatches.size();
                    switch(numberOfAltMatches) {
                        case 0 : System.out.println("MATCH_NOT_FOUND_ERROR: " + FOLDER_NAME.substring(0, FOLDER_NAME.indexOf('_')));
                            break label;
                        case 1 : {
                            //move feedback file with name = FEEDBACK_FOLDER_PATH + "\\" + filesToMove.get(matches.get(0)) into folder with name = FEEDBACK_FOLDER_PATH + foldersToFill.get(i)
                            final String FILE_NAME = filesToMove.get(altMatches.get(0));
                            moveFile(FILE_NAME, FOLDER_NAME, FEEDBACK_FOLDER_PATH, submissionsFolderPath);
                        }
                            break label;
                    }
                }
                default : System.out.println("TOO_MANY_MATCHES_FOUND_ERROR: " + FOLDER_NAME.substring(0, FOLDER_NAME.indexOf('_')));
                    break;
                case 1 : {
                    //move feedback file with name = FEEDBACK_FOLDER_PATH + "\\" + filesToMove.get(matches.get(0)) into folder with name = FEEDBACK_FOLDER_PATH + foldersToFill.get(i)
                    final String FILE_NAME = filesToMove.get(matches.get(0));
                    moveFile(FILE_NAME, FOLDER_NAME, FEEDBACK_FOLDER_PATH, submissionsFolderPath);
                }
                    break;
            }
        }*/

        //this is correct
        /*
        for(int i = 0; i < folderNames.length; i++) {
            final String FOLDER_NAME = foldersToFill.get(i);
            ArrayList<String> folderName = folderNames[i];
            int numberOfNames = folderName.size();
            ArrayList<Integer>[] matches = new ArrayList[numberOfNames];

            System.out.println(i + ": " + FOLDER_NAME);

            int numberOfMatches = 0;
            for(int j = 0; j < numberOfNames; j++) {
                matches[j] = initializeMatches(filesMoved);
                int divisionFactor;
                do {
                    int oldSize = matches[j].size();
                    matches[j] = filter(filesToMove, matches[j], folderName.get(j));
                    divisionFactor = oldSize - matches[j].size();
                } while(divisionFactor > 0);
                numberOfMatches += matches[j].size();
            }

            if(numberOfMatches == 0)
                System.out.println("MATCH_NOT_FOUND_ERROR: " + FOLDER_NAME.substring(0, FOLDER_NAME.indexOf('_')));
            else {
                System.out.println(i + ": 2");
                int maxScoreMatch = -1;
                Map<Integer, Integer> matchesScore = new HashMap<Integer, Integer>();
                int x = 0;
                while(maxScoreMatch == -1) {
                    maxScoreMatch = matches[x].size() > 0 ? matches[x].get(0) : maxScoreMatch;
                    x++;
                }
                System.out.println(i + ": 3");
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
        }*/

        long endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        folderNamesFile.delete();
        fileNamesFile.delete();

        //maybe an attempt for another time
        /*for(int m = 0; m < numberOfMatches; m++) {
            int minimumMatchIndex = 0;
            for(int n = 1; n < matches.length; n++) {
                int currentMatch = matches[n].get(0), minimumWatch = matches[minimumMatchIndex].get(0);
                if(currentMatch < minimumWatch) minimumMatchIndex = n;
                else if(currentMatch == minimumWatch)
            }

        }*/
    }

    public static ArrayList<Integer> initializeMatches(boolean[] filesMoved) {
        ArrayList<Integer> initializedMatches = new ArrayList<Integer>(filesMoved.length);
        for(int i = 0; i < filesMoved.length; i++) if(!filesMoved[i]) initializedMatches.add(i);
        return initializedMatches;
    }

    private static void moveFile(String fileName, String folderName, String feedbackFolderPath, String submissionsFolderPath) {
        File feedbackFile = new File(feedbackFolderPath + "\\" + fileName);
        final String DESTINATION_PARENT_FOLDER_PATH = submissionsFolderPath.substring(0, submissionsFolderPath.lastIndexOf('\\')) + "\\Empty Feedback Folders\\";
        feedbackFile.renameTo(new File(DESTINATION_PARENT_FOLDER_PATH + folderName + "\\" + fileName));
    }

    private static ArrayList<Integer> filter(ArrayList<String> filesToMove, ArrayList<Integer> matches, String folderName) {
        for(int i = 0; i < matches.size(); i++) {
            int indexToCheck = matches.get(i);
            ArrayList<String> fileName = new ArrayList<String>();

            String feedbackFileName = filesToMove.get(indexToCheck);
            feedbackFileName = feedbackFileName.substring(0, feedbackFileName.indexOf('.'));

            while(feedbackFileName.contains("_")) {
                int splitIndex = feedbackFileName.indexOf('_');
                fileName.add(feedbackFileName.substring(0, splitIndex));
                feedbackFileName = feedbackFileName.substring(splitIndex + 1);
            }
            if(feedbackFileName.length() > 1 && !feedbackFileName.contains("(")) fileName.add(feedbackFileName);

            if(!fileName.contains(folderName)) {
                matches.remove(i);
                i--;
            }
        }
        return matches;
    }

    private static String generateEmptyFolders() throws IOException, InterruptedException {
        //D:\Documents\Academics\UPEI\Sem 2\Grading CS-1910\Assignment 3\Submissions
        //D:\Documents\Academics\UPEI\Sem 2\Grading CS-1910\Assignment 3\A3
        final String FILE_PATH = getFolderPath("submissions");
        final String FILE_NAME = "folderNames.txt";
        File folderNamesFile = generateFile(FILE_PATH, FILE_NAME);

        final String DESTINATION_PARENT_FOLDER_PATH = FILE_PATH.substring(0, FILE_PATH.lastIndexOf('\\'));
        final String DESTINATION_PARENT_FOLDER_NAME = "Empty Feedback Folders";
        makeDestinationParentFolder(DESTINATION_PARENT_FOLDER_PATH, DESTINATION_PARENT_FOLDER_NAME);

        final String DESTINATION_FOLDER_PATH = DESTINATION_PARENT_FOLDER_PATH + "\\" + DESTINATION_PARENT_FOLDER_NAME;
        makeDestinationFolders(folderNamesFile, DESTINATION_FOLDER_PATH);

        return FILE_PATH;
    }

    private static void makeDestinationFolders(File folderNameSource, String destinationFolderPath) throws IOException {
        Scanner sc = new Scanner(folderNameSource);
        while(sc.hasNextLine()) {
            final String DESTINATION_FOLDER_NAME = sc.nextLine();
            if(!DESTINATION_FOLDER_NAME.contains(".")) new File(destinationFolderPath + "\\" + DESTINATION_FOLDER_NAME).mkdir();
        }
        File redundantFolder = new File(destinationFolderPath + "\\" + destinationFolderPath.substring(destinationFolderPath.lastIndexOf('\\')));
        FileUtils.deleteDirectory(redundantFolder);
        sc.close();
    }

    private static void makeDestinationParentFolder(String folderPath, String folderName) {
        File destinationFolder = new File(folderPath + "\\" + folderName);
        destinationFolder.mkdir();
    }

    private static File generateFile(String filePath, String fileName) throws IOException, InterruptedException {
        String command = "cd \"" + filePath + "\" && dir /B /O:G > \"" + filePath + "\\" +  fileName + "\"";
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        p.waitFor();
        return new File(filePath + "\\" + fileName);
    }

    private static String getFolderPath(String descriptor) {
        String folderPath = "?";
        Scanner sc = new Scanner(System.in);
        while(folderPath.equals("?")) {
            System.out.println("Please enter the full path of your " + descriptor + " folder with \\ as delimiter (including drive volume label):");
            folderPath = sc.nextLine();
            if(!validateFilePath(folderPath)) folderPath = "?";
        }
        return folderPath;
    }

    //file path is invalid if it contains <>:"/|?*
    private static boolean validateFilePath(String filePath) {
        if(filePath.length() < 5) return false;
        char[] invalidCharacters = { '<', '>', '\"', '|', '?', '*' };
        if(!(Character.isUpperCase(filePath.charAt(0)) && filePath.substring(1, 3).equals(":\\"))) return false;
        for(int i = 0; i < invalidCharacters.length; i++)
            if(filePath.contains(invalidCharacters[i] + "")) return false;
        return true;
    }
}
