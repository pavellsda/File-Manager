package com.med.controllers;

import com.med.utils.History;
import com.med.utils.PathTreeItem;
import com.med.utils.TableCellExtension;
import com.med.utils.TreeCellExtension;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.med.utils.Utils.alert;
import static java.lang.String.valueOf;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by pavellsda on 21.01.17.
 */


public class MainWindowController {

    @FXML
    public TreeView<String> foldersTree; //Дерево каталогов
    public TableView fileTable; //Таблица файлов, находящихся в выбранном каталоге
    public Label currentFolderLabel; //Отображает текущий каталог
    public Button backButton; //Кнопка для возрата в предыдущий каталог
    public Button forwardButton; //Кнопка для перехода в следующий каталог в истории
    public Button findFileButton; //Кнопка поиска файла по имени/части имени
    public Button addDirButton; //Кнопка для создания каталога
    public ListView<String> listView; //Список стандартных каталогов(Home, Documents...)
    private final ContextMenu popupMenu = new ContextMenu(); //Меню, вызываемое по правому щелчку мыши
    public ListView<String> listFind; //Список, выводящий результаты поиска
    
    private StringProperty currentPath; //Текущий путь к каталогу
    private History history; //Класс истории(для перемещения вперед/назад)
    private Map<Path, Image> iconImgCache = new HashMap<>(); //Контейнер для хранения иконок каталогов/файлов
    private File[] filesToCopy; //Хранит файлы для копирования
    private File[] filesToMove; //Хранит файлы для перемещения
    private boolean cutCheck = false; //Если перемещается/копируется директория, то true
    private String osName;

    public MainWindowController() {
    }


    @FXML
    private void initialize() throws IOException {

        currentPath = new SimpleStringProperty();
        history = new History();

        setOs(getOS());

        initListFind(); //Инициализация списка поиска
        initListView(); //Инициализация списка основных каталогов
        initContextMenu(); //Инициализация меню
        initDirTree(); //Инициализация дерева каталогов
        initProperty(); //Инициализация отображения каталогов
        initFileTable(); //Инициализация таблицы файлов

        /*
         Устанавливаем домашний каталог
         */
        setCurrentPath(System.getProperty("user.home"));
        history.add(System.getProperty("user.home"));
        currentFolderLabel.setText(System.getProperty("user.home"));
    }

    private void initListFind(){
        listFind.setVisible(false);

        /*
          Обработчик событий для списка поиска
          Каждый элемент списка отображает найденный файл
          При нажатии происходит переход в каталог с выбранным файлом
         */
        listFind.setOnMouseClicked(event -> {
            disableFindTable();

            String path = listFind.getSelectionModel().getSelectedItem();
            String[] availablePath = path.split("`");

            setCurrentPath(availablePath[0]);
            history.add(availablePath[0]);
            currentFolderLabel.setText(getCurrentPath());
            listFind.getItems().clear();
        });
    }
    private void initListView(){
        listView.getItems().addAll(FXCollections.observableList(Arrays.asList("Home",
                "Downloads", "Documents", "Music", "Pictures", "Trash")));

        /*
          Обработчик событий для списка основных каталогов
          При нажатии происходит переход в выбранный каталог
         */
        listView.setOnMouseClicked(event -> {

            disableFindTable();

            String path = System.getProperty("user.home");
            String choice = listView.getSelectionModel().getSelectedItem();

            switch (choice) {
                case "Home":
                    break;
                case "Trash":
                    if(Objects.equals(osName, "windows"))
                        path = "C:\\$Recycle.bin";
                    if(Objects.equals(osName, "linux"))
                        path = path + "/.local/share/Trash/files";
                    break;
                default:
                    path = path+"/"+choice;
                    break;
            }


            setCurrentPath(path);
            history.add(path);
            currentFolderLabel.setText(getCurrentPath());

        });

    }

