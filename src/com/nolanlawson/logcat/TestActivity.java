package com.nolanlawson.logcat;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.nolanlawson.logcat.helper.ServiceHelper;
import com.nolanlawson.logcat.helper.SuperUserHelper;
import com.nolanlawson.logcat.util.UtilLogger;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestActivity extends Activity implements OnClickListener {

	private Button mBtn = null;
	private Button mBtn1= null;
	private boolean mStartRecord = false;
	private static UtilLogger log = new UtilLogger(TestActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CrashHandler crashHandler = CrashHandler.getInstance() ;
        crashHandler.init(this) ;
		setContentView(R.layout.testlayout);
		mBtn = (Button) findViewById(R.id.btn_log);
		mBtn1 = (Button) findViewById(R.id.btn_crash);
		mBtn1.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                int i = 1/0;//除数不能为0 这里会抛出异常  
            }
        });
		mBtn.setOnClickListener(this);
		runUpdatesIfNecessaryAndShowInitialMessage();
	}

	@Override
	public void onClick(View v) {
		Log.i("TestActivity", "onClick");
		if (v.getId() == R.id.btn_log) {
			mStartRecord = !mStartRecord;
			if (mStartRecord) {
				ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(this,
				        createFilename("Log"), "", "V");
				mBtn.setText("Stop Record");
			} else {
				ServiceHelper.stopBackgroundServiceIfRunning(this);
				mBtn.setText("Start Record");
			}
		}
	}

	private static String createFilename(String fileType) {
		Date date = new Date();
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);

		DecimalFormat twoDigitDecimalFormat = new DecimalFormat("00");
		DecimalFormat fourDigitDecimalFormat = new DecimalFormat("0000");

		String year = fourDigitDecimalFormat
				.format(calendar.get(Calendar.YEAR));
		String month = twoDigitDecimalFormat.format(calendar
				.get(Calendar.MONTH) + 1);
		String day = twoDigitDecimalFormat.format(calendar
				.get(Calendar.DAY_OF_MONTH));
		String hour = twoDigitDecimalFormat.format(calendar
				.get(Calendar.HOUR_OF_DAY));
		String minute = twoDigitDecimalFormat.format(calendar
				.get(Calendar.MINUTE));
		String second = twoDigitDecimalFormat.format(calendar
				.get(Calendar.SECOND));

		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(year).append("-").append(month).append("-")
				.append(day).append("-").append(hour).append("-")
				.append(minute).append("-").append(second);

		stringBuilder.append(fileType+".txt");

		return stringBuilder.toString();
	}

//	private LogReaderAsyncTask task;

	/*private void startUpMainLog() {

		Runnable mainLogRunnable = new Runnable() {

			@Override
			public void run() {
				task = new LogReaderAsyncTask();
				task.execute((Void) null);
			}
		};

		if (task != null) {
			// do only after current log is depleted, to avoid splicing the
			// streams together
			// (Don't cross the streams!)
			task.unpause();
			task.setOnFinished(mainLogRunnable);
			task.killReader();
			task = null;
		} else {
			// no main log currently running; just start up the main log now
			mainLogRunnable.run();
		}
	}*/

/*	private class LogReaderAsyncTask extends AsyncTask<Void, LogLine, Void> {

		private volatile boolean paused;
		private final Object lock = new Object();
		private boolean firstLineReceived;
		private boolean killed;
		private LogcatReader reader;
		private Runnable onFinished;

		@Override
		protected Void doInBackground(Void... params) {

			try {
				// use "recordingMode" because we want to load all the existing
				// lines at once
				// for a performance boost
				LogcatReaderLoader loader = LogcatReaderLoader.create(
						TestActivity.this, true);
				reader = loader.loadReader();

				int maxLines = PreferenceHelper
						.getDisplayLimitPreference(TestActivity.this);

				String line;
				LinkedList<LogLine> initialLines = new LinkedList<LogLine>();
				while ((line = reader.readLine()) != null) {
					if (paused) {
						synchronized (lock) {
							if (paused) {
								lock.wait();
							}
						}
					}
					LogLine logLine = LogLine.newLogLine(line, false);
					if (!reader.readyToRecord()) {
						// "ready to record" in this case means all the initial
						// lines have been flushed from the reader
						initialLines.add(logLine);
						if (initialLines.size() > maxLines) {
							initialLines.removeFirst();
						}
					} else if (!initialLines.isEmpty()) {
						// flush all the initial lines we've loaded
						initialLines.add(logLine);
						publishProgress(ArrayUtil.toArray(initialLines,
								LogLine.class));
						initialLines.clear();
					} else {
						// just proceed as normal
						publishProgress(logLine);
					}
				}
			} catch (InterruptedException e) {
				log.d(e, "expected error");
			} catch (Exception e) {
				log.d(e, "unexpected error");
			} finally {
				killReader();
				log.d("AsyncTask has died");
			}

			return null;
		}

		public void killReader() {
			if (!killed) {
				synchronized (lock) {
					if (!killed && reader != null) {
						reader.killQuietly();
						killed = true;
					}
				}
			}

		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			log.d("onPostExecute()");
			doWhenFinished();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			log.d("onPreExecute()");
		}

		@Override
		protected void onProgressUpdate(LogLine... values) {
			super.onProgressUpdate(values);

			if (!firstLineReceived) {
				firstLineReceived = true;
			}

		}

		private void doWhenFinished() {
			if (paused) {
				unpause();
			}
			if (onFinished != null) {
				onFinished.run();
			}
		}

		public void pause() {
			synchronized (lock) {
				paused = true;
			}
		}

		public void unpause() {
			synchronized (lock) {
				paused = false;
				lock.notify();
			}
		}

		public boolean isPaused() {
			return paused;
		}

		public void setOnFinished(Runnable onFinished) {
			this.onFinished = onFinished;
		}

	}*/

	private void runUpdatesIfNecessaryAndShowInitialMessage() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				SuperUserHelper.requestRoot(TestActivity.this);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
//				startUpMainLog();
			}

		}.execute((Void) null);
	}
}
