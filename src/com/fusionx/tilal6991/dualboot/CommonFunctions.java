package com.fusionx.tilal6991.dualboot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class CommonFunctions {
    static String runRootCommand(String cmd) {
        final StringBuilder sb = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            String read = br.readLine();
            while (read != null) {
                sb.append(read + '\n');
                read = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("Mutiboot", cmd);
        Log.d("Mutiboot", sb.toString());
        return sb.toString();
    }

    static String runRootCommands(String[] cmd) {
        final StringBuilder sb = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (int i = 0; i < cmd.length; i++)
                os.writeBytes(cmd[i] + "\n");
            os.writeBytes("exit\n");
            os.flush();
            String read = br.readLine();
            while (read != null) {
                sb.append(read + '\n');
                read = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < cmd.length; i++)
            Log.d("Mutiboot", cmd[i]);
        Log.d("Mutiboot", sb.toString());
        return sb.toString();
    }

    public static void deleteIfExists(String fileName) {
        if (new File(fileName).exists())
            runRootCommand("rm -rf " + fileName);
    }
}