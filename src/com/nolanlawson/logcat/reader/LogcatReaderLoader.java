package com.nolanlawson.logcat.reader;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import com.nolanlawson.logcat.R;
import com.nolanlawson.logcat.helper.LogcatHelper;

public class LogcatReaderLoader implements Parcelable {

	private Map<String,String> lastLines = new HashMap<String, String>();
	private boolean recordingMode;
	private boolean multiple;
	public static final String DELIMITER = ",";
	
	
	private LogcatReaderLoader(Parcel in) {
		this.recordingMode = in.readInt() == 1;
		this.multiple = in.readInt() == 1;
		Bundle bundle = in.readBundle();
		for (String key : bundle.keySet()) {
			lastLines.put(key, bundle.getString(key));
		}
	}
	
	private LogcatReaderLoader(List<String> buffers, boolean recordingMode) {
		this.recordingMode = recordingMode;
		this.multiple = buffers.size() > 1;
		for (String buffer : buffers) {
			// no need to grab the last line if this isn't recording mode
			String lastLine = recordingMode ? LogcatHelper.getLastLogLine(buffer) : null;
			lastLines.put(buffer, lastLine);
		}
	}
	
	public LogcatReader loadReader() throws IOException {
		LogcatReader reader;
		if (!multiple) {
			// single reader
			String buffer = lastLines.keySet().iterator().next();
			String lastLine = lastLines.values().iterator().next();
			reader = new SingleLogcatReader(recordingMode, buffer, lastLine);
		} else {
			// multiple reader
			reader = new MultipleLogcatReader(recordingMode, lastLines);
		}
		
		return reader;
	}
	
	public static LogcatReaderLoader create(Context context, boolean recordingMode) {
		List<String> buffers = getBuffers(context);
		LogcatReaderLoader loader = new LogcatReaderLoader(buffers, recordingMode);
		return loader;
	}
	
	public static List<String> getBuffers(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        String defaultValue = context.getString(R.string.pref_buffer_choice_main_value);
        String key = context.getString(R.string.pref_buffer);
        
        String value = sharedPrefs.getString(key, defaultValue);
        
        return Arrays.asList(split(value, DELIMITER));
    }
	
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(recordingMode ? 1 : 0);
		dest.writeInt(multiple ? 1 : 0);
		Bundle bundle = new Bundle();
		for (Entry<String,String> entry : lastLines.entrySet()) {
			bundle.putString(entry.getKey(), entry.getValue());
		}
		dest.writeBundle(bundle);
	}
	
	public static final Parcelable.Creator<LogcatReaderLoader> CREATOR = new Parcelable.Creator<LogcatReaderLoader>() {
		public LogcatReaderLoader createFromParcel(Parcel in) {
			return new LogcatReaderLoader(in);
		}

		public LogcatReaderLoader[] newArray(int size) {
			return new LogcatReaderLoader[size];
		}
	};
	
	/**
     * same as the String.split(), except it doesn't use regexes, so it's faster.
     *
     * @param str       - the string to split up
     * @param delimiter the delimiter
     * @return the split string
     */
    public static String[] split(String str, String delimiter) {
        List<String> result = new ArrayList<String>();
        int lastIndex = 0;
        int index = str.indexOf(delimiter);
        while (index != -1) {
            result.add(str.substring(lastIndex, index));
            lastIndex = index + delimiter.length();
            index = str.indexOf(delimiter, index + delimiter.length());
        }
        result.add(str.substring(lastIndex, str.length()));

        return toArray(result, String.class);
    }
    
    public static <T> T[] toArray(List<T> list, Class<T> clazz) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(clazz, list.size());
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
