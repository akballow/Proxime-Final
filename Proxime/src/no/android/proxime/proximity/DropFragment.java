/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.android.proxime.proximity;

import no.android.proxime.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class DropFragment extends DialogFragment {
	private static final String ARG_NAME = "name";

	private String mName;

	public static DropFragment getInstance(String name) {
		final DropFragment fragment = new DropFragment();

		final Bundle args = new Bundle();
		args.putString(ARG_NAME, name);
		fragment.setArguments(args);

		return fragment;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mName = getArguments().getString(ARG_NAME);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.app_name)).setMessage(getString(R.string.proximity_notification_drop, mName))
				.setPositiveButton(android.R.string.ok, null).create();
	
		
	}

}
