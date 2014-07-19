package org.tigase.messenger.phone.pro.ui;

import org.tigase.messenger.phone.pro.R;

public class Layouts {

	public static class ChatLayout {
		public final int message;
		public final int locality;
		public final int file;
		public final int image;
		public final int video;
		
		ChatLayout(int message, int locality, int file, int image, int video) {
			this.message = message;
			this.locality = locality;
			this.file = file;
			this.image = image;
			this.video = video;
		}
	}
	
	public static final ChatLayout CHAT_BUBBLE = new ChatLayout(R.layout.chat_item_his, 
			R.layout.chat_item_map, R.layout.chat_item_file, R.layout.chat_item_image, 
			R.layout.chat_item_video); 

	public static final ChatLayout CHAT_SIMPLE_HIS = new ChatLayout(R.layout.chat_item_simple_his, 
			R.layout.chat_item_simple_locality_his, R.layout.chat_item_simple_file_his, R.layout.chat_item_simple_image_his, 
			R.layout.chat_item_simple_video_his); 

	public static final ChatLayout CHAT_SIMPLE_MINE = new ChatLayout(R.layout.chat_item_simple_mine, 
			R.layout.chat_item_simple_locality_mine, R.layout.chat_item_simple_file_mine, R.layout.chat_item_simple_image_mine, 
			R.layout.chat_item_simple_video_mine); 
}
