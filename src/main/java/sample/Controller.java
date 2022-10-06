package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.File;

public class Controller {

    public Button selectFile;
    public Button selectFolder;
    public Label label;
    public CheckBox create_folder;
    public CheckBox change_file_name;
    public MenuBar menuBar;
    public MenuItem info;
    public String folderPath;

    @FXML
    void fileSelector(ActionEvent event) throws IOException {
        label.setText("");
        Window window = ((Node) (event.getSource())).getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(window);
        if (create_folder.isSelected() && change_file_name.isSelected()) {
            label.setText(imageCoppyToFolder(folderPath, file));
        } else if (create_folder.isSelected() && !change_file_name.isSelected()) {
            label.setText(imageCoppyToFolderOldNames(folderPath, file));
        } else if (!create_folder.isSelected() && change_file_name.isSelected()) {
            label.setText(imageCoppy(folderPath, file));
        } else if (!create_folder.isSelected() && !change_file_name.isSelected()) {
            label.setText(imageCoppyOldNames(folderPath, file));
        }
        event.consume();
    }


    public static final Thread.UncaughtExceptionHandler ALERT_EXCEPTION_HANDLER = (thread, cause) -> {
        try {
            cause.printStackTrace();
            final Runnable showDialog = () -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("An unknown error occurred");
                alert.showAndWait();
            };
            if (Platform.isFxApplicationThread()) {
                showDialog.run();
            } else {
                FutureTask<Void> showDialogTask = new FutureTask<Void>(showDialog, null);
                Platform.runLater(showDialogTask);
                showDialogTask.get();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            System.exit(-1);
        }
    };



    @FXML
    void folderSelector(ActionEvent event) throws IOException {
        label.setText("");
        Window window = ((Node) (event.getSource())).getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(window);
        String path = selectedDirectory.getAbsolutePath();
        if(selectedDirectory == null){
            label.setText("Nie wybrano folderu");
        }else{
            label.setText(path);
            folderPath = path;
        }
        event.consume();
    }

    @FXML
    void showInfo(ActionEvent event) throws IOException {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText("Plik do importu:");
        String s = "Format: .csv, .txt - kolumny rozdzielane średnikami \n" +
                "Kolumna I: Nazwa szukanego pliku \n" +
                "Kolumna II: Nowa nazwa \n" +
                "\n" +
                "Przykład:" +
                "\n" +
                "Szukany_plik.jpg;Nowa_nazwa.jpg\n" +
                "Szukany_plik_2.jpg;Nowa_nazwa_2.jpg\n" +
                "\n" +
                "Zaznaczony chceckbox powoduje kopiowanie zdjęc do tworzonych folderów o nazwie pliku";
        alert.setContentText(s);
        alert.show();
    }

    public static List<Path> findByFileName(Path path, String fileName)
            throws IOException {

        List<Path> result;
        try (Stream<Path> pathStream = Files.find(path,
                Integer.MAX_VALUE,
                (p, basicFileAttributes) ->
                        p.getFileName().toString().equalsIgnoreCase(fileName))
        ) {
            result = pathStream.collect(Collectors.toList());
        }
        return result;
    }

