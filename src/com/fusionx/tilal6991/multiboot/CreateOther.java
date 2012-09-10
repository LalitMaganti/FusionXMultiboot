package com.fusionx.tilal6991.multiboot;

import java.io.File;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

public class CreateOther extends CommonMultibootBase {

	private class CreateMultibootGappsAsync extends
			AsyncTask<Bundle, String, Void> {
		private final Runnable mFinished = new Runnable() {
			@Override
			public void run() {
				final Intent intent = new Intent(getApplicationContext(),
						MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		};

		private void cleanup() {
			publishProgress("Cleaning up");
			deleteIfExists(tempSdCardDir);
			deleteIfExists(dataDir);
			publishProgress("PLEASE FLASH THE BOOT IMAGE OF THE ROM YOU WANT TO BOOT INTO IN RECOVERY AFTER FLASHING THE PACKAGE JUST CREATED. The files can be found at /sdcard/multiboot/boot-images/");
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
			publishProgress("Editing updater script");
			fixUpdaterScript();
			packUpAndFinish();
			cleanup();
			return null;
		}

		private void extractRom() {
			publishProgress("Extracting ROM");
			runRootCommand(dataDir + "busybox unzip -q " + inputFile + " -d "
					+ romExtractionDir);
		}

		@Override
		protected void onProgressUpdate(final String... values) {
			super.onProgressUpdate(values);
			WriteOutput(values[0]);
			if (values[0] == "Finished!")
				mHandler.postDelayed(mFinished, 15000);
		}

		private void packUpAndFinish() {
			publishProgress("Making ROM zip");
			runRootCommands(new String[] {
					"cd " + romExtractionDir,
					dataDir + "zip -r -q " + finalOutdir + "loop-roms/"
							+ romName + "-loopinstall.zip " + "*" });
		}

		private void preClean() {
			publishProgress("Running a preclean");
			deleteIfExists(finalOutdir + "loop-roms/" + romName
					+ "-loopinstall.zip");
			deleteIfExists(tempSdCardDir);
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_make_somthing);
		final CreateMultibootGappsAsync instance = new CreateMultibootGappsAsync();
		instance.execute(getIntent().getExtras());
	}

}
