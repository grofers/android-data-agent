package com.grofers.library;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;



/**
 * Created by prateek.dixit on 17/07/16.
 */
class DBManager {
	private static final String TAG = "dbmanager";

	private static final String DATABASE_NAME = "batch.db";
	private static final String CACHE_TABLE = "cache";
	private OpenHelper openHelper;

	private String COL_NAME_EVENTID = "eventId";
	private String COL_NAME_GROUPID = "groupId";
	private String COL_NAME_DATA = "data";
	private String COL_NAME_EXPIRY = "expiry";

	private SQLiteStatement insertStmt;

	// private SQLiteStatement insertStmt;
	// private SQLiteStatement insertStmt;
	public DBManager(Context context) {
		openHelper = new OpenHelper(context);
		String strInsert = "insert into " + CACHE_TABLE + " ("
				+ COL_NAME_EVENTID + ", " + COL_NAME_GROUPID + ", "
				+ COL_NAME_DATA + ", " + COL_NAME_EXPIRY
				+ ") values (?, ?, ?, ?)";
		insertStmt = openHelper.getDatabase().compileStatement(strInsert);
	}

	public void loadCachedDataInBatchNetworkingInstance(
			BatchNetworking batchNetworking) {

		if (null == batchNetworking) {
			return;
		}

		Cursor cursor = openHelper.getDatabase().query(
				CACHE_TABLE,
				new String[] { COL_NAME_EVENTID, COL_NAME_GROUPID,
						COL_NAME_DATA, COL_NAME_EXPIRY }, null, null, null,
				null, COL_NAME_EVENTID);


		if (cursor.moveToFirst()) {
			do {
				try {
					String groupId = cursor.getString(1);
					Group group = batchNetworking.getGroupPriorityQueue()
							.getGroupForGroupId(groupId);
					if (null == group) {
						continue;
					}

					Data data = new Data();
					data.setEventId(cursor.getLong(0));
					data.setData(group.getBatchDataHandler()
							.deSerializeIndividualData(cursor.getBlob(2)));
					data.setCacheState(Data.DataCacheState.CSTATE_CACHED);
					group.push(data);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
		} else {
			Log.i(TAG,
					"In loadCachedDataInBatchNetworkingInstance: No data to load");
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	public boolean dataExists(long eventId) {
		Cursor cursor = openHelper.getDatabase().query(CACHE_TABLE,
				new String[] { COL_NAME_EVENTID }, COL_NAME_EVENTID + "=?",
				new String[] { "" + eventId }, null, null, null);
		boolean exists = cursor.moveToFirst();
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return exists;
	}

	public void emptyThisTable() {
		this.openHelper.getDatabase().delete(CACHE_TABLE, null, null);
	}

	public void persistData(long eventId, String groupId, byte[] data,
			long expiry) {
		try {
			insertStmt.bindLong(1, eventId);
			insertStmt.bindString(2, groupId);
			insertStmt.bindBlob(3, data);
			insertStmt.bindLong(4, expiry);
			this.insertStmt.executeInsert();
		} catch (Exception e) {
			Log.e(TAG, "Exception in persistBatchDatum " + e);
		}
	}

	public void removeData(long eventId) {
		openHelper.getDatabase().delete(CACHE_TABLE, COL_NAME_EVENTID + "=?",
				new String[] { "" + eventId });
	}

	class OpenHelper extends SQLiteOpenHelper {
		public static final int DB_VERSION = 1;
		private SQLiteDatabase database = null;

		public SQLiteDatabase getDatabase() {
			return database;
		}

		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DB_VERSION);
			database = getWritableDatabase();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + CACHE_TABLE + " ("
					+ COL_NAME_EVENTID + " INTEGER primary key, "
					+ COL_NAME_GROUPID + " TEXT, " + COL_NAME_DATA + " BLOB, "
					+ COL_NAME_EXPIRY + " INTEGER)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + CACHE_TABLE);
			db.execSQL("CREATE TABLE IF NOT EXISTS " + CACHE_TABLE + " ("
					+ COL_NAME_EVENTID + " INTEGER primary key, "
					+ COL_NAME_GROUPID + " TEXT, " + COL_NAME_DATA + " BLOB, "
					+ COL_NAME_EXPIRY + " INTEGER)");
		}
	}
}
