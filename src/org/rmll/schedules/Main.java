package org.rmll.schedules;

import java.util.Date;

import org.rmll.R;
import org.rmll.broadcast.FavoritesBroadcast;
import org.rmll.db.DBAdapter;
import org.rmll.listeners.ParserEventListener;
import org.rmll.util.StringUtil;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements ParserEventListener,
		OnClickListener {
	public static final String LOG_TAG = Main.class.getName();

	public static final int STARTFETCHING = -1;
	public static final int DONEFETCHING = 0;
	public static final int TAGEVENT = 1;
	public static final int DONELOADINGDB = 2;
	public static final int ROOMIMGSTART = 3;
	public static final int ROOMIMGDONE = 4;
	public static final int LOAD_BG_START = 5;
	public static final int LOAD_BG_END = 6;

	public static final String PREFS = "org.rmll";
	public static final String XML_URL = "https://2015.rmll.info/schedule/xml";
	public static final String ROOM_IMG_URL_BASE = "https://2015.rmll.info/schedule/room/";

	public int counter = 0;
	protected TextView tvProgress = null, tvDbVer = null;
	protected Button btnDay1, btnDay2, btnDay3, btnDay4, btnDay5, btnDay6, btnDay7;

	@SuppressWarnings("unused")
	private BroadcastReceiver favoritesChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getIntExtra(FavoritesBroadcast.EXTRA_TYPE,-1)
							!=FavoritesBroadcast.EXTRA_TYPE_INSERT && intent
							.getIntExtra(FavoritesBroadcast.EXTRA_TYPE,-1)!=FavoritesBroadcast.EXTRA_TYPE_DELETE)
				return;
			long count = intent.getLongExtra(FavoritesBroadcast.EXTRA_COUNT, -1);
			Log.v(getClass().getName(), "FavoritesBroadcast received! "  + count);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String queryAction = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			EventListActivity.doSearchWithIntent(this, intent);
			finish();
		}
		if (Intent.ACTION_VIEW.equals(queryAction)) {
			Intent i = new Intent(this, DisplayEvent.class);
			i.putExtra(DisplayEvent.ID, Integer
					.parseInt(intent.getDataString()));
			startActivity(i);
			finish();
		}
		
		Intent initialLoadIntent = new Intent(FavoritesBroadcast.ACTION_FAVORITES_INITIAL_LOAD);
		sendBroadcast(initialLoadIntent);

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.show();
		}

		setContentView(R.layout.main);

		btnDay1 = (Button) findViewById(R.id.btn_day_1);
		btnDay1.setOnClickListener(this);
		btnDay2 = (Button) findViewById(R.id.btn_day_2);
		btnDay2.setOnClickListener(this);
		btnDay3 = (Button) findViewById(R.id.btn_day_3);
		btnDay3.setOnClickListener(this);
		btnDay4 = (Button) findViewById(R.id.btn_day_4);
		btnDay4.setOnClickListener(this);
		btnDay5 = (Button) findViewById(R.id.btn_day_5);
		btnDay5.setOnClickListener(this);
		btnDay6 = (Button) findViewById(R.id.btn_day_6);
		btnDay6.setOnClickListener(this);
		btnDay7 = (Button) findViewById(R.id.btn_day_7);
		btnDay7.setOnClickListener(this);

		tvProgress = (TextView) findViewById(R.id.progress);
		tvDbVer = (TextView) findViewById(R.id.db_ver);

		// FIXME on first startup
		// - propose user to update database
	}

	@Override
	protected void onResume() {
		super.onResume();
		tvDbVer.setText(getString(R.string.schedule_last_updated, StringUtil.dateTimeToString(getDBLastUpdated())));

		DBAdapter dbAdapter = new DBAdapter(this);
		long count = 0;
		try {
			dbAdapter.open();
			count = dbAdapter.getEventCount();
			btnDay1.setEnabled(count > 0);
			btnDay2.setEnabled(count > 0);
			btnDay3.setEnabled(count > 0);
			btnDay4.setEnabled(count > 0);
			btnDay5.setEnabled(count > 0);
			btnDay6.setEnabled(count > 0);
			btnDay7.setEnabled(count > 0);
		} finally {
			dbAdapter.close();
		}

		if (count < 1) {
			createUpdateDialog();
		}

		// FIXME on first startup
		// - propose user to update database
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_actions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_search:
				onSearchRequested();
				break;
			case R.id.action_refresh:
				createUpdateDialog();
				break;
			case R.id.action_favorites:
				showFavorites();
				break;
			case R.id.action_settings:
				showSettings();
				break;
			case R.id.action_about:
				createAboutDialog();
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * @return
	 */
	private Dialog createAboutDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final View view = getLayoutInflater().inflate(R.layout.about, null,
				false);
		builder.setTitle(getString(R.string.app_name));
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setView(view);
		builder.setPositiveButton(getString(android.R.string.ok), null);
		builder.setCancelable(true);
		return builder.show();
	}

	/**
	 * @return
	 */
	private Dialog createUpdateDialog() {

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(getString(R.string.updater_title));

		final boolean[] selection = { true, true };
		builder.setMultiChoiceItems(R.array.updater_dialog_choices, selection,
				new OnMultiChoiceClickListener() {

					public void onClick(DialogInterface dialog, int which,
							boolean isChecked) {
						selection[which] = isChecked;
					}
				});

		builder.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						// if none selected, skip
						if (!(selection[0] || selection[1]))
							return;

						final Thread t = new Thread(new BackgroundUpdater(
								handler, Main.this, getApplicationContext(),
								selection[0], selection[1]));
						t.start();
					}

				});

		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setCancelable(true);

		return builder.show();
	}

	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.btn_day_1:
			showTracksForDay(1);
			break;
		case R.id.btn_day_2:
			showTracksForDay(2);
			break;
		case R.id.btn_day_3:
			showTracksForDay(3);
			break;
		case R.id.btn_day_4:
			showTracksForDay(4);
			break;
		case R.id.btn_day_5:
			showTracksForDay(5);
			break;
		case R.id.btn_day_6:
			showTracksForDay(6);
			break;
		case R.id.btn_day_7:
			showTracksForDay(7);
			break;
		default:
			Log.e(LOG_TAG,
					"Received a button click, but I don't know from where.");
			break;
		}
	}

	public void toast(String message) {
		final Context context = getApplicationContext();
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
		toast.show();
	}

	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg == null)
				return;
			switch (msg.what) {
			case TAGEVENT:
				Main.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
				tvProgress.setText(getString(R.string.fetched_talks, counter));
				break;
			case STARTFETCHING:
				tvProgress.setText(getString(R.string.downloading_talks));
				break;
			case DBAdapter.MSG_EVENT_STORED:
				tvProgress.setText(getString(R.string.stored_talks, msg.arg1));
				break;
			case DONEFETCHING:
				tvProgress.setText(getString(R.string.done_fetching_talks));
				setDBLastUpdated();
				break;
			case DONELOADINGDB:
				final String doneDb = getString(R.string.done_storing_talks);
				tvProgress.setText("");
				toast(doneDb);
				tvDbVer.setText(getString(R.string.schedule_last_updated, StringUtil.dateTimeToString(getDBLastUpdated())));
				DBAdapter db = new DBAdapter(Main.this);
				db.open();
				try {
					long count = db.getEventCount();
					btnDay1.setEnabled(count > 0);
					btnDay2.setEnabled(count > 0);
					btnDay3.setEnabled(count > 0);
					btnDay4.setEnabled(count > 0);
					btnDay5.setEnabled(count > 0);
					btnDay6.setEnabled(count > 0);
					btnDay7.setEnabled(count > 0);
				} finally {
					db.close();
				}
				break;
			case ROOMIMGSTART:
				tvProgress.setText(getString(R.string.downloading_rooms));
				break;
			case ROOMIMGDONE:
				final String doneRooms = getString(R.string.done_downloading_rooms);
				tvProgress.setText("");
				toast(doneRooms);
				break;
			/*case LOAD_BG_START:
				Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay(); 
				Main.this.setRequestedOrientation(display.getOrientation());
				break;
			case LOAD_BG_END:
				Main.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				break;*/
			}
		}
	};

	public void onTagEvent(String tag, int type) {
		if (tag.equals("event") && type == ParserEventListener.TAG_OPEN) {
			counter++;
			final Message msg = Message.obtain();
			msg.what = TAGEVENT;
			handler.sendMessage(msg);
		}
	}

	public void showTracksForDay(int day) {
		Log.d(LOG_TAG, "showTracksForDay(" + day + ");");
		Intent i = new Intent(this, TrackListActivity.class);
		i.putExtra(TrackListActivity.DAY_INDEX, day);
		startActivity(i);
	}

	public void showFavorites() {
		Intent i = new Intent(this, EventListActivity.class);
		i.putExtra(EventListActivity.FAVORITES, true);
		startActivity(i);
	}

	/**
	 * Set NOW as the time that the Schedule database has been imported.
	 */
	private void setDBLastUpdated() {
		SharedPreferences.Editor editor = getSharedPreferences(Main.PREFS, 0)
				.edit();
		long timestamp = System.currentTimeMillis() / 1000;
		editor.putLong("db_last_updated", timestamp);
		editor.commit(); // Don't forget to commit your edits!!!
	}

	/**
	 * Fetch the Date when the Schedule database has been imported
	 * 
	 * @return Date of the last Database update
	 */
	private Date getDBLastUpdated() {
		SharedPreferences settings = getSharedPreferences(Main.PREFS, 0);
		long timestamp = settings.getLong("db_last_updated", 0);
		if (timestamp == 0)
			return null;
		return new Date(timestamp * 1000);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void showSettings() {
		Intent i = new Intent(this, Preferences.class);
		startActivity(i);
	}
}