    private void initContextMenu(){
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        MenuItem delete = new MenuItem("Delete");
        popupMenu.getItems().addAll(cut, copy, paste, delete);

        /*
          Обработчик событий для меню
          Обработчик привязывается к конкретной функции, например к функции menuCut(ev)
         */
        cut.setOnAction((event) -> menuCut());
        copy.setOnAction((event) -> menuCopy());
        paste.setOnAction((event) -> menuPaste());
        delete.setOnAction((event) -> menuDelete());
    }

    private void initDirTree(){
        TreeItem<String> rootItem = new TreeItem<>("root");
        for (Path pathToRoot : FileSystems.getDefault().getRootDirectories()) {
            TreeItem<String> treeItem = new PathTreeItem(pathToRoot);
            rootItem.getChildren().add(treeItem);
        }
        
        foldersTree.setCellFactory((param) -> new TreeCellExtension());

        foldersTree.setOnKeyReleased(event -> {
            System.out.println("##"+event.toString());
            if (event.getCode() == KeyCode.ENTER) {

                PathTreeItem selectedItem = (PathTreeItem) foldersTree.getSelectionModel().getSelectedItem();
                setCurrentPath(selectedItem.myPath.toString());
                history.add(selectedItem.myPath);
            }
        });

        foldersTree.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                PathTreeItem selectedItem = (PathTreeItem) foldersTree.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    setCurrentPath(selectedItem.myPath.toString());
                    history.add(selectedItem.myPath);
                    System.out.println(selectedItem.myPath);
                }
            }
        });

        foldersTree.setRoot(rootItem);
        foldersTree.setShowRoot(false);
    }

    @SuppressWarnings("unchecked")
    private void initProperty() {
        iconImgCache.clear();
        /*
          Обработчик событий для перехода между директориями
         */
        currentPath.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            ObservableList<Path> pathList = FXCollections.observableArrayList();
            try {
                DirectoryStream<Path> dir= Files.newDirectoryStream(Paths.get(newValue));
                for (Path file : dir) {
                    pathList.add(file);
                }
            } catch (IOException e) {
                alert("Directory not found");
            }
            ObservableList<Path> pathList_ = FXCollections.observableArrayList();
            pathList.stream()
                    .sorted((pathToFile, pathToFile_) -> {
                        if (Files.isDirectory(pathToFile) != Files.isDirectory(pathToFile_)) {
                            return (Files.isDirectory(pathToFile)) ? -1 : 1;
                        }
                        return pathToFile.compareTo(pathToFile_);
                    })
                    .forEach(pathList_::add);
            fileTable.setItems(pathList_);
        });
    }

    @SuppressWarnings("unchecked")
    private void initFileTable() {
        fileTable.getColumns().clear();

        /*
          Создание столбцов таблицы и их заполнение
         */

        TableColumn<Path, Path> nameColumn = new TableColumn<>("File name");
        nameColumn.setCellValueFactory(param -> new ObjectBinding<Path>() {
            @Override
            protected Path computeValue() {
                return param.getValue();
            }
        });
        nameColumn.setCellFactory(param -> {
            TableCellExtension tbce = new TableCellExtension();
            tbce.setIconImgCache(iconImgCache);
            return tbce;
        });
        nameColumn.setPrefWidth(200);
        fileTable.getColumns().add(nameColumn);

        TableColumn<Path, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(param -> new StringBinding() {
            @Override
            protected String computeValue() {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                    return sdf.format(new Date(Files.getLastModifiedTime(param.getValue()).toMillis()));
                } catch (IOException e) {
                    alert(e.getMessage());
                    return "";
                }
            }
        });
        dateColumn.setPrefWidth(200);
        fileTable.getColumns().add(dateColumn);

        TableColumn<Path, String> sizeColumn = new TableColumn<>("File size");
        sizeColumn.setCellValueFactory((TableColumn.CellDataFeatures<Path, String> file) -> new StringBinding() {
             @Override
             protected String computeValue() {
                 if (Files.isDirectory(file.getValue())) return "dir";
                 try {
                     return getFileSizeStr(Files.size(file.getValue()));
                 } catch (IOException e) {
                     alert(e.getMessage());
                     return "-";
                 }
             }

             private String getFileSizeStr(long size) {

                 if (size < 1024) return size + " B";
                 else {
                     return String.format("%,d KB", (size / 1024));
                 }
             }
         });
        sizeColumn.setPrefWidth(110);

        sizeColumn.setCellFactory(param -> new TableCell<Path, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(item);
                this.setAlignment(Pos.CENTER_RIGHT);
            }
        });

        fileTable.getColumns().add(sizeColumn);

        fileTable.setOnMouseClicked(event -> {
            if(event.isControlDown())
                fileTable.getSelectionModel().setSelectionMode(
                        SelectionMode.MULTIPLE);
            else
                fileTable.getSelectionModel().setSelectionMode(
                        SelectionMode.SINGLE);
            if (event.getButton() == MouseButton.PRIMARY) {
                if (event.getClickCount() == 2 && fileTable.getSelectionModel().getSelectedItem() != null) {
                    Path path = (Path) fileTable.getSelectionModel().getSelectedItem();
                    if (Files.isDirectory(path)) {
                        setCurrentPath(path.toString());
                        history.add(path);
                        currentFolderLabel.setText(valueOf(path));
                    } else {
                        Thread thr = new Thread(()->{
                            try {
                                Desktop.getDesktop().open(path.toFile());
                            } catch (IOException e) {
                                alert(e.getMessage());
                            }
                        });
                        thr.start();

                    }
                }

            }
            else {

                popupMenu.show(fileTable, event.getScreenX(), event.getScreenY());
            }
        });

        fileTable.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.RIGHT && !event.isAltDown()) {
                Path path = (Path) fileTable.getSelectionModel().getSelectedItem();
                if (path != null && Files.isDirectory(path)) {
                    setCurrentPath(path.toString());
                    history.add(path);
                    currentFolderLabel.setText(valueOf(path));
                    fileTable.getSelectionModel().selectFirst();
                    fileTable.scrollTo(0);
                }
            } else if (event.getCode() == KeyCode.LEFT && !event.isAltDown()) {
                Path oldPath = Paths.get(getCurrentPath());
                setCurrentPath(oldPath.getParent().toString());
                history.add(oldPath.getParent());
                fileTable.getSelectionModel().select(oldPath);
                fileTable.scrollTo(oldPath);
            }
        });
    }

    private void menuCut(){
        if(fileTable.getSelectionModel().getSelectedItems().size()>0) {
            ObservableList itemsToCopy = fileTable.getSelectionModel().getSelectedItems();
            Path pathToCopiesFile[] = new Path[itemsToCopy.size()];
            filesToCopy = new File[itemsToCopy.size()];
            for (int i = 0; i < itemsToCopy.size(); i++) {
                pathToCopiesFile[i] = (Path) itemsToCopy.get(i);
                if (Files.isDirectory(pathToCopiesFile[i])) {
                    filesToCopy[i] = pathToCopiesFile[i].toFile();
                    filesToMove = filesToCopy;
                    cutCheck = true;
                } else {
                    filesToCopy[i] = pathToCopiesFile[i].toFile();
                    filesToMove = filesToCopy;
                }
            }
        }
    }

    /*
     * Обработчик кнопки copy в popupMenu
     */
    private void menuCopy(){
        if(fileTable.getSelectionModel().getSelectedItems().size()>0) {// Проверка выбран ли файл для копирования
            ObservableList itemsToCopy = fileTable.getSelectionModel().getSelectedItems();// Записываем все выбранные файлы в таблице
            Path path[] = new Path[itemsToCopy.size()];
            filesToCopy = new File[itemsToCopy.size()];
            for (int i = 0; i < itemsToCopy.size(); i++) {
                path[i] = (Path) itemsToCopy.get(i);
                if (Files.isDirectory(path[i])) {
                    filesToCopy[i] = path[i].toFile();
                    cutCheck = true;

                } else {
                    filesToCopy[i] = path[i].toFile();
                }
            }
        }
       //  = (Path) fileTable.getSelectionModel().getSelectedItems();

    }

    /*
     * Обработчик кнопки delete в popupMenu
     */
    private void menuDelete(){
        if(fileTable.getSelectionModel().getSelectedItems().size()>0) { //Выбран ли файл для удаления
            ObservableList itemsToDelete = fileTable.getSelectionModel().getSelectedItems();
            Path path[] = new Path[itemsToDelete.size()]; //Создание массива путей к файлам для удаления
            for (int i = 0; i < itemsToDelete.size(); i++) {
                if (fileTable.getSelectionModel().getSelectedItem() != null) {
                    path[i] = (Path) itemsToDelete.get(i);
                    delete(path[i].toFile()); // Вызов функции delete из com.med.Utils для удаления файлов
                }
            }
            updatePath();

        }
    }

    /*
     * Обработчик кнопки paste в popupMenu
     */

    private void menuPaste() {
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                if (filesToCopy != null)
                    for (int i = 0; i < filesToCopy.length; i++) {
                        if (fileTable.getSelectionModel().getSelectedItem() != null) {
                            Path path = (Path) fileTable.getSelectionModel().getSelectedItem();

                            if (Files.isDirectory(path)) {

                                try {
                                    if (!cutCheck) {
                                        String target = path.toString() + "/" + filesToCopy[i].getName();
                                        path = Paths.get(target);
                                        if (filesToMove == null)
                                            copyDir(filesToCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(filesToCopy[i], path.toFile(), false);
                                            if (i == filesToCopy.length - 1)
                                                filesToMove = null;

                                        }
                                    } else {
                                        cutCheck = false;
                                        if (filesToMove == null)
                                            copyDir(filesToCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(filesToCopy[i], path.toFile(), true);
                                            if (i == filesToCopy.length - 1)
                                                filesToMove = null;
                                        }
                                    }
                                } catch (IOException e) {
                                    alert(e.getMessage());
                                }

                            }
                        } else {
                            String target = getCurrentPath();
                            if (Files.isDirectory(Paths.get(target))) {
                                try {
                                    if (!cutCheck) {
                                        target = target + "/" + filesToCopy[i].getName();
                                        Path path = Paths.get(target);
                                        if (filesToMove == null)
                                            copyDir(filesToCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(filesToCopy[i], path.toFile(), true);
                                            if (i == filesToCopy.length - 1)
                                                filesToMove = null;
                                        }
                                    } else {
                                        cutCheck = false;
                                        target = target + "/" + filesToCopy[i].getName();
                                        Path path = Paths.get(target);

                                        if (filesToMove == null)
                                            copyDir(filesToCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(filesToCopy[i], path.toFile(), true);
                                            if (i == filesToCopy.length - 1)
                                                filesToMove = null;
                                        }
                                    }
                                } catch (IOException e) {
                                    alert(e.getMessage());
                                }
                            }
                        }
                        updatePath();
                    }
                return null;
            }
        };

        new Thread(task).start();
    }

    /*
     * Обработчик кнопки next
     * Осуществляет переход в следующий каталог в истории
     */
    @FXML
    public void next(){
            Path path = history.next();
            setCurrentPath(path.toString());
            currentFolderLabel.setText(valueOf(path));
    }
    /*
     * Обработчик кнопки back
     * Осуществляет переход в предыдущий каталог в истории
     */
    @FXML
    public void back(){
            Path path = history.back();
            setCurrentPath(path.toString());
            currentFolderLabel.setText(valueOf(path));
    }

    /*
     * Обработчик кнопки addDir
     * Создает новый каталог
     */
    @FXML
    public void addDir(){

        //Создание диалогового окна, где происходит ввод названия нового каталога
        TextInputDialog dialog = new TextInputDialog("folderName");
        dialog.setTitle("Create Folder");
        dialog.setContentText("Please enter folder name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            String folderName = "/"+result.get();
            String pathToFile = getCurrentPath()+folderName;

           createDir(new File(pathToFile));
        }
        updatePath();
    }

    /*
     * Обработчик кнопки findFile
     * Осуществляет поиск по имени/части имени файла
     */
    @FXML
    public void findFile(){
        //Создание диалогового окна для ввода имени файла для поиска
        TextInputDialog dialog = new TextInputDialog("Find");
        dialog.setTitle("Find");
        dialog.setContentText("Please enter file name/folder name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            String name = result.get();
            listFind.setVisible(true);
            fileTable.setVisible(false);

            Thread thr = new Thread(() -> {
                try {
                    findFile(name, Paths.get(getCurrentPath()).toFile()); //Вызов функции поиска
                    Thread.sleep(200);
                } catch (IOException | InterruptedException e) {
                    alert(e.getMessage());
                }
            });
            thr.start();

        }

    }

    /*
     * Функция поиска файла
     * Создает список файлов в текущем каталоге и в подкаталогах
     * Далее происходит сравнение списка файлов с искомым файлом
     */


    private void findFile(String name, File directory)throws IOException {

        File[] list = directory.listFiles();
        if (list != null) {
            for (File fileInDirectory : list) {
                if (fileInDirectory.isDirectory()) {
                    try {
                        findFile(name, fileInDirectory);
                    } catch (IOException e) {
                       alert(e.getMessage());
                    }
                } else if (fileInDirectory.getName().contains(name))
                    Platform.runLater(() -> listFind.getItems().add(fileInDirectory.getParentFile().toString() + "`" + fileInDirectory.getName()));

            }
        }
    }


    /*
     * Функция для обновления текущего каталога(очень плохо)
     */
    private void updatePath(){

        String current_path = getCurrentPath();
        Path updatePath = Paths.get(System.getProperty("user.home"));
        setCurrentPath(updatePath.toString());
        setCurrentPath(current_path);

    }

    private void disableFindTable(){

        listFind.setVisible(false);
        fileTable.setVisible(true);
    }

    /*
     * Функция для копирования/перемещения файлов
     * Происходит проверка является ли файл каталогом, если да, то все файлы в нем копируются/перемещаются
     * И так пока не функция не дойдет до конца
     */
    private void copyDir(File src, File dest, boolean cut)
            throws IOException{


        if(src.isDirectory()){

            if(!dest.exists()){
                createDir(dest);
            }

            String files[] = src.list();

            assert files != null;
            for (String file : files) {

                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                copyDir(srcFile,destFile, cut);
            }

        }else{

            if(!cut)
                Files.copy(src.toPath(), dest.toPath(), REPLACE_EXISTING);
            else
                Files.move(src.toPath(), dest.toPath(), REPLACE_EXISTING);
        }
        if(cut)
            delete(src);

    }

    private static boolean delete(File file) {

        if (file.isDirectory()) {

            String[] files = file.list();

            assert files != null;
            if (files.length == 0) {

                return file.delete();

            } else {


                for (String temp : files) {

                    File fileDelete = new File(file, temp);

                    delete(fileDelete);
                }

                String[] fileList = file.list();

                assert fileList != null;
                if (fileList.length == 0) {
                    return file.delete();

                }
            }
        } else {
            return file.delete();
        }

        return false;
    }

    private String getOS(){
        return System.getProperty("os.name").toLowerCase();
    }

    private void setOs(String osName){this.osName = osName;}

    private boolean createDir(File fileToCreate){
        return fileToCreate.mkdirs();
    }

    /*
     * Функция для установки текущего каталога
     */
    private void setCurrentPath(String currentPath) { this.currentPath.set(currentPath); }
    /*
     * Функция получения пути текущего каталога
     */
    private String getCurrentPath() { return currentPath.get(); }


}