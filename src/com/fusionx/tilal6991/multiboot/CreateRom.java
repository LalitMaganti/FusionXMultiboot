package com.fusionx.tilal6991.multiboot;

import java.io.File;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

public class CreateRom extends CommonMultibootBase {
	private class CreateMultibootRomAsync extends
			AsyncTask<Bundle, String, Void> {

		private void cleanup() {
			publishProgress("Cleaning up");
			deleteIfExists(tempSdCardDir);
			deleteIfExists(dataDir);
			publishProgress("Finished!");
		}

		@Override
		protected Void doInBackground(final Bundle... params) {
			bundle = params[0];
			inputFile = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/" + bundle.getString("filename");
			romName = bundle.getString("filename").replace(".zip", "");
			romExtractionDir = tempSdCardDir + romName + "/";
			tempFlashableBootDir = tempSdCardDir + "tempFlashBoot/";

			preClean();

			publishProgress("Making directories");
			new File(romExtractionDir).mkdirs();
			new File(finalOutdir + "loop-roms").mkdirs();
			new File(finalOutdir + "boot-images").mkdirs();
			new File(tempSdCardDir
					+ "tempFlashBoot/META-INF/com/google/android/").mkdirs();

			publishProgress("Getting data from wizard");
			dataImageName = bundle.getString("dataimagename");
			systemImageName = bundle.getString("systemimagename");

			final boolean data = bundle.getBoolean("createdataimage");
			final boolean system = bundle.getBoolean("createsystemimage");

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

		private void extractRom() {
			publishProgress("Extracting ROM - this may take quite some time");
			runRootCommand(dataDir + "busybox unzip -q " + inputFile + " -d "
					+ romExtractionDir);
		}

		private void fixUpdaterScript() {
			publishProgress("Editing updater script");
			fixUpdaterScript();
		}

		private void makeDataImage() {
			publishProgress("Making data image - this may take quite some time");
			makeImage(finalOutdir + dataImageName,
					Integer.parseInt(bundle.getString("dataimagesize")) * 1024);
		}

		private void makeImage(final String imageOutput, final int imageSize) {
			final String losetupLocation = runRootCommand("losetup -f").trim();
			runRootCommand("dd if=/dev/zero of=" + imageOutput
					+ " bs=1024 count=" + imageSize);
			runRootCommand("losetup " + losetupLocation + " " + imageOutput);
			runRootCommand("mke2fs -t ext2 " + losetupLocation);
			try {
				Thread.sleep(10000);
			} catch (final InterruptedException e) {
				e.getStackTrace();
			}
			runRootCommand("losetup -d " + losetupLocation);
		}

		private void makeSystemImage() {
			publishProgress("Making system image - this may take quite some time");
			makeImage(
					finalOutdir + systemImageName,
					Integer.parseInt(bundle.getString("systemimagesize")) * 1024);
		}

		@Override
		protected void onProgressUpdate(final String... values) {
			super.onProgressUpdate(values);
			WriteOutput(values[0]);
			if (values[0] == "Finished!")
				mHandler.postDelayed(mFinish, 5000);
		}

		private void packUpAndFinish() {
			publishProgress("Making ROM zip - this may take quite some time");
			runRootCommands(new String[] {
					"cd " + romExtractionDir,
					dataDir + "zip -r -q " + finalOutdir + "loop-roms/"
							+ romName + "-loopinstall.zip " + "*" });

			publishProgress("Creating copy of loop boot image for flashing in recovery");
			runRootCommand("cp " + romExtractionDir + "boot.img "
					+ tempFlashableBootDir + "boot.img");

			final String updaterScriptFile = "package_extract_file(\"boot.img\", \"/tmp/boot.img\");write_raw_image(\"/tmp/boot.img\", \"boot\");";

			publishProgress("Creating flashable boot image in recovery");
			CommonMultibootBase
					.writeToFile(
							tempSdCardDir
									+ "tempFlashBoot/META-INF/com/google/android/updater-script",
							updaterScriptFile);

			CommonMultibootBase
					.runRootCommand("cp "
							+ romExtractionDir
							+ "META-INF/com/google/android/update-binary "
							+ tempSdCardDir
							+ "tempFlashBoot/META-INF/com/google/android/update-binary");

			runRootCommands(new String[] {
					"cd " + tempFlashableBootDir,
					dataDir + "zip -r -q " + finalOutdir + "boot-images/"
							+ romName + "-bootimage.zip " + "*" });

			publishProgress("Creating copy of loop boot image for flashing in app");
			runRootCommand("cp " + romExtractionDir + "boot.img " + finalOutdir
					+ romName + "boot.img");

			String shFile = "#!/system/bin/sh\n"
					+ "flash_image boot /sdcard/multiboot/" + romName
					+ "boot.img\n" + "reboot";

			publishProgress("Creating loop script file");
			writeToFile(finalOutdir + "boot" + romName + ".sh", shFile);

			runRootCommand("cp /init.rc " + tempSdCardDir
					+ "currentRom.init.rc");

			if (!findTextInFile(tempSdCardDir + "currentRom.init.rc",
					"mount ext2 loop@")) {
				deleteIfExists(finalOutdir + "boot.img");
				deleteIfExists(finalOutdir + "boot.sh");

				publishProgress("Creating nand boot image");
				CommonMultibootBase
						.runRootCommand("dd if=/dev/mtd/mtd1 of=/sdcard/multiboot/boot.img bs=4096");

				shFile = "#!/system/bin/sh\n"
						+ "flash_image boot /sdcard/multiboot/boot.img\n"
						+ "reboot";

				publishProgress("Creating nand script file");
				writeToFile(finalOutdir + "boot.sh", shFile);
			}
		}

		private void preClean() {
			publishProgress("Running a preclean");
			deleteIfExists(finalOutdir + romName + "boot.img");
			deleteIfExists(finalOutdir + "boot" + romName + ".sh");
			deleteIfExists(finalOutdir + "loop-roms/" + romName
					+ "-loopinstall.zip");
			deleteIfExists(tempSdCardDir);
		}

		protected void remakeBootImage() {
			publishProgress("Moving boot image");
			moveBootImage();

			publishProgress("Getting boot.img parameters");
			getBootImageParameters();

			publishProgress("Extracting kernel");
			extractKernel();

			publishProgress("Extracting ramdisk");
			extractRamdisk();

			publishProgress("Editing init.rc");
			editInitRc();

			publishProgress("Making compressed ramdisk");
			makeRamdisk();

			publishProgress("Making boot image");
			makeBootImage();

		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_make_somthing);
		final CreateMultibootRomAsync instance = new CreateMultibootRomAsync();
		instance.execute(getIntent().getExtras());
	}
}