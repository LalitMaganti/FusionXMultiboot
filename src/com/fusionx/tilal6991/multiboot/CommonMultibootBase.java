package com.fusionx.tilal6991.multiboot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

public class CommonMultibootBase extends Activity {
	public static final String dataDir = "/data/data/com.fusionx.tilal6991.multiboot/files/";
	public static final String TAG = "FusionXMultiboot";

	static void deleteIfExists(final String fileName) {
		if (new File(fileName).exists())
			runRootCommand("rm -rf " + fileName);
	}

	static void findAndReplaceInFile(final String fileName,
			final String findString, final String replaceString) {
		findAndReplaceInFile(fileName, findString, replaceString, false);
	}

	static void findAndReplaceInFile(final String fileName,
			final String findString, final String replaceString,
			final boolean once) {
		try {
			final Scanner scanner = new Scanner(new File(fileName));
			final FileWriter s = new FileWriter(new File(fileName + ".fix"));
			boolean writtenOnce = false;
			while (scanner.hasNextLine()) {
				final String nextLine = scanner.nextLine();
				if (nextLine.contains(findString) && !writtenOnce) {
					s.write(replaceString + "\n");
					writtenOnce = once;
				} else
					s.write(nextLine + "\n");
			}
			s.close();
			runRootCommand("mv " + fileName + ".fix " + fileName);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	protected static boolean findTextInFile(final String fileName,
			final String findString) {
		try {
			final Scanner scanner = new Scanner(new File(fileName));
			while (scanner.hasNextLine()) {
				final String nextLine = scanner.nextLine();
				if (nextLine.contains(findString))
					return true;
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	static String runRootCommand(final String cmd) {
		final StringBuilder sb = new StringBuilder();
		try {
			final Process p = Runtime.getRuntime().exec("su");
			final BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			final DataOutputStream os = new DataOutputStream(
					p.getOutputStream());
			Log.d(TAG, cmd);
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit\n");
			os.flush();
			String read = br.readLine();
			while (read != null) {
				sb.append(read + '\n');
				read = br.readLine();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	static void writeToFile(final String fileName, final String stringToWrite) {
		try {
			final FileWriter fileWriter = new FileWriter(fileName);
			fileWriter.write(stringToWrite);
			fileWriter.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
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
					MainActivity.class);
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

		runRootCommand("cp " + tempSdCardDir + "init.rc " + dataDir
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

		runRootCommand("cp " + dataDir + "boot.img-ramdisk/init.rc "
				+ tempSdCardDir + "init.rc");
		deleteIfExists(dataDir + "boot.img-ramdisk/init.rc");
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
						+ " --kernel boot.img-kernel --ramdisk ramdisk.gz -o "
						+ romExtractionDir + "boot.img" });
	}

	protected void makeDirectories() {
		new File(romExtractionDir).mkdirs();
		new File(finalOutdir + "loop-roms").mkdirs();
		new File(finalOutdir + "boot-images").mkdirs();
		new File(tempSdCardDir + "tempFlashBoot/META-INF/com/google/android/")
				.mkdirs();
	}

	protected void makeRamdisk() {
		runRootCommands(new String[] { "cd " + dataDir,
				"./mkbootfs boot.img-ramdisk | gzip > ramdisk.gz" });
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

	protected String runRootCommands(final String[] cmd) {
		final StringBuilder sb = new StringBuilder();
		try {
			final Process p = Runtime.getRuntime().exec("su");
			final BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			final DataOutputStream os = new DataOutputStream(
					p.getOutputStream());
			for (final String command : cmd) {
				Log.d(TAG, command);
				os.writeBytes(command + "\n");
			}
			os.writeBytes("exit\n");
			os.flush();
			String read = br.readLine();
			while (read != null) {
				Log.d(TAG, read);
				sb.append(read + '\n');
				read = br.readLine();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	protected void WriteOutput(final String paramString) {
		final TextView editText = (TextView) findViewById(R.id.editText1);
		editText.append(paramString + "\n");
		editText.setMovementMethod(new ScrollingMovementMethod());
	}
}