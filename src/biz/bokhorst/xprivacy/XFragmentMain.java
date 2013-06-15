package biz.bokhorst.xprivacy;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class XFragmentMain extends FragmentActivity {

	private final static int cXposedMinVersion = 34;

	private PagerAdapter mPageAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setContentView(R.layout.xmain);

		// Setup pager
		List<Fragment> listFragment = new ArrayList<Fragment>();
		listFragment.add(new XFragmentApp());
		listFragment.add(new XFragmentRestriction());
		mPageAdapter = new PagerAdapter(getSupportFragmentManager(), listFragment);
		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		viewPager.setAdapter(mPageAdapter);

		// Check Android version
		if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
				&& Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN_MR1) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(getString(R.string.app_name));
			alertDialog.setMessage(getString(R.string.app_wrongandroid));
			alertDialog.setIcon(R.drawable.ic_launcher);
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			alertDialog.show();
		}

		// Check Xposed version
		int xVersion = XUtil.getXposedVersion();
		if (xVersion < cXposedMinVersion) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(getString(R.string.app_name));
			alertDialog.setMessage(String.format(getString(R.string.app_notxposed), cXposedMinVersion));
			alertDialog.setIcon(R.drawable.ic_launcher);
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			alertDialog.show();
		}

		// Check if XPrivacy is enabled
		if (!XUtil.isXposedEnabled()) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(getString(R.string.app_name));
			alertDialog.setMessage(String.format(getString(R.string.app_notenabled), cXposedMinVersion));
			alertDialog.setIcon(R.drawable.ic_launcher);
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			alertDialog.show();
		}
	}

	private class PagerAdapter extends FragmentPagerAdapter {
		private List<Fragment> mListfragment;

		public PagerAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			mListfragment = fragments;
		}

		@Override
		public Fragment getItem(int position) {
			return mListfragment.get(position);
		}

		@Override
		public int getCount() {
			return mListfragment.size();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.xmain, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			switch (item.getItemId()) {
			case R.id.menu_settings:
				optionSettings();
				return true;
			case R.id.menu_export:
				optionExport();
				return true;
			case R.id.menu_import:
				optionImport();
				return true;
			case R.id.menu_report:
				optionReportIssue();
				return true;
			case R.id.menu_about:
				optionAbout();
				return true;
			default:
				return super.onOptionsItemSelected(item);
			}
		} catch (Throwable ex) {
			XUtil.bug(null, ex);
			return true;
		}
	}

	private void optionSettings() {
		// Settings
		Dialog dlgSettings = new Dialog(this);
		dlgSettings.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		dlgSettings.setTitle(getString(R.string.app_name));
		dlgSettings.setContentView(R.layout.xsettings);
		dlgSettings.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);

		// Expert mode
		CheckBox cbSettings = (CheckBox) dlgSettings.findViewById(R.id.cbExpert);
		cbSettings.setChecked(XRestriction.getSetting(null, XFragmentMain.this, XRestriction.cExpertMode));
		cbSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				XRestriction.setSetting(null, XFragmentMain.this, XRestriction.cExpertMode, isChecked);
			}
		});

		dlgSettings.setCancelable(true);
		dlgSettings.show();
	}

	private void optionExport() {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			try {
				// Create file
				String folder = Environment.getExternalStorageDirectory().getAbsolutePath();
				String fileName = folder + File.separator + "XPrivacy.xml";
				FileOutputStream fos = new FileOutputStream(new File(fileName));

				// Create serializer
				XmlSerializer serializer = Xml.newSerializer();
				serializer.setOutput(fos, "UTF-8");
				serializer.startDocument(null, Boolean.valueOf(true));
				serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
				serializer.startTag(null, "XPrivacy");

				// Process restrictions
				Cursor cursor = getContentResolver().query(XPrivacyProvider.URI_RESTRICTION, null, null,
						new String[] { Integer.toString(0), Boolean.toString(false) }, null);
				while (cursor.moveToNext()) {
					// Decode uid
					int uid = cursor.getInt(cursor.getColumnIndex(XPrivacyProvider.COL_UID));
					boolean restricted = Boolean.parseBoolean(cursor.getString(cursor
							.getColumnIndex(XPrivacyProvider.COL_RESTRICTED)));
					if (restricted) {
						String[] packages = getPackageManager().getPackagesForUid(uid);
						if (packages == null)
							XUtil.log(null, Log.WARN, "No packages for uid=" + uid);
						else
							for (String packageName : packages) {
								// Package
								serializer.startTag(null, "Package");

								// Package name
								serializer.attribute(null, "Name", packageName);

								// Restriction name
								String restrictionName = cursor.getString(cursor
										.getColumnIndex(XPrivacyProvider.COL_RESTRICTION));
								serializer.attribute(null, "Restriction", restrictionName);

								serializer.endTag(null, "Package");
							}
					}
				}
				cursor.close();

				// End serialization
				serializer.endTag(null, "XPrivacy");
				serializer.endDocument();
				serializer.flush();
				fos.close();

				// Display file name
				Toast toast = Toast.makeText(this, fileName, Toast.LENGTH_LONG);
				toast.show();
			} catch (Throwable ex) {
				XUtil.bug(null, ex);
				Toast toast = Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG);
				toast.show();
			}
	}

	private void optionImport() {

	}

	private void optionReportIssue() {
		// Report issue
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/M66B/XPrivacy/issues"));
		startActivity(browserIntent);
	}

	private void optionAbout() {
		// About
		Dialog dlgAbout = new Dialog(this);
		dlgAbout.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		dlgAbout.setTitle(getString(R.string.app_name));
		dlgAbout.setContentView(R.layout.xabout);
		dlgAbout.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);

		// Show version
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			TextView tvVersion = (TextView) dlgAbout.findViewById(R.id.tvVersion);
			tvVersion.setText(String.format(getString(R.string.app_version), pInfo.versionName, pInfo.versionCode));
		} catch (Throwable ex) {
			XUtil.bug(null, ex);
		}

		// Show Xposed version
		int xVersion = XUtil.getXposedVersion();
		TextView tvXVersion = (TextView) dlgAbout.findViewById(R.id.tvXVersion);
		tvXVersion.setText(String.format(getString(R.string.app_xversion), xVersion));

		dlgAbout.setCancelable(true);
		dlgAbout.show();
	}
}