package com.fusionx.tilal6991.multiboot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class DataPartition extends Activity {
    public void changeEnable(final View view) {
        int visible;
        if (!((CheckBox) view).isChecked()) {
            visible = 4;
        } else {
            visible = 0;
        }
        findViewById(R.id.editText1).setVisibility(visible);
        findViewById(R.id.editText2).setVisibility(visible);
        findViewById(R.id.textView1).setVisibility(visible);
        findViewById(R.id.textView2).setVisibility(visible);
    }

    public void next(final View v) {
        final Intent intent = new Intent(this, Finalisation.class);
        final boolean checked = ((CheckBox) (findViewById(R.id.checkBox1)))
                .isChecked();
        intent.putExtra("createdataimage", checked);
        if (checked) {
            intent.putExtra("dataimagename",
                    ((EditText) findViewById(R.id.editText2)).getText()
                            .toString());
            intent.putExtra("dataimagesize",
                    ((EditText) findViewById(R.id.editText1)).getText()
                            .toString());
        }
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_partition);
        changeEnable(findViewById(R.id.checkBox1));
    }
}