package org.topnetwork.pintogether.permission;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


/**
 * Created by lgc on 2019/6/11.
 *
 * Permissions helper for {@link Fragment} from the framework.
 */
public class FrameworkFragmentPermissionHelper extends BaseFrameworkPermissionHelper<Fragment> {

	protected FrameworkFragmentPermissionHelper(@NonNull Fragment host) {
		super(host);
	}

	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public FragmentManager getFragmentManager() {
		return getHost().getChildFragmentManager();
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public void directRequestPermissions(int requestCode, @NonNull String... perms) {
		getHost().requestPermissions(perms, requestCode);
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public boolean shouldShowRequestPermissionRational(@NonNull String perm) {
		return getHost().shouldShowRequestPermissionRationale(perm);
	}

	@Override
	public Context getContext() {
		return getHost().getActivity();
	}
}
