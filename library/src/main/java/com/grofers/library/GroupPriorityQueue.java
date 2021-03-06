package com.grofers.library;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by prateek.dixit on 16/07/16.
 */
public class GroupPriorityQueue {

	private static final String TAG = "GroupPriorityQueue";

	public static final int NOTIFICATION_POKE_ME = 0;
	public static final int NOTIFICATION_SYNC_SUCCESSFUL = 1;
	public static final int NOTIFICATION_SYNC_FAILED = -1;

	private ArrayList<Group> _queue;
	private HashMap<String, Group> _groupMap;

	private Handler notificationHandler = null;

	public Handler getNotificationHandler() {
		if (null == notificationHandler) {
			// initialize notification handler
			HandlerThread thread = new HandlerThread("Notification handler");
			thread.start();
			notificationHandler = new Handler(thread.getLooper()) {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case NOTIFICATION_SYNC_SUCCESSFUL:
					case NOTIFICATION_POKE_ME:
						this.removeMessages(NOTIFICATION_POKE_ME);
						pokeDataBatchesForSyncing();
						break;
					case NOTIFICATION_SYNC_FAILED:
					default:
						break;
					}
				}
			};
		}
		return notificationHandler;
	}

	public GroupPriorityQueue() {
		_queue = new ArrayList<Group>();
		_groupMap = new HashMap<String, Group>();
	}

	/**
	 * Read information
	 */
	public boolean isEmpty() {
		return _queue.size() == 0;
	}

	public int size() {
		return _queue.size();
	}

	public Group getGroupForGroupId(String groupIdentifier) {
		return _groupMap.get(groupIdentifier);
	}

	public boolean addGroup(Group group, String groupIdentifier) {


		// remove if old object exists
		if (getGroupForGroupId(groupIdentifier) != null) {
			return false;
		}

		// add object to map
		_groupMap.put(groupIdentifier, group);

		// add object to queue
		if (_queue.size() > 0) {

			int priority = group.getBatchDataHandler().getPriority();

			// use binary search.
			int min = 0;
			int max = _queue.size() - 1;
			int mid = max;

			while (min < max) {
				mid = (min + max) / 2;

				if (priority <= _queue.get(mid).getBatchDataHandler()
						.getPriority()) {
					max = mid;
				} else {
					if (min == mid) {
						if (priority > _queue.get(max).getBatchDataHandler()
								.getPriority()) {
							mid = max + 1;
						} else
							mid = max;
						break;
					} else {
						min = mid;
					}
				}
			}
			_queue.add(mid, group);
		} else
			_queue.add(group);
		return true;
	}

	private void pokeDataBatchesForSyncing() {
		ConnectivityManager cm = (ConnectivityManager) BatchNetworking
				.getDefaultInstance().getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null
				&& activeNetwork.isConnectedOrConnecting();
		if (!isConnected) {
			return;
		}

		for (Group dataTypeBatch : _queue) {
			if (dataTypeBatch.handleSyncPoke()) {
				return;
			}
		}
	}
}
