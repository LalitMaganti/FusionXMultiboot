package com.fusionx.tilal6991.dualboot;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.EditText;
import android.widget.Toast;

public class MakeMultiBoot extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_multi_boot);
        RunRootCommandsAsync h = new RunRootCommandsAsync();
        h.execute(getIntent().getExtras());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_make_multi_boot, menu);
        return true;
    }

    public void DisplayToast(String paramString) {
        Toast.makeText(this, paramString, Toast.LENGTH_SHORT).show();
    }
    public void WriteOutput(String paramString) {
        EditText k = (EditText) findViewById(R.id.editText1);
        k.append(paramString);
    }
}