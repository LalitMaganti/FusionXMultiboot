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
import android.app.Activity;
import android.widget.EditText;
import android.widget.Toast;

public class MakeMultiBoot extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_multi_boot);
        RunRootCommandsAsync h = new RunRootCommandsAsync();
        h.execute(getIntent().getExtras());
    }

    @Override
    public void onBackPressed() {
    }

    public void DisplayToast(String paramString) {
        Toast.makeText(this, paramString, Toast.LENGTH_SHORT).show();
    }

    public class RunRootCommandsAsync extends AsyncTask<Bundle, String, Void> {
        String tempSdCardDir = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/tempMultiboot/";
        String finalOutdir = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/multiboot/";
        final static String dataDir = "/data/data/com.fusionx.tilal6991.dualboot/files/";

        String inputFile;
        String romExtractionDir;
        String romName;
        String systemImageName;
        String dataImageName;

        Bundle b;

        @Override
        protected Void doInBackground(Bundle... params) {
            b = params[0];
            inputFile = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + b.getString("filename");
            romName = b.getString("filename").replace(".zip", "");
            romExtractionDir = tempSdCardDir + romName + "/";

            new File(romExtractionDir).mkdirs();
            new File(finalOutdir).mkdirs();
            new File(dataDir + "boot.img-ramdisk").mkdirs();

            publishProgress("Making directories");

            dataImageName = b.getString("dataimagename");
            systemImageName = b.getString("systemimagename");

            makeSystemImage();
            makeDataImage();
            extractRom();
            remakeBootImage();
            fixUpdaterScript();
            packUpAndFinish();
            cleanup();
            return null;
        }

        private void packUpAndFinish() {
            publishProgress("Making ROM zip");
            runRootCommand(dataDir + "zip -r -q " + finalOutdir + " loop-roms/"
                    + romName + "-loopinstall.zip" + romExtractionDir + "*");
            publishProgress("Creating copy of loop boot image for flashing");
            runRootCommand("cp " + romExtractionDir + "boot.img " + finalOutdir
                    + romName + "boot.img");
            String m = "#!/system/bin/sh\n"
                    + "flash_image boot /sdcard/multiboot/" + romName
                    + "boot.img\n" + "reboot";
            publishProgress("Creating loop script file");
            try {
                FileWriter l = new FileWriter(finalOutdir + "boot" + romName
                        + ".sh");
                l.write(m);
                l.close();
            } catch (IOException e) {
            }

            publishProgress("Creating nand boot image");
            runRootCommand("dd if=/dev/mtd/mtd1 of=/sdcard/multiboot/boot.img bs=4096");
            m = "#!/system/bin/sh\n"
                    + "flash_image boot /sdcard/multiboot/boot.img\n"
                    + "reboot";
            publishProgress("Creating nand script file");
            try {
                FileWriter l = new FileWriter(finalOutdir + "boot" + romName
                        + ".sh");
                l.write(m);
                l.close();
            } catch (IOException e) {
            }
        }

        private void fixUpdaterScript() {
            File file = new File(romExtractionDir
                    + "META-INF/com/google/android/updater-script");
            publishProgress("Finding updater script format");
            try {
                String k = null;
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    String nextline = scanner.nextLine();
                    if (nextline
                            .contains("format(\"yaffs2\", \"MTD\", \"system\", \"0\", \"/system\")")) {
                        k = "/format(\"yaffs2\", \"MTD\", \"system\", \"0\", \"\\/system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mkdir\", \"-p\", \"\\/sdcard\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\", \"0\", \"\\/system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"vfat\", \"\\/dev\\/block\\/mmcblk0p1\", \"\\/sdcard\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\", \"0\", \"\\/system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop0\", \"\\/sdcard\\/multiboot\\/"
                                + systemImageName
                                + "\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\", \"0\", \"\\/system\");/i\\\"\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop1\", \"\\/sdcard\\/multiboot\\/"
                                + dataImageName
                                + "\");\n\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\", \"0\", \"\\/system\");/c\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop1\", \"\\/sdcard\\/multiboot\\/"
                                + dataImageName
                                + "\");\n\n"
                                +

                                "/mount(\"yaffs2\", \"MTD\", \"system\", \"\\/system\");/c\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"ext2\", \"\\/dev\\/block\\/loop0\", \"\\/system\");\n\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop0\");\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop1\");\n";
                        break;
                    } else if (nextline
                            .contains("format(\"yaffs2\", \"MTD\", \"system\", \"0\")")) {
                        k = "/format(\"yaffs2\", \"MTD\", \"system\", \"0\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mkdir\", \"-p\", \"\\/sdcard\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\", \"0\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"vfat\", \"\\/dev\\/block\\/mmcblk0p1\", \"\\/sdcard\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\", \"0\");/i\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop0\", \"\\/sdcard\\/multiboot\\/"
                                + systemImageName
                                + "\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\", \"0\");/i\\\"\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop1\", \"\\/sdcard\\/multiboot\\/"
                                + dataImageName
                                + "\");\n\n"
                                +

                                "/format(\"yaffs2\", \"MTD\", \"system\", \"0\");/c\\\n"
                                + "run_program(\"\\/sbin\\/mke2fs\", \"-T\", \"ext2\", \"\\/dev\\/block\\/loop0\");\n\n"
                                +

                                "/mount(\"yaffs2\", \"MTD\", \"system\", \"\\/system\");/c\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"ext2\", \"\\/dev\\/block\\/loop0\", \"\\/system\");\n\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop0\");\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop1\");\n";
                        break;
                    } else if (nextline
                            .contains("format(\"yaffs2\", \"MTD\", \"system\"")) {
                        k = "/format(\"yaffs2\", \"MTD\", \"system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mkdir\", \"-p\", \"\\/sdcard\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"vfat\", \"\\/dev\\/block\\/mmcblk0p1\", \"\\/sdcard\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop0\", \"\\/sdcard\\/multiboot\\/"
                                + systemImageName
                                + "\");\n"
                                + "/format(\"yaffs2\", \"MTD\", \"system\");/i\\\"\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop1\", \"\\/sdcard\\/multiboot\\/"
                                + dataImageName
                                + "\");\n\n"
                                +

                                "/format(\"yaffs2\", \"MTD\", \"system\");/c\\\n"
                                + "run_program(\"\\/sbin\\/mke2fs\", \"-T\", \"ext2\", \"\\/dev\\/block\\/loop0\");\n\n"
                                +

                                "/mount(\"yaffs2\", \"MTD\", \"system\", \"\\/system\");/c\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"ext2\", \"\\/dev\\/block\\/loop0\", \"\\/system\");\n\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop0\");\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop1\");\n";
                        break;
                    } else if (nextline.contains("format(\"MTD\", \"system\"")) {
                        k = "/format(\"MTD\", \"system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mkdir\", \"-p\", \"\\/sdcard\");\n"
                                + "/format(\"MTD\", \"system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"vfat\", \"\\/dev\\/block\\/mmcblk0p1\", \"\\/sdcard\");\n"
                                + "/format(\"MTD\", \"system\");/i\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop0\", \"\\/sdcard\\/multiboot\\/"
                                + systemImageName
                                + "\");\n"
                                + "/format(\"MTD\", \"system\");/i\\\"\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"\\/dev\\/block\\/loop1\", \"\\/sdcard\\/multiboot\\/"
                                + dataImageName
                                + "\");\n\n"
                                +

                                "/format(\"MTD\", \"system\");/c\\\n"
                                + "run_program(\"\\/sbin\\/mke2fs\", \"-T\", \"ext2\", \"\\/dev\\/block\\/loop0\");\n\n"
                                +

                                "/mount(\"yaffs2\", \"MTD\", \"system\", \"\\/system\");/c\\\n"
                                + "run_program(\"\\/sbin\\/mount\", \"-t\", \"ext2\", \"\\/dev\\/block\\/loop0\", \"\\/system\");\n\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop0\");\n"
                                +

                                "/unmount(\"\\/system\");/a\\\n"
                                + "run_program(\"\\/sbin\\/losetup\", \"-d\", \"\\/dev\\/block\\/loop1\");\n";
                        break;
                    }
                }
                publishProgress("Creating find and replace file");
                FileWriter l = new FileWriter(dataDir + "upd.sed");
                l.write(k);
                l.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e1) {
            }
            publishProgress("Editing updater script");
            runRootCommand("sed -f " + dataDir + "upd.sed < "
                    + romExtractionDir
                    + "META-INF/com/google/android/updater-script > "
                    + romExtractionDir
                    + "META-INF/com/google/android/updater-script.fix");
            deleteIfExists(dataDir + "upd.sed");

            publishProgress("Replacing edited updater script");
            runRootCommand("mv " + romExtractionDir
                    + "META-INF/com/google/android/updater-script.fix "
                    + romExtractionDir
                    + "META-INF/com/google/android/updater-script");
        }

        private void remakeBootImage() {
            final String jellybeanReplace = "/on fs/a\\\n"
                    + "mkdir -p \\/storage\\/sdcard0\n\n"
                    + "/on fs/a\\\n"
                    + "mount vfat \\/dev\\/block\\/mmcblk0p1 \\/storage\\/sdcard0\n\n"
                    + "/mount yaffs2 mtd@system \\/system ro remount/c\\\n"
                    + "mount ext2 loop@\\/storage\\/sdcard0\\/multiboot\\/"
                    + systemImageName + " \\/system ro remount\n\n"
                    + "/mount yaffs2 mtd@system \\/system/c\\n"
                    + "mount ext2 loop@\\/storage\\/sdcard0\\/multiboot\\/"
                    + systemImageName + " \\/system\n\n"
                    + "/mount yaffs2 mtd@userdata \\/data nosuid nodev/c\\\n"
                    + "mount ext2 loop@\\/storage\\/sdcard0\\/multiboot\\/"
                    + dataImageName + " \\/data nosuid nodev\n";

            final String otherReplace = "/on fs/a\\\n"
                    + "mkdir -p \\/mnt\\/sdcard\n\n"
                    + "/on fs/a\\\n"
                    + "mount vfat \\/dev\\/block\\/mmcblk0p1 \\/mnt\\/sdcard\n\n"
                    + "/mount yaffs2 mtd@system \\/system ro remount/c\\\n"
                    + "mount ext2 loop@\\/mnt\\/sdcard\\/multiboot\\/"
                    + systemImageName + " \\/system ro remount\n\n"
                    + "/mount yaffs2 mtd@system \\/system/c\\n"
                    + "mount ext2 loop@\\/mnt\\/sdcard\\/multiboot\\/"
                    + systemImageName + " \\/system\n\n"
                    + "/mount yaffs2 mtd@userdata \\/data nosuid nodev/c\\\n"
                    + "mount ext2 loop@\\/mnt\\/sdcard\\/multiboot\\/"
                    + dataImageName + " \\/data nosuid nodev\n";

            publishProgress("Moving boot image");
            runRootCommand("mv " + romExtractionDir + "boot.img " + dataDir
                    + "boot.img");

            publishProgress("Getting info about boot.img");
            String base = "0x"
                    + runRootCommand(
                            "od -A n -h -j 34 -N 2 " + dataDir
                                    + "boot.img|sed 's/ //g'").trim() + "0000";
            String commandLine = "\""
                    + runRootCommand(
                            "od -A n --strings -j 64 -N 512 " + dataDir
                                    + "boot.img").trim() + "\"";

            publishProgress("Extracting kernel");
            runRootCommands(new String[] { "cd " + dataDir,
                    "./extract-kernel.pl " + dataDir + "boot.img" });
            runRootCommand("mv " + dataDir + "boot.img-kernel " + dataDir
                    + "zImage");

            publishProgress("Extracting ramdisk");
            runRootCommands(new String[] { "cd " + dataDir,
                    "./extract-ramdisk.pl " + dataDir + "boot.img" });
            runRootCommands(new String[] {
                    "cd " + dataDir + "boot.img-ramdisk",
                    "gunzip -c ../boot.img-ramdisk.cpio.gz | cpio -i" });

            deleteIfExists(dataDir + "boot.img");

            publishProgress("Making find and replace file");
            try {
                FileWriter k = new FileWriter(dataDir + "init.sed");
                if (Build.VERSION.SDK_INT >= 16)
                    k.write(jellybeanReplace);
                else
                    k.write(otherReplace);
                k.close();
            } catch (IOException e1) {
            }

            publishProgress("Editing init.rc for mounting loop systems");
            runRootCommand("sed -f " + dataDir + "init.sed < " + dataDir
                    + "boot.img-ramdisk/init.rc > " + dataDir
                    + "boot.img-ramdisk/init.rc.fix");
            runRootCommand("mv " + dataDir + "boot.img-ramdisk/init.rc.fix "
                    + dataDir + "boot.img-ramdisk/init.rc");

            publishProgress("Making compressed ramdisk");
            runRootCommand(dataDir + "mkbootfs " + dataDir
                    + "boot.img-ramdisk | gzip > " + dataDir + "ramdisk.gz");

            publishProgress("Making edited boot image");
            runRootCommand(dataDir + "mkbootimg --cmdline " + commandLine
                    + " --base " + base + " --kernel " + dataDir
                    + "zImage --ramdisk " + dataDir + "ramdisk.gz -o "
                    + romExtractionDir + "boot.img");
        }

        private void extractRom() {
            publishProgress("Extracting ROM");
            runRootCommand("unzip -q " + inputFile + " -d " + romExtractionDir);
        }

        private void cleanup() {
            publishProgress("Cleaning up");
            deleteIfExists(tempSdCardDir);
            deleteIfExists(dataDir);
            publishProgress("Finished!");
        }

        private void makeDataImage() {
            String dataoutput = finalOutdir + dataImageName;
            int datasize = Integer.parseInt(b.getString("dataimagesize")) * 1024;
            publishProgress("Making data image");
            String losetupLocation = runRootCommand("losetup -f").trim();
            runRootCommand("dd if=/dev/zero of=" + dataoutput
                    + " bs=1024 count=" + datasize);
            runRootCommand("losetup " + losetupLocation + " " + dataoutput);
            runRootCommand("mke2fs -t ext2 " + losetupLocation);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            runRootCommand("losetup -d " + losetupLocation);
        }

        private void makeSystemImage() {
            String systemoutput = finalOutdir + systemImageName;
            int systemsize = Integer.parseInt(b.getString("systemimagesize")) * 1024;
            publishProgress("Making system image");
            String losetupLocation = runRootCommand("losetup -f").trim();
            runRootCommand("dd if=/dev/zero of=" + systemoutput
                    + " bs=1024 count=" + systemsize);
            runRootCommand("losetup " + losetupLocation + " " + systemoutput);
            runRootCommand("mke2fs -t ext2 " + losetupLocation);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            runRootCommand("losetup -d " + losetupLocation);
        }

        private String runRootCommand(String cmd) {
            Process p = null;
            StringBuilder sb = new StringBuilder();
            try {
                p = Runtime.getRuntime().exec("su");
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        p.getInputStream()));
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
            try {
                p = Runtime.getRuntime().exec("su");
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        p.getInputStream()));
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
                runRootCommand("rm -rf " + fileName);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            WriteOutput(values[0]);
        }

        public void WriteOutput(String paramString) {
            EditText k = (EditText) findViewById(R.id.editText1);
            k.append(paramString);
        }
    }
}