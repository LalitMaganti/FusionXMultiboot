package com.fusionx.tilal6991.multiboot;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class BootRom extends Activity {

	private class chmodDir extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(final Void... arg0) {
			CommonMultibootBase.runRootCommand("chmod 777 " + multibootdir
					+ "*.sh");
			return null;
		}
	}

	private class rebootIntoRom extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(final String... arg0) {
			CommonMultibootBase.runRootCommand("sh " + multibootdir + arg0[0]);
			return null;
		}
	}

	final String multibootdir = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/multiboot/";

	FilenameFilter shFilter = new FilenameFilter() {
		@Override
		public boolean accept(final File dir, final String name) {
			return name.endsWith(".sh");
		}
	};

	public void DisplayProgress(final String title, final String message) {
		ProgressDialog.show(BootRom.this, title, message, true);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_boot_roms);

		final LinearLayout ll = (LinearLayout) findViewById(R.id.layout);

		final File root = new File(multibootdir);
		if (root.exists()) {
			final chmodDir chmod = new chmodDir();
			chmod.execute(null, null);
			for (final String file : root.list(shFilter)) {
				final Button btn = new Button(this);
				btn.setText(file);
				ll.addView(btn);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(final View v) {
						DisplayProgress("Rebooting",
								"Rebooting into specified ROM");
						final rebootIntoRom reboot = new rebootIntoRom();
						reboot.execute(file);
					}
				});
			}
		}
	}
}
