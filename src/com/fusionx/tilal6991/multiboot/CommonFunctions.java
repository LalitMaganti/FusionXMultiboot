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
            Log.d(TAG, cmd);
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            String read = br.readLine();
            while (read != null) {
                sb.append(read + '\n');
                read = br.readLine();
                Log.d(TAG, read);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
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
                Log.d(TAG, command);
                os.writeBytes(command + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            String read = br.readLine();
            while (read != null) {
                sb.append(read + '\n');
                read = br.readLine();
                Log.d(TAG, read);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}