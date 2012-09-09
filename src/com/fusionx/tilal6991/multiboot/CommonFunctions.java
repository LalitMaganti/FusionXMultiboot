package com.fusionx.tilal6991.multiboot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import android.util.Log;

public class CommonFunctions {
	private static final String TAG = "FusionXMultiboot";

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
			while (scanner.hasNextLine()) {
				final String nextLine = scanner.nextLine();
				if (nextLine.contains(findString)) {
					s.write(replaceString + "\n");
					if (once)
						break;
				} else
					s.write(nextLine + "\n");
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
				Log.d(TAG, read);
				sb.append(read + '\n');
				read = br.readLine();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	static String runRootCommands(final String[] cmd) {
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

	static void writeToFile(final String fileName, final String stringToWrite) {
		try {
			final FileWriter fileWriter = new FileWriter(fileName);
			fileWriter.write(stringToWrite);
			fileWriter.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}