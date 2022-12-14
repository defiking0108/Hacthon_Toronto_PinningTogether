package org.topnetwork.pintogether.permission;

import android.content.Context;

import androidx.annotation.NonNull;


/**
 * Created by lgc on 2019/6/11.
 *
 * Permissions helper for apps built against API < 23, which do not need runtime permissions.
 */
public class LowApiPermissionHelper extends PermissionHelper<Object> {

	protected LowApiPermissionHelper(@NonNull Object host) {
		super(host);
	}

	@Override
	public void directRequestPermissions(int requestCode, @NonNull String... perms) {
		throw new IllegalStateException("Should never be requesting permissions on API < 23!");
	}

	@Override
	public boolean shouldShowRequestPermissionRational(@NonNull String perm) {
		return false;
	}

	@Override
	public void showRequestPermissionRational(@NonNull String rational, int requestCode, @NonNull String... perms) {
		throw new IllegalStateException("Should never be requesting permissions on API < 23!");
	}

	@Override
	public Context getContext() {
		return null;
	}
}
