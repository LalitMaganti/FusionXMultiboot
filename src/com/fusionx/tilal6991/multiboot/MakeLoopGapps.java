package com.fusionx.tilal6991.multiboot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class MakeLoopGapps extends Activity {

    public class CreateMultibootGappsAsync extends
            AsyncTask<Bundle, String, Void> {
        final static String dataDir = "/data/data/com.fusionx.tilal6991.multiboot/files/";
        Bundle bundle;
        final String externalPath = Environment.getExternalStorageDirectory()
                .getAbsolutePath();
        final String finalOutdir = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/multiboot/";

        String inputFile;
        private final Runnable mFinished = new Runnable() {
            public void run() {
                final Intent intent = new Intent(getApplicationContext(),
                        MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        };
        String romExtractionDir;
        String romName;

        String systemImageName;

        final String tempSdCardDir = externalPath + "/tempMultiboot/";

        private void cleanup() {
            publishProgress("Cleaning up");
            CommonFunctions.deleteIfExists(tempSdCardDir);
            CommonFunctions.deleteIfExists(dataDir);
            publishProgress("Finished!");
        }

        @Override
        protected Void doInBackground(final Bundle... params) {
            bundle = params[0];
            inputFile = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + bundle.getString("filename");
            romName = bundle.getString("filename").replace(".zip", "");
            romExtractionDir = tempSdCardDir + romName + "/";

            preClean();

            publishProgress("Making directories");
            new File(romExtractionDir).mkdirs();
            new File(finalOutdir + "loop-roms").mkdirs();

            publishProgress("Getting data from wizard");
            systemImageName = bundle.getString("systemimagename");

            extractRom();
            fixUpdaterScript();
            packUpAndFinish();
            cleanup();
            return null;
        }

        private void extractRom() {
            publishProgress("Extracting ROM");
            CommonFunctions.runRootCommand(dataDir + "busybox unzip -q "
                    + inputFile + " -d " + romExtractionDir);
        }

        private void findAndReplaceInFile(final String fileName,
                final String findString, final String replaceString) {
            try {
                final Scanner scanner = new Scanner(new File(fileName));
                final FileWriter s = new FileWriter(new File(fileName + ".fix"));
                while (scanner.hasNextLine()) {
                    final String nextLine = scanner.nextLine();
                    if (nextLine.contains(findString)) {
                        s.write(replaceString + "\n");
                    } else {
                        s.write(nextLine + "\n");
                    }
                }
                s.close();
                CommonFunctions.runRootCommand("mv " + fileName + ".fix "
                        + fileName);
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        private void fixUpdaterScript() {
            publishProgress("Editing updater script");
            final String updaterScript = romExtractionDir
                    + "META-INF/com/google/android/updater-script";
            String findString = null;
            try {
                final Scanner scanner = new Scanner(new File(updaterScript));
                while (scanner.hasNextLine()) {
                    findString = scanner.nextLine();
                    if (findString.contains("format(")
                            && (findString.contains("\"MTD\", \"system\""))) {
                        findAndReplaceInFile(updaterScript, findString, "");
                    }
                }
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
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

        @Override
        protected void onProgressUpdate(final String... values) {
            super.onProgressUpdate(values);
            WriteOutput(values[0]);
            if (values[0] == "Finished!") {
                new Handler().postDelayed(mFinished, 5000);
            }
        }

        private void packUpAndFinish() {
            publishProgress("Making ROM zip");
            CommonFunctions.runRootCommands(new String[] {
                    "cd " + romExtractionDir,
                    dataDir + "zip -r -q " + finalOutdir + "loop-roms/"
                            + romName + "-loopinstall.zip " + "*" });
        }

        private void preClean() {
            publishProgress("Running a preclean");
            CommonFunctions.deleteIfExists(finalOutdir + "loop-roms/" + romName
                    + "-loopinstall.zip");
            CommonFunctions.deleteIfExists(tempSdCardDir);
        }

        public void WriteOutput(final String paramString) {
            final TextView editText = (TextView) findViewById(R.id.editText1);
            editText.append(paramString + "\n");
            editText.setMovementMethod(new ScrollingMovementMethod());
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_loop_gapps);
        final CreateMultibootGappsAsync instance = new CreateMultibootGappsAsync();
        instance.execute(getIntent().getExtras());
    }

}
