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
                return filename.endsWith(Globals.getString("Finalisation.0")); //$NON-NLS-1$
            }
        };
        final String[] mFileList = mPath.list(filter);

        final Builder builder = new Builder(this);
        builder.setTitle(Globals.getString("Finalisation.1")); //$NON-NLS-1$

        final DialogInterface.OnClickListener k = new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final File sel = new File(mFileList[which]);
                if (sel.isDirectory()) {
                    chooseRom(new File(mPath.getAbsolutePath() + Globals.getString("Finalisation.2") //$NON-NLS-1$
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
        if (b.getBoolean(Globals.getString("Finalisation.3")) == true) { //$NON-NLS-1$
            intent = new Intent(this, MakeLoopGapps.class);
            intent.putExtra(Globals.getString("Finalisation.4"), //$NON-NLS-1$
                    ((EditText) findViewById(R.id.edtSystem)).getText()
                            .toString());
        } else {
            intent = new Intent(this, CreateMultiBootRom.class);
            if (b.getBoolean(Globals.getString("Finalisation.5")) == false) { //$NON-NLS-1$
                intent.putExtra(Globals.getString("Finalisation.6"), //$NON-NLS-1$
                        ((EditText) findViewById(R.id.edtData)).getText()
                                .toString());
            }
            if (b.getBoolean(Globals.getString("Finalisation.7")) == false) { //$NON-NLS-1$
                intent.putExtra(Globals.getString("Finalisation.8"), //$NON-NLS-1$
                        ((EditText) findViewById(R.id.edtSystem)).getText()
                                .toString());
            }
        }
        intent.putExtra(Globals.getString("Finalisation.9"), mChosen); //$NON-NLS-1$
        intent.putExtras(getIntent().getExtras());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalisation);
        b = getIntent().getExtras();
        if (b.getBoolean(Globals.getString("Finalisation.10")) == true) { //$NON-NLS-1$
            findViewById(R.id.edtData).setVisibility(4);
            findViewById(R.id.txtData).setVisibility(4);
        } else {
            if (b.getBoolean(Globals.getString("Finalisation.11")) == true) { //$NON-NLS-1$
                findViewById(R.id.edtData).setVisibility(4);
                findViewById(R.id.txtData).setVisibility(4);
            }
            if (b.getBoolean(Globals.getString("Finalisation.12")) == true) { //$NON-NLS-1$
                findViewById(R.id.edtSystem).setVisibility(4);
                findViewById(R.id.txtSystem).setVisibility(4);
            }
        }
    }
}
