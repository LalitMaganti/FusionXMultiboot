package com.fusionx.tilal6991.multiboot;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SystemPartition extends Fragment {
	/*public void next(final View v) {
		final Intent intent = new Intent(this, DataPartition.class);
		final boolean checked = ((CheckBox) findViewById(R.id.checkBox1))
				.isChecked();
		intent.putExtra("createsystemimage", checked);
		if (checked) {
			intent.putExtra("systemimagename",
					((EditText) findViewById(R.id.editText2)).getText()
							.toString());
			intent.putExtra("systemimagesize",
					((EditText) findViewById(R.id.editText1)).getText()
							.toString());
		}
		startActivity(intent);
	}*/

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_system_partition, container, false);
		return view;
	}

}