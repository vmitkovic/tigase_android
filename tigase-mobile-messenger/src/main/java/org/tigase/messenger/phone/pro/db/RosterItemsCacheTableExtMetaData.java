package org.tigase.messenger.phone.pro.db;

public class RosterItemsCacheTableExtMetaData {

    /**
     * Additional field to keep status of buddy from roster item
     * to speed up searches of online users
     * <ul>
     * <li><code>0</code> - unavailable</li>
     * <li><code>1</code> - error</li>
     * <li><code>5</code> - dnd</li>
     * <li><code>10</code> - xa</li>
     * <li><code>15</code> - away</li>
     * <li><code>20</code> - available</li>
     * <li><code>25</code> - chat</li>
     * </ul>
     */
	public static final String FIELD_STATUS = "status";
	
}
