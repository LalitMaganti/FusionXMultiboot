package com.fusionx.tilal6991.multiboot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new CleaupAndExtract().execute(null, null, null);
    }

    private void writeRawResource(int resource, String name) {
        if (!(new File("/data/data/com.fusionx.tilal6991.multiboot/files/"
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

    public void createGapps(View view) {
        Intent intent = new Intent(this, Finalisation.class);
        intent.putExtra("gapps", true);
        startActivity(intent);
    }

    private class CleaupAndExtract extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            CommonFunctions
                    .deleteIfExists("/data/data/com.fusionx.tilal6991.multiboot/files/");
            writeRawResource(R.raw.raw, "raw.tar");
            CommonFunctions
                    .runRootCommand("tar -zxvf /data/data/com.fusionx.tilal6991.multiboot/files/raw.tar -C /data/data/com.fusionx.tilal6991.multiboot/files/");
            CommonFunctions
                    .runRootCommand("chmod -R 777 /data/data/com.fusionx.tilal6991.multiboot/files/*");
            CommonFunctions
                    .deleteIfExists("/data/data/com.fusionx.tilal6991.multiboot/files/raw.tar");
            return null;
        }
    }
}
