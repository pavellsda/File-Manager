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

import static com.med.utils.Utils.delete;
import static java.lang.String.valueOf;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by pavellsda on 21.01.17.
 */


public class MainWindowController {

    @FXML
    public TreeView<String> tree_folders_list; //Дерево каталогов
    public TableView tableView; //Таблица файлов, находящихся в выбранном каталоге
    public Label label_current_folder; //Отображает текущий каталог
    public Button backButton; //Кнопка для возрата в предыдущий каталог
    public Button forwardButton; //Кнопка для перехода в следующий каталог в истории
    public Button findFileButton; //Кнопка поиска файла по имени/части имени
    public Button addDirButton; //Кнопка для создания каталога
    public ListView<String> listView; //Список стандартных каталогов(Home, Documents...)
    private final ContextMenu contextMenu = new ContextMenu(); //Меню, вызываемое по правому щелчку мыши
    public ListView<String> listFind; //Список, выводящий результаты поиска

    private StringProperty curPath; //Текущий путь к каталогу
    private History history; //Класс истории(для перемещения вперед/назад)
    private Map<Path, Image> iconImgCache = new HashMap<>(); //Контейнер для хранения иконок каталогов/файлов
    private File[] toCopy; //Хранит файлы для копирования
    private File[] toMove; //Хранит файлы для перемещения
    private boolean directoryMove = false; //Если перемещается/копируется директория, то true

    public MainWindowController() {
    }


