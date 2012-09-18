package com.fusionx.tilal6991.multiboot;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

public class CreateOther extends CommonMultibootBase {

	private class CreateMultibootGappsAsync extends
			AsyncTask<Bundle, String, Void> {
		private void cleanup() {
			publishProgress("Cleaning up");
			CommonFunctions.deleteIfExists(tempSdCardDir);
			CommonFunctions.deleteIfExists(workingDir);
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
			makeDirectories();

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
			publishProgress("Extracting misc package file");
			CommonFunctions.runRootCommand(dataDir + "busybox unzip -q "
					+ inputFile + " -d " + romExtractionDir);
		}

		@Override
		protected void onProgressUpdate(final String... values) {
			super.onProgressUpdate(values);
			WriteOutput(values[0]);
			if (values[0] == "Finished!")
				mHandler.postDelayed(mFinish, 15000);
		}

		private void packUpAndFinish() {
			publishProgress("Making package zip");
			runRootCommands(new String[] {
					"cd " + romExtractionDir,
					dataDir + "zip -r -q " + finalOutdir + "loop-roms/"
							+ romName + "-loopinstall.zip " + "*" });
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
