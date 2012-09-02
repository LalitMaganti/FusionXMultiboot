package com.fusionx.tilal6991.dualboot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        writeRawResource(R.raw.raw, "raw.tar");
        runRootCommand("tar -zxvf /data/data/com.fusionx.tilal6991.dualboot/files/raw.tar -C /data/data/com.fusionx.tilal6991.dualboot/files/");
        runRootCommand("chmod -R 777 /data/data/com.fusionx.tilal6991.dualboot/files/*");
    }

    private void writeRawResource(int resource, String name) {
        if (!(new File("/data/data/com.fusionx.tilal6991.dualboot/files/"
                + name).exists())) {
            try {
                InputStream in = getResources().openRawResource(resource);
                byte[] buffer = new byte[4096];
                OutputStream out = openFileOutput(name, Context.MODE_PRIVATE);
                int n = in.read(buffer, 0, buffer.length);
                while (n >= 0) {
                    out.write(buffer, 0, n);
                    n = in.read(buffer, 0, buffer.length);
                }
                in.close();
                out.close();
            } catch (IOException e) {
            }
        } 
    }
    
    public void openRomBoot(View view) {
        Intent intent = new Intent(this, BootRom.class);
        startActivity(intent);
    }

    public void createRom(View view) {
        Intent intent = new Intent(this, SystemPartition.class);
        startActivity(intent);
    }

     private String runRootCommand(String cmd) {
            Process p = null;
            StringBuilder sb = new StringBuilder();
            try {
                p = Runtime.getRuntime().exec("su");
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
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
            return sb.toString();
        }
}