    @FXML
    private void initialize() throws IOException {

        curPath = new SimpleStringProperty();
        history = new History();

        initListFind(); //Инициализация списка поиска
        initListView(); //Инициализация списка основных каталогов
        initContextMenu(); //Инициализация меню
        initDirTree(); //Инициализация дерева каталогов
        initProperty(); //Инициализация отображения каталогов
        initFileTable(); //Инициализация таблицы файлов

        /*
         Устанавливаем домашний каталог
         */
        setCurPath(System.getProperty("user.home"));
        history.add(System.getProperty("user.home"));
        label_current_folder.setText(System.getProperty("user.home"));
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

            setCurPath(availablePath[0]);
            history.add(availablePath[0]);
            label_current_folder.setText(getCurPath());
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

            if(!Objects.equals(choice, "Home"))
                path = path+"/"+choice;

            setCurPath(path);
            history.add(path);
            label_current_folder.setText(getCurPath());

        });

    }

    private void initContextMenu(){
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        MenuItem delete = new MenuItem("Delete");
        contextMenu.getItems().addAll(cut, copy, paste, delete);

        /*
          Обработчик событий для меню
          Обработчик привязывается к конкретной функции, например к функции menuCut(ev)
         */
        cut.setOnAction((ev) -> menuCut());
        copy.setOnAction((ev) -> menuCopy());
        paste.setOnAction((ev1) -> menuPaste());
        delete.setOnAction((ev) -> menuDelete());
    }

    private void initDirTree(){
        TreeItem<String> rootItem = new TreeItem<>("root");
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            TreeItem<String> treeItem = new PathTreeItem(p);
            rootItem.getChildren().add(treeItem);
        }

        tree_folders_list.setCellFactory((p) -> new TreeCellExtension());

        tree_folders_list.setOnKeyReleased(ev -> {
            System.out.println("##"+ev.toString());
            if (ev.getCode() == KeyCode.ENTER) {

                PathTreeItem selectedItem = (PathTreeItem)tree_folders_list.getSelectionModel().getSelectedItem();
                setCurPath(selectedItem.myPath.toString());
                history.add(selectedItem.myPath);
            }
        });

        tree_folders_list.setOnMouseClicked(ev -> {
            if (ev.getButton() == MouseButton.PRIMARY) {
                PathTreeItem selectedItem = (PathTreeItem)tree_folders_list.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    setCurPath(selectedItem.myPath.toString());
                    history.add(selectedItem.myPath);
                    System.out.println(selectedItem.myPath);
                }
            }
        });

        tree_folders_list.setRoot(rootItem);
        tree_folders_list.setShowRoot(false);
    }

    @SuppressWarnings("unchecked")
    private void initProperty() {
        iconImgCache.clear();
        /*
          Обработчик событий для перехода между директориями
         */
        curPath.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            ObservableList<Path> l = FXCollections.observableArrayList();
            try {
                DirectoryStream<Path> dir= Files.newDirectoryStream(Paths.get(newValue));
                for (Path file : dir) {
                    l.add(file);
                }
            } catch (IOException e) {
                System.err.println("Directory not found!");
            }
            ObservableList<Path> l2 = FXCollections.observableArrayList();
            l.stream()
                    .sorted((e1, e2) -> {
                        if (Files.isDirectory(e1) != Files.isDirectory(e2)) {
                            return (Files.isDirectory(e1)) ? -1 : 1;
                        }
                        return e1.compareTo(e2);
                    })
                    .forEach(l2::add);
            tableView.setItems(l2);
        });
    }

    @SuppressWarnings("unchecked")
    private void initFileTable() {
        tableView.getColumns().clear();

        /*
          Создание столбцов таблицы и их заполнение
         */

        TableColumn<Path, Path> column1 = new TableColumn<>("File name");
        column1.setCellValueFactory(param -> new ObjectBinding<Path>() {
            @Override
            protected Path computeValue() {
                return param.getValue();
            }
        });
        column1.setCellFactory(param -> {
            TableCellExtension tbce = new TableCellExtension();
            tbce.setIconImgCache(iconImgCache);
            return tbce;
        });
        column1.setPrefWidth(200);
        tableView.getColumns().add(column1);

        TableColumn<Path, String> column3 = new TableColumn<>("Date");
        column3.setCellValueFactory(param -> new StringBinding() {
            @Override
            protected String computeValue() {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                    return sdf.format(new Date(Files.getLastModifiedTime(param.getValue()).toMillis()));
                } catch (IOException e) {
                    e.printStackTrace();
                    return "";
                }
            }
        });
        column3.setPrefWidth(200);
        tableView.getColumns().add(column3);

        TableColumn<Path, String> column2 = new TableColumn<>("File size");
        column2.setCellValueFactory((TableColumn.CellDataFeatures<Path, String> arg0) -> new StringBinding() {
             @Override
             protected String computeValue() {
                 if (Files.isDirectory(arg0.getValue())) return "dir";
                 try {
                     return getFileSizeStr(Files.size(arg0.getValue()));
                 } catch (IOException e) {
                     e.printStackTrace();
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
        column2.setPrefWidth(110);

        column2.setCellFactory(param -> new TableCell<Path, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(item);
                this.setAlignment(Pos.CENTER_RIGHT);
            }
        });

        tableView.getColumns().add(column2);

        tableView.setOnMouseClicked(ev -> {
            if(ev.isControlDown())
                tableView.getSelectionModel().setSelectionMode(
                        SelectionMode.MULTIPLE);
            else
                tableView.getSelectionModel().setSelectionMode(
                        SelectionMode.SINGLE);
            if (ev.getButton() == MouseButton.PRIMARY) {
                if (ev.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                    Path path = (Path) tableView.getSelectionModel().getSelectedItem();
                    if (Files.isDirectory(path)) {
                        setCurPath(path.toString());
                        history.add(path);
                        label_current_folder.setText(valueOf(path));
                    } else {
                        Thread thr = new Thread(()->{
                            try {
                                Desktop.getDesktop().open(path.toFile());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        thr.start();

                    }
                }

            }
            else {

                contextMenu.show(tableView, ev.getScreenX(), ev.getScreenY());
            }
        });

        tableView.setOnKeyReleased(ev -> {
            if (ev.getCode() == KeyCode.RIGHT && !ev.isAltDown()) {
                Path path = (Path) tableView.getSelectionModel().getSelectedItem();
                if (path != null && Files.isDirectory(path)) {
                    setCurPath(path.toString());
                    history.add(path);
                    label_current_folder.setText(valueOf(path));
                    tableView.getSelectionModel().selectFirst();
                    tableView.scrollTo(0);
                }
            } else if (ev.getCode() == KeyCode.LEFT && !ev.isAltDown()) {
                Path oldPath = Paths.get(getCurPath());
                setCurPath(oldPath.getParent().toString());
                history.add(oldPath.getParent());
                tableView.getSelectionModel().select(oldPath);
                tableView.scrollTo(oldPath);
            }
        });
    }

    private void menuCut(){
        if(tableView.getSelectionModel().getSelectedItems().size()>0) {
            ObservableList itemsToCopy = tableView.getSelectionModel().getSelectedItems();
            Path path[] = new Path[itemsToCopy.size()];
            toCopy = new File[itemsToCopy.size()];
            for (int i = 0; i < itemsToCopy.size(); i++) {
                path[i] = (Path) itemsToCopy.get(i);
                if (Files.isDirectory(path[i])) {
                    toCopy[i] = path[i].toFile();
                    toMove = toCopy;
                    directoryMove = true;
                } else {
                    toCopy[i] = path[i].toFile();
                    toMove = toCopy;
                }
            }
        }
    }

    /*
     * Обработчик кнопки copy в popupMenu
     */
    private void menuCopy(){
        if(tableView.getSelectionModel().getSelectedItems().size()>0) {// Проверка выбран ли файл для копирования
            ObservableList itemsToCopy = tableView.getSelectionModel().getSelectedItems();// Записываем все выбранные файлы в таблице
            Path path[] = new Path[itemsToCopy.size()];
            toCopy = new File[itemsToCopy.size()];
            for (int i = 0; i < itemsToCopy.size(); i++) {
                path[i] = (Path) itemsToCopy.get(i);
                if (Files.isDirectory(path[i])) {
                    toCopy[i] = path[i].toFile();
                    directoryMove = true;

                } else {
                    toCopy[i] = path[i].toFile();
                }
            }
        }
       //  = (Path) tableView.getSelectionModel().getSelectedItems();

    }

    /*
     * Обработчик кнопки delete в popupMenu
     */
    private void menuDelete(){
        if(tableView.getSelectionModel().getSelectedItems().size()>0) { //Выбран ли файл для удаления
            ObservableList itemsToDelete = tableView.getSelectionModel().getSelectedItems();
            Path path[] = new Path[itemsToDelete.size()]; //Создание массива путей к файлам для удаления
            for (int i = 0; i < itemsToDelete.size(); i++) {
                if (tableView.getSelectionModel().getSelectedItem() != null) {
                    path[i] = (Path) itemsToDelete.get(i);
                    try {
                        delete(path[i].toFile()); // Вызов функции delete из com.med.Utils для удаления файлов

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                if (toCopy != null)
                    for (int i = 0; i < toCopy.length; i++) {
                        if (tableView.getSelectionModel().getSelectedItem() != null) {
                            Path path = (Path) tableView.getSelectionModel().getSelectedItem();

                            if (Files.isDirectory(path)) {

                                try {
                                    if (!directoryMove) {
                                        String target = path.toString() + "/" + toCopy[i].getName();
                                        path = Paths.get(target);
                                        if (toMove == null)
                                            copyDir(toCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(toCopy[i], path.toFile(), false);
                                            if (i == toCopy.length - 1)
                                                toMove = null;

                                        }
                                    } else {
                                        directoryMove = false;
                                        if (toMove == null)
                                            copyDir(toCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(toCopy[i], path.toFile(), true);
                                            if (i == toCopy.length - 1)
                                                toMove = null;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        } else {
                            String target = getCurPath();
                            if (Files.isDirectory(Paths.get(target))) {
                                try {
                                    if (!directoryMove) {
                                        target = target + "/" + toCopy[i].getName();
                                        Path path = Paths.get(target);
                                        if (toMove == null)
                                            copyDir(toCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(toCopy[i], path.toFile(), true);
                                            if (i == toCopy.length - 1)
                                                toMove = null;
                                        }
                                    } else {
                                        directoryMove = false;
                                        target = target + "/" + toCopy[i].getName();
                                        Path path = Paths.get(target);

                                        if (toMove == null)
                                            copyDir(toCopy[i], path.toFile(), false);
                                        else {
                                            copyDir(toCopy[i], path.toFile(), true);
                                            if (i == toCopy.length - 1)
                                                toMove = null;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
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
            setCurPath(path.toString());
            label_current_folder.setText(valueOf(path));
    }
    /*
     * Обработчик кнопки back
     * Осуществляет переход в предыдущий каталог в истории
     */
    @FXML
    public void back(){
            Path path = history.back();
            setCurPath(path.toString());
            label_current_folder.setText(valueOf(path));
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
            String pathToFile = getCurPath()+folderName;

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
            tableView.setVisible(false);

            Thread thr = new Thread(() -> {
                try {
                    findFile(name, Paths.get(getCurPath()).toFile()); //Вызов функции поиска
                    Thread.sleep(200);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
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


    private void findFile(String name, File file1)throws IOException {

        File[] list = file1.listFiles();
        if (list != null) {
            for (File file2 : list) {
                if (file2.isDirectory()) {
                    try {
                        findFile(name, file2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (file2.getName().contains(name))
                    Platform.runLater(() -> listFind.getItems().add(file2.getParentFile().toString() + "`" + file2.getName()));

            }
        }
    }


    /*
     * Функция для обновления текущего каталога(очень плохо)
     */
    private void updatePath(){

        String current_path = getCurPath();
        Path updatePath = Paths.get(System.getProperty("user.home"));
        setCurPath(updatePath.toString());
        setCurPath(current_path);

    }

    private void disableFindTable(){

        listFind.setVisible(false);
        tableView.setVisible(true);
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

    private boolean createDir(File fileToCreate){
        return fileToCreate.mkdirs();
    }

    /*
     * Функция для установки текущего каталога
     */
    private void setCurPath(String curPath) { this.curPath.set(curPath); }
    /*
     * Функция получения пути текущего каталога
     */
    private String getCurPath() { return curPath.get(); }


}