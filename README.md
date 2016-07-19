# android-data-agent
Push data in batches and groups.

Group:
      Represent a set of same type of data.
      
      
GroupHandler:
      Handler for a group. Fetches next batch of data from the group, Syncs it with the server, persists to the local db, delete the synced batch from local.
       
GroupSynsPolicy:
      Defines various sync policy like sync time, batch size etc.
