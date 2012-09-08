package com.fusionx.tilal6991.multiboot;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class BootRom extends Activity {

    FilenameFilter audioFilter = new FilenameFilter() {
        public boolean accept(final File dir, final String name) {
            if (name.endsWith(".sh"))
                return true;
            else
                return false;
        }
    };

    public void DisplayToast(final String paramString) {
        Toast.makeText(this, paramString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_romboot);

        final LinearLayout ll = (LinearLayout) findViewById(R.id.layout);

        final String multibootdir = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/multiboot/";

        final File root = new File(multibootdir);
        if (root.exists()) {
            final String files[] = root.list(audioFilter);

            CommonFunctions
                    .runRootCommand("chmod 777 " + multibootdir + "*.sh");

            for (final String file : files) {
                final Button btn = new Button(this);
                btn.setText(file);
                ll.addView(btn);
                btn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(final View v) {
                        DisplayToast("Rebooting into specified ROM");
                        CommonFunctions.runRootCommand("sh " + multibootdir
                                + file);
                    }
                });
            }
        }
    }
}