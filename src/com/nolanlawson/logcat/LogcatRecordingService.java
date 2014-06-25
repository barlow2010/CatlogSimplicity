package com.nolanlawson.logcat;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Random;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nolanlawson.logcat.helper.SaveLogHelper;
import com.nolanlawson.logcat.reader.LogcatReader;
import com.nolanlawson.logcat.reader.LogcatReaderLoader;
import com.nolanlawson.logcat.util.UtilLogger;

/**
 * Reads logs.
 * 
 */
public class LogcatRecordingService extends IntentService {

	private static final String ACTION_STOP_RECORDING = "com.nolanlawson.catlog.action.STOP_RECORDING";
	public static final String URI_SCHEME = "catlog_recording_service";

	public static final String EXTRA_FILENAME = "filename";
	public static final String EXTRA_LOADER = "loader";
	public static String filename;
	
	public static String getFilename() {
        return filename;
    }

    public static void setFilename(String filename) {
        LogcatRecordingService.filename = filename;
    }
    private static UtilLogger log = new UtilLogger(LogcatRecordingService.class);

	private LogcatReader reader;

	private boolean killed;
	private final Object lock = new Object();
	
	
	private Handler handler;


	public LogcatRecordingService() {
		super("AppTrackerService");
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		log.d("onCreate()");
		
	
		
		handler = new Handler(Looper.getMainLooper());

	}


	private void initializeReader(Intent intent) {
		try {
			// use the "time" log so we can see what time the logs were logged at
			LogcatReaderLoader loader = intent.getParcelableExtra(EXTRA_LOADER);
			reader = loader.loadReader();
		
			while (!reader.readyToRecord() && !killed) {
				reader.readLine();
				// keep skipping lines until we find one that is past the last log line, i.e.
				// it's ready to record
			}			
			if (!killed) {
				makeToast(R.string.log_recording_started, Toast.LENGTH_SHORT);
			}
		} catch (IOException e) {
			log.d(e, "");
		}
		
	}


	@Override
	public void onDestroy() {
		log.d("onDestroy()");
		super.onDestroy();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				killProcess();
				stopLog = true;
			}
		}, 1000);
	}

    // This is the old onStart method that will be called on the pre-2.0
    // platform.
    @Override
    public void onStart(Intent intent, int startId) {
    	log.d("onStart()");
    	super.onStart(intent, startId);
        handleCommand(intent);
        stopLog = false;
        logThread.start();
    }
    private boolean stopLog = false;
    private Thread logThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while(!stopLog) {
				log.i("logThread");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});
	private void handleCommand(Intent intent) {
        Intent stopRecordingIntent = new Intent();
        stopRecordingIntent.setAction(ACTION_STOP_RECORDING);
        // have to make this unique for God knows what reason
        stopRecordingIntent.setData(Uri.withAppendedPath(Uri.parse(URI_SCHEME + "://stop/"), 
        		Long.toHexString(new Random().nextLong())));
	}



	protected void onHandleIntent(Intent intent) {
		log.d("onHandleIntent()");
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		
		log.d("Starting up %s now with intent: %s", LogcatRecordingService.class.getSimpleName(), intent);
		
		filename = intent.getStringExtra(EXTRA_FILENAME);

		SaveLogHelper.deleteLogIfExists(filename);
		
		initializeReader(intent);
		
		StringBuilder stringBuilder = new StringBuilder();
		
		try {
			
			String line;
			int lineCount = 0;
			int logLinePeriod = getLogLinePeriodPreference(getApplicationContext());
			while ((line = reader.readLine()) != null && !killed) {
				stringBuilder.append(line).append("\n");
				
				if (++lineCount % logLinePeriod == 0) {
					// avoid OutOfMemoryErrors; flush now
					SaveLogHelper.saveLog(stringBuilder, filename);
					stringBuilder.delete(0, stringBuilder.length()); // clear
				}
			}
			
			
			
		} catch (IOException e) {
			log.e(e, "unexpected exception");
		} finally {
			killProcess();
			log.d("CatlogService ended");
			
			boolean logSaved = SaveLogHelper.saveLog(stringBuilder, filename);
			
			if (logSaved) {
				makeToast(R.string.log_saved, Toast.LENGTH_SHORT);
//				startLogcatActivityToViewSavedFile(filename);
			} else {
				makeToast(R.string.unable_to_save_log, Toast.LENGTH_LONG);
			}
		}
	}

	public static int getLogLinePeriodPreference(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        String defaultValue = context.getText(R.string.pref_log_line_period_default).toString();
        
        String intAsString = sharedPrefs.getString(context.getText(R.string.pref_log_line_period).toString(), defaultValue);
        
        try { 
            return Integer.parseInt(intAsString);
        } catch (NumberFormatException e) {
            return Integer.parseInt(defaultValue);
        }
    }
	
	
	private void makeToast(final int stringResId, final int toastLength) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				
				Toast.makeText(LogcatRecordingService.this, stringResId, toastLength).show();
				
			}
		});
		
	}
	
	private void killProcess() {
		if (!killed) {
			synchronized (lock) {
				if (!killed && reader != null) {
					// kill the logcat process
					reader.killQuietly();
					killed = true;
				}
			}
		}
	}
	
}
