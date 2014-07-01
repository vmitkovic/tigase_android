package org.tigase.messenger.phone.pro.db;

public class ChatTableMetaData {

    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.chatitem";

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.chat";

    public static final String FIELD_ACCOUNT = "account";

    public static final String FIELD_AUTHOR_JID = "author_jid";

    public static final String FIELD_AUTHOR_NICKNAME = "author_nickname";

    public static final String FIELD_BODY = "body";
    
    public static final String FIELD_DATA = "data";

    public static final String FIELD_ID = "_id";

    public static final String FIELD_JID = "jid";

    /**
     * Description of values for FIELD_STATE (state)
     * <ul>
     * <li><code>0</code> - incoming message</li>
     * <li><code>1</code> - outgoing, not sent</li>
     * <li><code>2</code> - outgoing, sent</li>
     * <li><code>3</code> - incoming unread</li>
     * <li><code>4</code> - incoming locality</li>
     * <li><code>5</code> - outgoing locality</li>
     * </ul>
     */
    public static final String FIELD_STATE = "state";

    public static final String FIELD_THREAD_ID = "thread_id";

    public static final String FIELD_TIMESTAMP = "timestamp";

    /**
     * Description of values for FIELD_ITEM_TYPE (item_type)
     * <ul>
     * <li><code>0</code> - message</li>
     * <li><code>1</code> - locality</li>
     * </ul>
     */
    public static final String FIELD_ITEM_TYPE = "item_type";
    
    public static final String INDEX_JID = "chat_history_jid_index";

    public final static int ITEM_TYPE_MESSAGE = 0;
    
    public final static int ITEM_TYPE_LOCALITY = 1;
    
    public final static int STATE_INCOMING = 0;

    public final static int STATE_INCOMING_LOCALITY = 4;
    
    public final static int STATE_INCOMING_UNREAD = 3;

    public final static int STATE_OUT_LOCALITY = 5;
    
    public final static int STATE_OUT_NOT_SENT = 1;

    public final static int STATE_OUT_SENT = 2;

    public static final String TABLE_NAME = "chat_history";	
	
}
