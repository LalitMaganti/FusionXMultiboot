package com.fusionx.tilal6991.multiboot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
	private class CleaupAndExtract extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(final Void... arg0) {
			CommonMultibootBase
					.deleteIfExists("/data/data/com.fusionx.tilal6991.multiboot/files/");
			writeRawResource(R.raw.raw, "raw.tar");
			CommonMultibootBase
					.runRootCommand("tar -zxvf /data/data/com.fusionx.tilal6991.multiboot/files/raw.tar -C /data/data/com.fusionx.tilal6991.multiboot/files/");
			CommonMultibootBase
					.runRootCommand("chmod -R 777 /data/data/com.fusionx.tilal6991.multiboot/files/*");
			CommonMultibootBase
					.deleteIfExists("/data/data/com.fusionx.tilal6991.multiboot/files/raw.tar");
			return null;
		}
	}

	public void createGapps(final View view) {
		final Intent intent = new Intent(this, Finalisation.class);
		intent.putExtra("type", "gapps");
		startActivity(intent);
	}

	public void createRom(final View view) {
		final Intent intent = new Intent(this, SystemPartition.class);
		intent.putExtra("type", "normal");
		startActivity(intent);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		new CleaupAndExtract().execute(null, null);
	}

	public void openRomBoot(final View view) {
		final Intent intent = new Intent(this, BootRom.class);
		startActivity(intent);
	}

	private void writeRawResource(final int resource, final String name) {
		if (!new File("/data/data/com.fusionx.tilal6991.multiboot/files/"
				+ name).exists())
			try {
				final InputStream in = getResources().openRawResource(resource);
				final byte[] buffer = new byte[4096];
				final OutputStream out = openFileOutput(name,
						Context.MODE_PRIVATE);
				int n = in.read(buffer, 0, buffer.length);
				while (n >= 0) {
					out.write(buffer, 0, n);
					n = in.read(buffer, 0, buffer.length);
				}
				in.close();
				out.close();
			} catch (final IOException e) {
			}
	}
}
