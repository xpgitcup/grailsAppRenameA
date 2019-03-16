/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grailsapprenamea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author LiXiaopingChuyun
 */
public class GrailsAppRenameA {

    public String orgName;
    public String lowCaseOrgName;
    public String projectName;
    public String lowCaseProjectName;
    public Integer fileItemCount = 0;
    public Integer lineCount = 0;
    public Integer fileNameCount = 0;
    public Integer fileRewriteCount = 0;
    public String[] deleteList;
    public List<File> files4rename = new ArrayList<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        GrailsAppRenameA grailsAppRenameA = new GrailsAppRenameA();

        if (args.length < 2) {
            System.out.print("usage java AppRenameA 原名字 新工程名\n");
            return;
        }

        grailsAppRenameA.processArgs(args);

    }

    public FileFilter filter;

    public GrailsAppRenameA() {
        this.filter = (File pathname) -> {
            String name = pathname.getName();
            boolean file4process = name.endsWith(".xml")
                    || name.endsWith(".iml")
                    || name.endsWith(".gradle")
                    || name.endsWith(".groovy")
                    || name.endsWith(".java");
            return pathname.isDirectory() || file4process;
        };
        this.deleteList = new String[]{"build", "artifacts", ".git"};
    }

    private void processDir(File sdir) {
        File[] fileList = sdir.listFiles(filter);
        if (null != fileList && fileList.length > 0) {
            for (File fileItem : fileList) {
                if (fileItem.isDirectory()) {
                    //System.out.println("文件夹:" + fileItem.getAbsolutePath());
                    processDir(fileItem);
                } else {
                    //System.out.println("文件:" + fileItem.getAbsolutePath());
                    processFileItem(fileItem);
                }
            }
        }
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String item : children) {
                boolean success = deleteDir(new File(dir, item));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    private void processArgs(String[] args) {
        orgName = args[0];
        lowCaseOrgName = orgName.toLowerCase();

        projectName = args[1];
        lowCaseProjectName = projectName.toLowerCase();

        fileItemCount = 0;
        fileNameCount = 0;
        fileRewriteCount = 0;
        lineCount = 0;
        files4rename.clear();

        System.out.format("将工程名称由%s 改成 %s. \n\n", orgName, projectName);

        File sdir = new File(projectName);
        if (sdir.exists()) {
            System.out.format("开始处理%s...\n", projectName);
            if (sdir.isDirectory()) {
                deleteUnusedDir(sdir);
                processDir(sdir);
                processDir4FileName(sdir);

                System.out.format("\n\n 需要改名的文件：\n");
                files4rename.forEach((File item) -> {
                    if (item.isFile()) {
                        System.out.println(item.getName());
                        processFileName(item);
                    }
                });
                files4rename.forEach((File item) -> {
                    if (item.isDirectory()) {
                        System.out.println(item.getName());
                        processFileName(item);
                    }
                });
            } else {
                System.out.format("%s 这不是一个目录！\n", projectName);
            }
        } else {
            System.out.format("%s 目标不存在！\n", projectName);
        }

        System.out.format("共处理%d个文件, 改名：%d个, 替换%d行代码, 重写%d个文件。\n", fileItemCount, fileNameCount, lineCount, fileRewriteCount);
    }

    private void processFileName(File sdir) {
        //System.out.format("先处理文件名：%s\n", sdir.getName());
        String name = sdir.getName();
        if (name.equals(lowCaseOrgName)) {
            System.out.format("%s 需要改名...\n", name);
            String opath = sdir.getAbsolutePath();
            String newName = opath.replace(lowCaseOrgName, lowCaseProjectName);
            System.out.format("%s-->%s\n", opath, newName);
            File dest = new File(newName);
            sdir.renameTo(dest);
            fileNameCount++;
        }

        if (name.contains(orgName)) {
            System.out.format("%s 需要改名...\n", name);
            String opath = sdir.getAbsolutePath();
            String newName = opath.replace(orgName, projectName);
            System.out.format("%s-->%s\n", opath, newName);
            File dest = new File(newName);
            sdir.renameTo(dest);
            fileNameCount++;
        }
    }

    private void processFileItem(File fileItem) {

        System.out.format("开始处理 %s...\n", fileItem.getName());

        FileInputStream fis = null;
        InputStreamReader reader = null;
        BufferedReader br = null;

        try {
            //System.out.format("处理文件：%s\n", fileItem.getName());
            //processFileName(fileItem);
            fis = new FileInputStream(fileItem);
            reader = new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(reader);

            String tmp = "";
            String temp = "";
            boolean need2rewrite = false;

            List<String> lines = new ArrayList<>();

            while ((tmp = br.readLine()) != null) {
                //System.out.println(tmp);
                if (tmp.contains(lowCaseOrgName)) {
                    temp = tmp.replace(lowCaseOrgName, lowCaseProjectName);
                    lines.add(temp);
                    need2rewrite = true;
                    printOut(tmp, temp);
                } else {
                    if (tmp.contains(orgName)) {
                        temp = tmp.replace(orgName, projectName);
                        lines.add(temp);
                        need2rewrite = true;
                        printOut(tmp, temp);
                    } else {
                        lines.add(tmp);
                    }
                }
            }

            if (need2rewrite) {
                fileRewriteCount++;

                String bakFileName = fileItem.getAbsolutePath() + ".bak";
                String newFileName = fileItem.getAbsolutePath();

                File bakFile = new File(bakFileName);
                fileItem.renameTo(bakFile);

                File newFile = new File(newFileName);
                FileOutputStream fos = new FileOutputStream(newFile);
                PrintStream ps = new PrintStream(fos);

                lines.forEach((string) -> {
                    ps.println(string);
                });

                fos.close();
            }

            fileItemCount++;
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(GrailsAppRenameA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GrailsAppRenameA.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(GrailsAppRenameA.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void printOut(String tmp, String temp) {
        System.out.format("%s\n -->\n%s \n\n", tmp, temp);
        lineCount++;
    }

    private void deleteUnusedDir(File sdir) {
        File[] files = sdir.listFiles();
        for (File item : files) {
            if (item.isDirectory()) {
                String name = item.getName();
                //System.out.format("%s\n", name);
                if (Arrays.asList(deleteList).contains(name)) {
                    System.out.format("删除%s！\n", item.getName());
                    deleteDir(item);
                } else {
                    deleteUnusedDir(item);
                }
            }
        }
    }

    private void processDir4FileName(File sdir) {
        File[] fileList = sdir.listFiles(filter);
        if (null != fileList && fileList.length > 0) {
            for (File fileItem : fileList) {
                if (fileItem.isDirectory()) {
                    if (fileItem.getName().equals(lowCaseOrgName) || fileItem.getName().contains(orgName)) {
                        files4rename.add(fileItem);
                    }
                    processDir4FileName(fileItem);
                } else {
                    if (fileItem.getName().equals(lowCaseOrgName) || fileItem.getName().contains(orgName)) {
                        files4rename.add(fileItem);
                    }
                }
            }
        }
    }

}
