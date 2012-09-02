package com.fusionx.tilal6991.dualboot;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class SystemPartition extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_partition);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_system_partition, menu);
        return true;
    }
    public void next(View v) {
        Intent intent = new Intent(this, DataPartition.class);
        boolean checked = ((CheckBox) (findViewById(R.id.checkBox1))).isChecked();
        intent.putExtra("createsystemimage", checked);
        if (checked)
        {
            intent.putExtra("systemimagename", ((EditText) findViewById(R.id.editText2)).getText().toString());
            intent.putExtra("systemimagesize", ((EditText) findViewById(R.id.editText1)).getText().toString());
        }
        startActivity(intent);
    }
    public void changeEnable(View view) {
        int visible;
        if(!(((CheckBox)view).isChecked()))
            visible = 4;
        else
            visible = 0;
        ((EditText) findViewById(R.id.editText1)).setVisibility(visible);
        ((EditText) findViewById(R.id.editText2)).setVisibility(visible);
        ((TextView) findViewById(R.id.textView1)).setVisibility(visible);
        ((TextView) findViewById(R.id.textView2)).setVisibility(visible);
    }
}