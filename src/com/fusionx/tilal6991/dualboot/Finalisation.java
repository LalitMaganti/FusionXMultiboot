package com.fusionx.tilal6991.dualboot;

import java.io.File;
import java.io.FilenameFilter;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class Finalisation extends Activity {
    String mChosen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalisation);
        Bundle b = getIntent().getExtras();
        boolean data = b.getBoolean("createdataimage");
        boolean system = b.getBoolean("createsystemimage");
        if (data == true) {
            findViewById(R.id.edtData).setVisibility(4);
            findViewById(R.id.txtData).setVisibility(4);
        }
        if (system == true) {
            findViewById(R.id.edtSystem).setVisibility(4);
            findViewById(R.id.txtSystem).setVisibility(4);
        }
    }

    public void chooseRom(final View v) {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".zip");
            }
        };
        File mPath = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath());
        final String[] mFileList = mPath.list(filter);

        Builder builder = new Builder(this);
        builder.setTitle("Choose your ROM");
        builder.setItems(mPath.list(filter),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        File sel = new File(mFileList[which]);
                        if (!(sel.isDirectory())) {
                            mChosen = mFileList[which];
                            TextView k = (TextView) findViewById(R.id.txtRom);
                            k.setText(mChosen);
                        }
                    }
                });
        builder.show();
    }

    public void finish(View view) {
        Intent intent = new Intent(this, CreateMultiBootRom.class);
        intent.putExtra("filename", mChosen);
        Bundle b = getIntent().getExtras();
        boolean data = b.getBoolean("createdataimage");
        boolean system = b.getBoolean("createsystemimage");
        if (data == false)
            intent.putExtra("dataimagename",
                    ((EditText) findViewById(R.id.edtData)).getText()
                            .toString());
        if (system == false)
            intent.putExtra("systemimagename",
                    ((EditText) findViewById(R.id.edtSystem)).getText()
                            .toString());
        intent.putExtras(getIntent().getExtras());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
