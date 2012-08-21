package com.fusionx.tilal6991.dualboot;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.File;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final Button button = (Button) findViewById(R.id.button);
		final Button button1 = (Button) findViewById(R.id.button1);

		final String sdcardlocation = Environment.getExternalStorageDirectory().getAbsolutePath();

		button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				File file = new File(sdcardlocation + "/multiboot/bootnand.img");
				final String[] nand = {"erase_image boot", "flash_image boot " + file.getAbsolutePath(), "reboot"};
				if(file.exists())
				{
					DisplayToast("Rebooting into NAND ROM");
					runRootCommands(nand);
				}
				else
					DisplayToast("Please check that " + file.getAbsolutePath() + " exists.");
			}
		});

		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				File file = new File(sdcardlocation + "/multiboot/bootsd.img");
				final String[] sdcard = {"erase_image boot", "flash_image boot " + file.getAbsolutePath(), "reboot"};
				if(file.exists())
				{
					DisplayToast("Rebooting into SD card ROM");
					runRootCommands(sdcard);
				}
				else
					DisplayToast("Please check that " + file.getAbsolutePath() + " exists.");
			}
		});
	}

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

	public void DisplayToast(String paramString)
	{
		Toast.makeText(this, paramString, Toast.LENGTH_SHORT).show();
	}
}
