package com.fusionx.tilal6991.dualboot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class CreateMultiBootRom extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_multi_boot);
        CreateMultibootRomAsync instance = new CreateMultibootRomAsync();
        instance.execute(getIntent().getExtras());
    }

    @Override
    public void onBackPressed() {
    }

    public class CreateMultibootRomAsync extends
            AsyncTask<Bundle, String, Void> {
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

        Bundle bundle;

        @Override
        protected Void doInBackground(Bundle... params) {
            bundle = params[0];
            inputFile = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + bundle.getString("filename");
            romName = bundle.getString("filename").replace(".zip", "");
            romExtractionDir = tempSdCardDir + romName + "/";

            publishProgress("Making directories");
            new File(romExtractionDir).mkdirs();
            new File(finalOutdir + "loop-roms").mkdirs();

            dataImageName = bundle.getString("dataimagename");
            systemImageName = bundle.getString("systemimagename");

            boolean data = bundle.getBoolean("createdataimage");
            boolean system = bundle.getBoolean("createsystemimage");

            preClean();
            if (system)
                makeSystemImage();
            if (data)
                makeDataImage();
            extractRom();
            remakeBootImage();
            fixUpdaterScript();
            packUpAndFinish();
            cleanup();
            return null;
        }

        private Handler mHandler = new Handler();

        private Runnable mUpdateTimeTask = new Runnable() {
            public void run() {
                Intent intent = new Intent(getApplicationContext(),
                        MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        };

        private void preClean() {
            publishProgress("Running a preclean");
            CommonFunctions.deleteIfExists(finalOutdir + romName + "boot.img");
            CommonFunctions.deleteIfExists(finalOutdir + "boot" + romName
                    + ".sh");
            CommonFunctions.deleteIfExists(finalOutdir + "boot.sh");
            CommonFunctions.deleteIfExists(finalOutdir + "boot.img");
            CommonFunctions.deleteIfExists(finalOutdir + "loop-roms/" + romName
                    + "-loopinstall.zip");
        }

        private void packUpAndFinish() {
            publishProgress("Making ROM zip");
            CommonFunctions.runRootCommands(new String[] {
                    "cd " + romExtractionDir,
                    dataDir + "zip -r -q " + finalOutdir + "loop-roms/"
                            + romName + "-loopinstall.zip " + "*" });

            publishProgress("Creating copy of loop boot image for flashing");
            CommonFunctions.runRootCommand("cp " + romExtractionDir
                    + "boot.img " + finalOutdir + romName + "boot.img");

            FileWriter fileWriter;
            String shFile = "#!/system/bin/sh\n"
                    + "flash_image boot /sdcard/multiboot/" + romName
                    + "boot.img\n" + "reboot";

            publishProgress("Creating loop script file");
            try {
                fileWriter = new FileWriter(finalOutdir + "boot" + romName
                        + ".sh");
                fileWriter.write(shFile);
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            publishProgress("Creating nand boot image");
            CommonFunctions
                    .runRootCommand("dd if=/dev/mtd/mtd1 of=/sdcard/multiboot/boot.img bs=4096");
            shFile = "#!/system/bin/sh\n"
                    + "flash_image boot /sdcard/multiboot/boot.img\n"
                    + "reboot";

            publishProgress("Creating nand script file");
            try {
                fileWriter = new FileWriter(finalOutdir + "boot.sh");
                fileWriter.write(shFile);
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void fixUpdaterScript() {
            String updaterScript = romExtractionDir
                    + "META-INF/com/google/android/updater-script";
            String findString = null;
            try {
                Scanner scanner = new Scanner(new File(updaterScript));
                while (scanner.hasNextLine()) {
                    findString = scanner.nextLine();
                    if (findString.contains("format(")
                            && (findString.contains("\"MTD\", \"system\""))) {
                        break;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            publishProgress("Editing updater script");
            findAndReplaceInFile(updaterScript, findString, "");
            findAndReplaceInFile(
                    updaterScript,
                    "mount(\"yaffs2\", \"MTD\", \"system\", \"/system\");",
                    "run_program(\"/sbin/losetup\", \"/dev/block/loop0\", \"/sdcard/multiboot/"
                            + systemImageName
                            + "\");\n"
                            + "run_program(\"/sbin/mke2fs\", \"-T\", \"ext2\", \"/dev/block/loop0\");\n"
                            + "run_program(\"/sbin/mount\", \"-t\", \"ext2\", \"/dev/block/loop0\", \"/system\");");
            findAndReplaceInFile(
                    updaterScript,
                    "unmount(\"/system\");",
                    "unmount(\"/system\");\n"
                            + "run_program(\"/sbin/losetup\", \"-d\", \"/dev/block/loop0\");");

        }

        private void findAndReplaceInFile(String fileName, String findString,
                String replaceString) {
            try {
                Scanner scanner = new Scanner(new File(fileName));
                FileWriter s = new FileWriter(new File(fileName + ".fix"));
                while (scanner.hasNextLine()) {
                    String nextLine = scanner.nextLine();
                    if (nextLine.contains(findString))
                        s.write(replaceString + "\n");
                    else
                        s.write(nextLine + "\n");
                }
                s.close();
                CommonFunctions.runRootCommand("mv " + fileName + ".fix "
                        + fileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void remakeBootImage() {
            publishProgress("Moving boot image");
            CommonFunctions.runRootCommand("cp " + romExtractionDir
                    + "boot.img " + dataDir + "boot.img");
            CommonFunctions.deleteIfExists(romExtractionDir + "boot.img");

            publishProgress("Getting boot.img parameters");
            String base = "0x"
                    + CommonFunctions.runRootCommand(
                            dataDir + "busybox od -A n -h -j 34 -N 2 "
                                    + dataDir + "boot.img").trim() + "0000";
            String commandLine = "'"
                    + CommonFunctions.runRootCommand(
                            dataDir + "busybox od -A n --strings -j 64 -N 512 "
                                    + dataDir + "boot.img").trim() + "'";

            publishProgress("Extracting kernel");
            CommonFunctions.runRootCommands(new String[] { "cd " + dataDir,
                    "./extract-kernel.pl " + dataDir + "boot.img" });

            publishProgress("Extracting ramdisk");
            CommonFunctions.runRootCommands(new String[] { "cd " + dataDir,
                    "./extract-ramdisk.pl " + dataDir + "boot.img" });
            CommonFunctions.runRootCommands(new String[] {
                    "cd " + dataDir + "boot.img-ramdisk",
                    "gunzip -c ../boot.img-ramdisk.cpio.gz | cpio -i" });

            CommonFunctions.deleteIfExists(dataDir + "boot.img");

            CommonFunctions.runRootCommand("cp " + dataDir
                    + "boot.img-ramdisk/init.rc " + tempSdCardDir + "init.rc");
            CommonFunctions
                    .deleteIfExists(dataDir + "boot.img-ramdisk/init.rc");

            publishProgress("Editing init.rc");

            findAndReplaceInFile(tempSdCardDir + "init.rc", "on fs", "on fs\n"
                    + "    mkdir -p /storage/sdcard0\n"
                    + "    mount vfat /dev/block/mmcblk0p1 /storage/sdcard0");

            findAndReplaceInFile(tempSdCardDir + "init.rc",
                    "mount yaffs2 mtd@system /system ro remount",
                    "    mount ext2 loop@/storage/sdcard0/multiboot/"
                            + systemImageName + " /system ro remount");

            findAndReplaceInFile(tempSdCardDir + "init.rc",
                    "mount yaffs2 mtd@system /system",
                    "    mount ext2 loop@/storage/sdcard0/multiboot/"
                            + systemImageName + " /system");

            findAndReplaceInFile(tempSdCardDir + "init.rc",
                    "mount yaffs2 mtd@userdata /data nosuid nodev",
                    "    mount ext2 loop@/storage/sdcard0/multiboot/"
                            + dataImageName + " /data nosuid nodev");

            CommonFunctions.runRootCommand("cp " + tempSdCardDir + "init.rc "
                    + dataDir + "boot.img-ramdisk/init.rc");
            CommonFunctions.deleteIfExists(tempSdCardDir + "init.rc");

            publishProgress("Making compressed ramdisk");
            CommonFunctions.runRootCommands(new String[] { "cd " + dataDir,
                    "./mkbootfs boot.img-ramdisk | gzip > ramdisk.gz" });

            publishProgress("Making boot image");
            CommonFunctions
                    .runRootCommands(new String[] {
                            "cd " + dataDir,
                            "./mkbootimg --cmdline "
                                    + commandLine
                                    + " --base "
                                    + base
                                    + " --kernel boot.img-kernel --ramdisk ramdisk.gz -o "
                                    + romExtractionDir + "boot.img" });
        }

        private void extractRom() {
            publishProgress("Extracting ROM");
            CommonFunctions.runRootCommand(dataDir + "busybox unzip -q "
                    + inputFile + " -d " + romExtractionDir);
        }

        private void cleanup() {
            publishProgress("Cleaning up");
            CommonFunctions.deleteIfExists(tempSdCardDir);
            CommonFunctions.deleteIfExists(dataDir);
            publishProgress("Finished!");
            publishProgress("gotomain");
        }

        private void makeDataImage() {
            String dataoutput = finalOutdir + dataImageName;
            int datasize = Integer.parseInt(bundle.getString("dataimagesize")) * 1024;
            publishProgress("Making data image");
            String losetupLocation = CommonFunctions.runRootCommand(
                    "losetup -f").trim();
            CommonFunctions.runRootCommand("dd if=/dev/zero of=" + dataoutput
                    + " bs=1024 count=" + datasize);
            CommonFunctions.runRootCommand("losetup " + losetupLocation + " "
                    + dataoutput);
            CommonFunctions.runRootCommand("mke2fs -t ext2 " + losetupLocation);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            CommonFunctions.runRootCommand("losetup -d " + losetupLocation);
        }

        private void makeSystemImage() {
            String systemoutput = finalOutdir + systemImageName;
            int systemsize = Integer.parseInt(bundle
                    .getString("systemimagesize")) * 1024;
            publishProgress("Making system image");
            String losetupLocation = CommonFunctions.runRootCommand(
                    "losetup -f").trim();
            CommonFunctions.runRootCommand("dd if=/dev/zero of=" + systemoutput
                    + " bs=1024 count=" + systemsize);
            CommonFunctions.runRootCommand("losetup " + losetupLocation + " "
                    + systemoutput);
            CommonFunctions.runRootCommand("mke2fs -t ext2 " + losetupLocation);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            CommonFunctions.runRootCommand("losetup -d " + losetupLocation);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (values[0] == "gotomain")
                mHandler.postDelayed(mUpdateTimeTask, 5000);
            else
                WriteOutput(values[0]);
        }

        public void WriteOutput(String paramString) {
            TextView editText = (TextView) findViewById(R.id.editText1);
            editText.append(paramString + "\n");
            editText.setMovementMethod(new ScrollingMovementMethod());
        }
    }
}