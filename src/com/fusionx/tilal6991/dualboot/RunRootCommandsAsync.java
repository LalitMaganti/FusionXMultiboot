package com.fusionx.tilal6991.dualboot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class RunRootCommandsAsync extends AsyncTask<Bundle, Void, Void> {
    final static String tempSdCardDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tempMultiboot/";
    final static String finalOutdir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/multiboot/";
    
    Bundle b;
    String inputFile;
    String romExtractionDir;
    String romName;
    String dataoutput;
    String systemoutput;
    final static String dataDir = "/data/data/com.fusionx.tilal6991.dualboot/files/";
    
    @Override
    protected Void doInBackground(Bundle... params) {
        b = params[0];
        inputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + b.getString("filename");
        romName = b.getString("filename").replace(".zip", "");
        romExtractionDir = tempSdCardDir + romName;
        
        new File(romExtractionDir).mkdirs();

        makeSystemImage();
        makeDataImage();
        extractRom();
        remakeBootImage();
        fixUpdaterScript();
        getRidOfBadFiles();
        return null;
    }
    
    private void fixUpdaterScript() {
        File file = new File("Student.txt");
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                if(scanner.nextLine().contains("format(\"yaffs2\", \"MTD\", \"system\", \"0\", \"/system\")")) { 
                    break;
                }
            }
        } catch (FileNotFoundException e) {
        }
    }

    private void remakeBootImage() {
        final String jellybeanReplace = "/on fs/a\\\n" + 
        "mkdir -p \\/storage\\/sdcard0\n\n" +
        "/on fs/a\\\n" +
        "mount vfat \\/dev\\/block\\/mmcblk0p1 \\/storage\\/sdcard0\n\n" +
        "/mount yaffs2 mtd@system \\/system ro remount/c\\\n" + 
        "mount ext2 loop@\\/storage\\/sdcard0\\/multiboot\\/" + systemoutput + " \\/system ro remount\n\n" + 
        "/mount yaffs2 mtd@system \\/system/c\\n" + 
        "mount ext2 loop@\\/storage\\/sdcard0\\/multiboot\\/" + systemoutput + " \\/system\n\n" +
        "/mount yaffs2 mtd@userdata \\/data nosuid nodev/c\\\n" +
        "mount ext2 loop@\\/storage\\/sdcard0\\/multiboot\\/" + dataoutput + " \\/data nosuid nodev\n";
        
        final String otherReplace = "/on fs/a\\\n" + 
                "mkdir -p \\/mnt\\/sdcard\n\n" +
                "/on fs/a\\\n" +
                "mount vfat \\/dev\\/block\\/mmcblk0p1 \\/mnt\\/sdcard\n\n" +
                "/mount yaffs2 mtd@system \\/system ro remount/c\\\n" + 
                "mount ext2 loop@\\/mnt\\/sdcard\\/multiboot\\/" + systemoutput + " \\/system ro remount\n\n" + 
                "/mount yaffs2 mtd@system \\/system/c\\n" + 
                "mount ext2 loop@\\/mnt\\/sdcard\\/multiboot\\/" + systemoutput + " \\/system\n\n" +
                "/mount yaffs2 mtd@userdata \\/data nosuid nodev/c\\\n" +
                "mount ext2 loop@\\/mnt\\/sdcard\\/multiboot\\/" + dataoutput + " \\/data nosuid nodev\n";
        
        runRootCommand("cp " + romExtractionDir + "/boot.img " + dataDir + "boot.img");
        String base = "0x" + runRootCommand("od -A n -h -j 34 -N 2 " + dataDir + "boot.img|sed 's/ //g'").trim() + "0000";
        String commandLine = runRootCommand("od -A n --strings -j 64 -N 512 " + dataDir + "boot.img").trim();

        runRootCommand(dataDir + "extract-kernel " + dataDir + "boot.img");
        runRootCommand("mv " + dataDir + "boot.img-kernel "  + dataDir + "zImage");
        
        runRootCommand(dataDir + "extract-ramdisk " + dataDir + "boot.img");
        runRootCommands(new String[]{"cd " + dataDir + "ramdisk-contents", "gunzip -c ../boot.img-ramdisk.gz | cpio -i"});
        
        deleteIfExists(dataDir + "boot.img");
        deleteIfExists(romExtractionDir + "boot.img");
        
        try {
            FileWriter k = new FileWriter(dataDir + "init.sed");
            if (Build.VERSION.SDK_INT >= 16)
                k.write(jellybeanReplace);
            else
                k.write(otherReplace);
            k.close();
        } catch (IOException e1) {
        }

        runRootCommand("sed -f " + dataDir + "init.sed < " + dataDir + "ramdisk-contents/init.rc > " + dataDir + "ramdisk-contents/initrc.fix");
        runRootCommand("mv " + dataDir + "ramdisk-contents/init.rc.fix " + dataDir + "ramdisk-contents/init.rc");
        
        deleteIfExists(dataDir + "init.sed");
        
        runRootCommand(dataDir + "mkbootfs " + dataDir + "ramdisk-contents | gzip > " + dataDir + "ramdisk.gz");
        runRootCommand(dataDir + "mkbootimg --cmdline " + commandLine + "--base " + base + " --kernel " + dataDir + "zImage --ramdisk " + dataDir + "ramdisk.gz -o " + tempSdCardDir + "boot.img");
        deleteIfExists(dataDir + "zImage");
        deleteIfExists(dataDir + "ramdisk.gz");
        deleteIfExists(dataDir + "ramdisk-contents");
     }
    
    private void extractRom() {
      new File(romExtractionDir).mkdir();
      new File(finalOutdir).mkdir();
      runRootCommand("unzip -q " + inputFile + " -d " + romExtractionDir);
    }
    
    private void getRidOfBadFiles() {
        
    }
    
    private void makeDataImage() {
        dataoutput = tempSdCardDir + b.getString("dataimagename");
        int datasize =  Integer.parseInt(b.getString("dataimagesize")) * 1024;
        String losetupLocation = runRootCommand("losetup -f").trim();
        runRootCommand("dd if=/dev/zero of=" + dataoutput + " bs=1024 count=" + datasize);
        runRootCommand("losetup " + losetupLocation + " " + dataoutput);
        runRootCommand("mke2fs -t ext2 " + losetupLocation);
        try {Thread.sleep(10000);} catch (InterruptedException e) {}
        runRootCommand("losetup -d " + losetupLocation);
    }
    
    private void makeSystemImage() {
        systemoutput = tempSdCardDir + b.getString("systemimagename");
        int systemsize =  Integer.parseInt(b.getString("systemimagesize")) * 1024;
        String losetupLocation = runRootCommand("losetup -f").trim();
        runRootCommand("dd if=/dev/zero of=" + systemoutput + " bs=1024 count=" + systemsize);
        runRootCommand("losetup " + losetupLocation + " " + systemoutput);
        runRootCommand("mke2fs -t ext2 " + losetupLocation);
        try {Thread.sleep(10000);} catch (InterruptedException e) {}
        runRootCommand("losetup -d " + losetupLocation);
    }
    
    private String runRootCommand(String cmd) {
        Process p = null;
        StringBuilder sb = new StringBuilder();
        Log.d("Multiboot", cmd);
        try {
            p = Runtime.getRuntime().exec("su");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            String read = br.readLine();
            while (read != null) {
                sb.append(read + '\n');
                read = br.readLine();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    private String runRootCommands(String[] cmd) {
        Process p = null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cmd.length; i++)
            Log.d("Multiboot", cmd[1]);
        try {
            p = Runtime.getRuntime().exec("su");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (int i = 0; i < cmd.length; i++)
                os.writeBytes(cmd[i] + "\n");
            os.writeBytes("exit\n");
            os.flush();
            String read = br.readLine();
            while (read != null) {
                sb.append(read + '\n');
                read = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    public void deleteIfExists(String fileName) {
        File file = new File(fileName);
        if (file.exists())
            file.delete();
    }
}