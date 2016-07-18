package com.grofers.library;


/**
 * Created by prateek.dixit on 16/07/16.
 */
public interface GroupSyncPolicy {

	public boolean elegibleForSyncing(Group group);
	public int syncBatchSize();
	public int syncIdleTime();
	
}