    public String imageCoppy(String directory, File file) throws IOException {
        try (
                InputStream inputStream = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.newFormat(';'));
        ) {
            Path path = Paths.get(directory);
            for (CSVRecord csvRecord : csvParser) {
                String fileName = csvRecord.get(0);
                String newFileName = csvRecord.get(1);
                List<Path> result = findByFileName(path, fileName);

                for (Path p : result) {
                    File dest = new File(path + "/kopie/" + newFileName);
                    long destSize = dest.length();

                    System.out.println("dest file size - " + destSize);
                    File source = new File(p.toString());
                    long sourceSize = source.length();
                    String fileNameFromPath = p.toString().replaceAll("\\\\", " ");
                    System.out.println("source file size - " + sourceSize);

                    if (fileNameFromPath.endsWith(fileName) && (destSize > sourceSize)) {
                        System.out.println(fileNameFromPath + " nie skopiowano: file size: " + sourceSize + " - " + p);
                    }
                    if (fileNameFromPath.endsWith(fileName) && (destSize < sourceSize)) {
                        try {
                            FileUtils.copyFile(source, dest);
                            System.out.println(fileNameFromPath + " Skopiowano: file size: " + sourceSize + " - " + p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
        Platform.runLater(() -> {
            Alert dialog = new Alert(Alert.AlertType.INFORMATION, "Sprawdz czy plik zawiera prawidłowe kolumny \n" +
                    "\n" +
                    "W przypadku wyboru zmiana nazwy pierwsza kolumna powinna zawierac nazwę szukanego pliku a druga nową nazwe", ButtonType.OK);
            dialog.setTitle("Info");
            dialog.setHeaderText("Błąd w pliku.");
            dialog.show();

        });
    }
        return "Utworzono folder: " + folderPath + "\\kopie";
    }

    public String imageCoppyOldNames(String directory, File file) throws IOException {
        try (
                InputStream inputStream = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.newFormat(';'));
        ) {
            Path path = Paths.get(directory);
            for (CSVRecord csvRecord : csvParser) {
                String fileName = csvRecord.get(0);
                List<Path> result = findByFileName(path, fileName);

                for (Path p : result) {
                    File dest = new File(path + "/kopie/" + fileName);
                    long destSize = dest.length();

                    System.out.println("dest file size - " + destSize);
                    File source = new File(p.toString());
                    long sourceSize = source.length();
                    String fileNameFromPath = p.toString().replaceAll("\\\\", " ");
                    System.out.println("source file size - " + sourceSize);

                    if (fileNameFromPath.endsWith(fileName) && (destSize > sourceSize)) {
                        System.out.println(fileNameFromPath + " nie skopiowano: file size: " + sourceSize + " - " + p);
                    }
                    if (fileNameFromPath.endsWith(fileName) && (destSize < sourceSize)) {
                        try {
                            FileUtils.copyFile(source, dest);
                            System.out.println(fileNameFromPath + " Skopiowano: file size: " + sourceSize + " - " + p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Utworzono folder: " + directory + "\\kopie";
    }

    public String imageCoppyToFolder(String directory, File file) throws IOException {
        try (
                InputStream inputStream = new FileInputStream(file);

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.newFormat(';'));
        ) {

            Path path = Paths.get(directory);

            for (CSVRecord csvRecord : csvParser) {
                String fileName = csvRecord.get(0);
                String newFileName = csvRecord.get(1);
                String folderName = newFileName.substring(0, newFileName.lastIndexOf('.'));
                List<Path> result = findByFileName(path, fileName);

                for (Path p : result) {
//                    File dest = new File(path + "/kopie/" + fileName);
                    File dest = new File(path + "/kopie/" + folderName + "/" + newFileName);
                    long destSize = dest.length();

                    System.out.println("dest file size - " + destSize);
                    File source = new File(p.toString());
                    long sourceSize = source.length();
                    String fileNameFromPath = p.toString().replaceAll("\\\\", " ");
                    System.out.println("source file size - " + sourceSize);

                    if (fileNameFromPath.endsWith(fileName) && (destSize > sourceSize)) {
                        System.out.println(fileNameFromPath + " nie skopiowano: file size: " + sourceSize + " - " + p);
                    }
                    if (fileNameFromPath.endsWith(fileName) && (destSize < sourceSize)) {
                        try {
                            FileUtils.copyFile(source, dest);
                            System.out.println(fileNameFromPath + " Skopiowano: file size: " + sourceSize + " - " + p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert dialog = new Alert(Alert.AlertType.WARNING, "Nieprawidłowy plik", ButtonType.OK);
                dialog.show();

            });
        }
        return "Utworzono folder: " + folderPath + "\\kopie";
    }

    public String imageCoppyToFolderOldNames(String directory, File file) throws IOException {
        try (
                InputStream inputStream = new FileInputStream(file);

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.newFormat(';'));
        ) {
            Path path = Paths.get(directory);

            for (CSVRecord csvRecord : csvParser) {
                String fileName = csvRecord.get(0);
//                String newFileName = csvRecord.get(1);
                String folderName = fileName.substring(0, fileName.lastIndexOf('.'));
                List<Path> result = findByFileName(path, fileName);

                for (Path p : result) {
//                    File dest = new File(path + "/kopie/" + fileName);
                    File dest = new File(path + "/kopie/" + folderName + "/" + fileName);
                    long destSize = dest.length();

                    System.out.println("dest file size - " + destSize);
                    File source = new File(p.toString());
                    long sourceSize = source.length();
                    String fileNameFromPath = p.toString().replaceAll("\\\\", " ");
                    System.out.println("source file size - " + sourceSize);

                    if (fileNameFromPath.endsWith(fileName) && (destSize > sourceSize)) {
                        System.out.println(fileNameFromPath + " nie skopiowano: file size: " + sourceSize + " - " + p);
                    }
                    if (fileNameFromPath.endsWith(fileName) && (destSize < sourceSize)) {
                        try {
                            FileUtils.copyFile(source, dest);
                            System.out.println(fileNameFromPath + " Skopiowano: file size: " + sourceSize + " - " + p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Utworzono folder: " + folderPath + "\\kopie";
    }

    public static void copyFileUsingApache(File from, File to) throws IOException {
        FileUtils.copyFile(from, to);
    }
}
