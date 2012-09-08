package com.fusionx.tilal6991.multiboot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class CommonFunctions {
    static final String TAG = "FusionXMultiboot";

    static void deleteIfExists(final String fileName) {
        if (new File(fileName).exists()) {
            runRootCommand("rm -rf " + fileName);
        }
    }

    static String runRootCommand(final String cmd) {
        final StringBuilder sb = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            final BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            final DataOutputStream os = new DataOutputStream(
                    p.getOutputStream());
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
        Log.d(TAG, cmd);
        Log.d(TAG, sb.toString());
        return sb.toString();
    }

    static String runRootCommands(final String[] cmd) {
        final StringBuilder sb = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            final BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            final DataOutputStream os = new DataOutputStream(
                    p.getOutputStream());
            for (final String command : cmd) {
                os.writeBytes(command + "\n");
            }
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
        for (final String command : cmd) {
            Log.d(TAG, command);
        }
        Log.d(TAG, sb.toString());
        return sb.toString();
    }
}