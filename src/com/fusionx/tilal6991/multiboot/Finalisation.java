package com.fusionx.tilal6991.multiboot;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Finalisation extends Activity {
    Bundle b;
    String mChosen;

    public void chooseRom(final File mPath) {
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(final File dir, final String filename) {
                return filename.endsWith(".zip");
            }
        };
        final String[] mFileList = mPath.list(filter);

        final Builder builder = new Builder(this);
        builder.setTitle("Choose your ROM");

        final DialogInterface.OnClickListener k = new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final File sel = new File(mFileList[which]);
                if (sel.isDirectory()) {
                    chooseRom(new File(mPath.getAbsolutePath() + "/"
                            + sel.getName()));
                } else {
                    mChosen = mFileList[which];
                    final TextView k = (TextView) findViewById(R.id.txtRom);
                    k.setText(mChosen);
                    final Button l = (Button) findViewById(R.id.button1);
                    l.setEnabled(true);
                }
            }
        };
        builder.setItems(mFileList, k);
        builder.show();
    }

    public void chooseRom(final View v) {
        chooseRom(Environment.getExternalStorageDirectory());
    }

    public void finish(final View view) {
        Intent intent;
        if (b.getBoolean("gapps") == true) {
            intent = new Intent(this, MakeLoopGapps.class);
            intent.putExtra("systemimagename",
                    ((EditText) findViewById(R.id.edtSystem)).getText()
                            .toString());
        } else {
            intent = new Intent(this, CreateMultiBootRom.class);
            if (b.getBoolean("createdataimage") == false) {
                intent.putExtra("dataimagename",
                        ((EditText) findViewById(R.id.edtData)).getText()
                                .toString());
            }
            if (b.getBoolean("createsystemimage") == false) {
                intent.putExtra("systemimagename",
                        ((EditText) findViewById(R.id.edtSystem)).getText()
                                .toString());
            }
        }
        intent.putExtra("filename", mChosen);
        intent.putExtras(getIntent().getExtras());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalisation);
        b = getIntent().getExtras();
        if (b.getBoolean("gapps") == true) {
            findViewById(R.id.edtData).setVisibility(4);
            findViewById(R.id.txtData).setVisibility(4);
        } else {
            if (b.getBoolean("createdataimage") == true) {
                findViewById(R.id.edtData).setVisibility(4);
                findViewById(R.id.txtData).setVisibility(4);
            }
            if (b.getBoolean("createsystemimage") == true) {
                findViewById(R.id.edtSystem).setVisibility(4);
                findViewById(R.id.txtSystem).setVisibility(4);
            }
        }
    }
}
