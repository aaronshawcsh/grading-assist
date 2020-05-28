## ***grading-assist usage instructions***

The algorithm works in the following way:

1. Before it begins, you must be ready with two folder paths:
    1. one, to the Submissions folder which contains subfolders that correspond to<br>
    student-submitted assignments, and
    2. the other, to the grader-created folder containing the feedback files, say A3<br>
    or any valid folder path.

2. First, it reads the names of the student-submitted folders in the submissions folder<br>
where assignment data lie.

3. It then creates a folder called Feedback Folders in the parent folder of the submissions<br>
folder (1 level up).

4. Following that, it reads through every legal student-submitted folder name and creates<br>
an empty folder with the same name inside the Feedback Folder created in 2.

5. Then, it proceeds to read the names of every feedback file in the grader-generated<br>
feedback file folder.

6. Here’s the crux of the algorithm:
    1. It splits the names of the newly-created empty feedback folders using the underscore<br>
    ( _ ) character between student names.
    2. It splits the names of the feedback files in the grader-created feedback files folder<br>
    using the whitespace (   ) character and truncating the file extension (for eg. “.docx”).
    3. It then proceeds to match these folders and files together using a score-based<br>
    matching system, which eliminates and ignores previously-moved feedback file<br>
    names when it the current folder. It goes through feedback folders first due to some<br> 
    students undoubtedly missing the assignment submission, which makes Moodle not<br>
    generate a submission folder for graders to download.
    
7. It then cleans up the temporary requirements, such as text files holding all the student<br>
feedback folder names and student feedback file names. Graders are encouraged to<br>
manually delete these files (in case the program fails to do so) if an exception is<br>
encountered during runtime.