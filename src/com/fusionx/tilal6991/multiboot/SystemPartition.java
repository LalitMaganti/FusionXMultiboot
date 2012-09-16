package com.fusionx.tilal6991.multiboot;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SystemPartition extends Fragment {
	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_system_partition, container, false);
		return view;
	}
}