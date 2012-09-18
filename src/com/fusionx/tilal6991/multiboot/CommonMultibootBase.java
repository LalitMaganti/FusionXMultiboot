package com.fusionx.tilal6991.multiboot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class CommonMultibootBase extends CommonFunctions {
	public static final String dataDir = "/data/data/com.fusionx.tilal6991.multiboot/files/";
	public static final String workingDir = dataDir + "working/";

	protected static void makeImage(final String imageOutput,
			final int imageSize) {
		final String losetupLocation = CommonFunctions.runRootCommand(
				"losetup -f").trim();
		runRootCommand("dd if=/dev/zero of=" + imageOutput + " bs=1024 count="
				+ imageSize);
		runRootCommand("losetup " + losetupLocation + " " + imageOutput);
		runRootCommand("mke2fs -t ext2 " + losetupLocation);
		try {
			Thread.sleep(10000);
		} catch (final InterruptedException e) {
			e.getStackTrace();
		}
		runRootCommand("losetup -d " + losetupLocation);
	}

	String base;

	protected Bundle bundle;

	String commandLine;

	protected String dataImageName;

	String externalDir;

	protected final String externalPath = Environment
			.getExternalStorageDirectory().getAbsolutePath();

	protected final String finalOutdir = externalPath + "/multiboot/";

	protected String inputFile;

	protected final Runnable mFinish = new Runnable() {
		@Override
		public void run() {
			final Intent intent = new Intent(getApplicationContext(),
					HomeScreen.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
	};

	protected final Handler mHandler = new Handler();

	protected String romExtractionDir;

	protected String romName;

	protected String systemImageName;

	protected String tempFlashableBootDir;

	protected final String tempSdCardDir = externalPath + "/tempMultiboot/";

	protected void editInitRc() {
		if (findTextInFile(romExtractionDir + "system/build.prop",
				"ro.build.version.release=4.1"))
			externalDir = "/storage/sdcard0";
		else
			externalDir = "/sdcard";

		findAndReplaceInFile(tempSdCardDir + "init.rc", "on fs", "on fs\n"
				+ "    mkdir -p " + externalDir + "\n"
				+ "    mount vfat /dev/block/mmcblk0p1 " + externalDir, true);

		findAndReplaceInFile(tempSdCardDir + "init.rc",
				"mount yaffs2 mtd@system /system ro remount",
				"    mount ext2 loop@" + externalDir + "/multiboot/"
						+ systemImageName + " /system ro remount");

		findAndReplaceInFile(tempSdCardDir + "init.rc",
				"mount yaffs2 mtd@system /system", "    mount ext2 loop@"
						+ externalDir + "/multiboot/" + systemImageName
						+ " /system");

		findAndReplaceInFile(tempSdCardDir + "init.rc",
				"mount yaffs2 mtd@userdata /data nosuid nodev",
				"    mount ext2 loop@" + externalDir + "/multiboot/"
						+ dataImageName + " /data nosuid nodev");

		runRootCommand("cp " + tempSdCardDir + "init.rc " + workingDir
				+ "boot.img-ramdisk/init.rc");
		deleteIfExists(tempSdCardDir + "init.rc");
	}

	protected void extractKernel() {
		runRootCommands(new String[] { "cd " + dataDir,
				"./extract-kernel.pl " + dataDir + "boot.img" });
	}

	protected void extractRamdisk() {
		runRootCommands(new String[] { "cd " + dataDir,
				"./extract-ramdisk.pl " + dataDir + "boot.img" });
		runRootCommands(new String[] { "cd " + dataDir + "boot.img-ramdisk",
				"gunzip -c ../boot.img-ramdisk.cpio.gz | cpio -i" });

		deleteIfExists(dataDir + "boot.img");
		
		runRootCommand("mv " + dataDir + "boot.img-ramdisk " + workingDir);
		runRootCommand("mv " + dataDir + "boot.img-kernel " + workingDir);

		runRootCommand("cp " + workingDir + "boot.img-ramdisk/init.rc "
				+ tempSdCardDir + "init.rc");
		deleteIfExists(workingDir + "boot.img-ramdisk/init.rc");
	}

	protected void fixUpdaterScript() {
		String findString = null;
		final String updaterScript = romExtractionDir
				+ "META-INF/com/google/android/updater-script";
		try {
			final Scanner scanner = new Scanner(new File(updaterScript));
			while (scanner.hasNextLine()) {
				findString = scanner.nextLine();
				if (findString.contains("format(")
						&& findString.contains("\"MTD\", \"system\""))
					findAndReplaceInFile(
							updaterScript,
							findString,
							"run_program(\"/sbin/losetup\", \"/dev/block/loop0\", \"/sdcard/multiboot/"
									+ systemImageName
									+ "\");\n"
									+ "run_program(\"/sbin/mke2fs\", \"-T\", \"ext2\", \"/dev/block/loop0\");\n"
									+ "run_program(\"/sbin/losetup\", \"-d\", \"/dev/block/loop0\");");
				else if (findString.contains("format(")
						&& findString.contains("\"MTD\", \"userdata\""))
					findAndReplaceInFile(updaterScript, findString, "");
				else if (findString.contains("mount(")
						&& findString.contains("\"MTD\", \"system\""))
					findAndReplaceInFile(
							updaterScript,
							findString,
							"run_program(\"/sbin/losetup\", \"/dev/block/loop0\", \"/sdcard/multiboot/"
									+ systemImageName
									+ "\");\n"
									+ "run_program(\"/sbin/mount\", \"-t\", \"ext2\", \"/dev/block/loop0\", \"/system\");");
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		findAndReplaceInFile(
				updaterScript,
				"unmount(\"/system\");",
				"unmount(\"/system\");\n"
						+ "run_program(\"/sbin/losetup\", \"-d\", \"/dev/block/loop0\");");
		findAndReplaceInFile(
				updaterScript,
				"run_program(\"/sbin/busybox\", \"mount\", \"/system\");",
				"run_program(\"/sbin/losetup\", \"/dev/block/loop0\", \"/sdcard/multiboot/"
						+ systemImageName
						+ "\");\n"
						+ "run_program(\"/sbin/mount\", \"-t\", \"ext2\", \"/dev/block/loop0\", \"/system\");");
		findAndReplaceInFile(
				updaterScript,
				"run_program(\"/sbin/busybox\", \"umount\", \"/system\");",
				"unmount(\"/system\");\n"
						+ "run_program(\"/sbin/losetup\", \"-d\", \"/dev/block/loop0\");");
	}

	protected void getBootImageParameters() {
		base = "0x"
				+ runRootCommand(
						dataDir + "busybox od -A n -h -j 34 -N 2 " + dataDir
								+ "boot.img").trim() + "0000";
		commandLine = "'"
				+ runRootCommand(
						dataDir + "busybox od -A n --strings -j 64 -N 512 "
								+ dataDir + "boot.img").trim() + "'";
	}

	protected void makeBootImage() {
		runRootCommands(new String[] {
				"cd " + dataDir,
				"./mkbootimg --cmdline " + commandLine + " --base " + base
						+ " --kernel + " + workingDir
						+ "boot.img-kernel --ramdisk " + workingDir
						+ "ramdisk.gz -o " + romExtractionDir + "boot.img" });
	}

	protected void makeDirectories() {
		makeDirectoryIfNotExists(workingDir);
		makeDirectoryIfNotExists(romExtractionDir);
		makeDirectoryIfNotExists(finalOutdir + "loop-roms");
		makeDirectoryIfNotExists(finalOutdir + "boot-images");
		makeDirectoryIfNotExists(tempSdCardDir
				+ "tempFlashBoot/META-INF/com/google/android/");
	}

	protected void makeRamdisk() {
		runRootCommands(new String[] { "cd " + dataDir,
				"./mkbootfs " + workingDir + "boot.img-ramdisk | gzip > " + workingDir + "ramdisk.gz" });
	}

	protected void moveBootImage() {
		runRootCommand("cp " + romExtractionDir + "boot.img " + dataDir
				+ "boot.img");
		deleteIfExists(romExtractionDir + "boot.img");
	}

	@Override
	public void onBackPressed() {
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_make_somthing);
	}

	protected void preClean() {
		deleteIfExists(finalOutdir + romName + "boot.img");
		deleteIfExists(finalOutdir + "boot" + romName + ".sh");
		deleteIfExists(finalOutdir + "loop-roms/" + romName
				+ "-loopinstall.zip");
		deleteIfExists(tempSdCardDir);
	}

	protected void WriteOutput(final String paramString) {
		final TextView editText = (TextView) findViewById(R.id.editText1);
		editText.append(paramString + "\n");
		editText.setMovementMethod(new ScrollingMovementMethod());
	}
}