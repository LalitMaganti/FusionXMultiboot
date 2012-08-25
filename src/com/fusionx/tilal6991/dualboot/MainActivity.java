package com.fusionx.tilal6991.dualboot;

import java.io.DataOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.File;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final String sdcardlocation = Environment.getExternalStorageDirectory()
				.getAbsolutePath();

		final String[] chmod = { "chmod 777 /sdcard/multiboot/*.sh" };
		runRootCommands(chmod);

		File root = new File(sdcardlocation + "/multiboot");
		final String files[] = root.list(audioFilter);

		LinearLayout ll = (LinearLayout) findViewById(R.id.layout);

		for (int i = 0; i < files.length; i++) {
			Button btn = new Button(this);
			btn.setText(files[i]);
			ll.addView(btn);
			final String currentfilename = files[i];
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					final String[] sdcard = {currentfilename};
					DisplayToast("Rebooting into specified ROM");
					runRootCommands(sdcard);
				}
			});
		}
	}

	FilenameFilter audioFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			if (name.endsWith(".sh")) {
				return true;
			}
			return false;
		}
	};

	public void runRootCommands(String[] cmds) {
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			for (String tmpCmd : cmds) {
				os.writeBytes(tmpCmd + "\n");
			}
			os.writeBytes("exit\n");
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void DisplayToast(String paramString) {
		Toast.makeText(this, paramString, Toast.LENGTH_SHORT).show();
	}
}
