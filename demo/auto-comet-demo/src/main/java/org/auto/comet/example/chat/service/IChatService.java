package org.auto.comet.example.chat.service;

import java.io.Serializable;

/**
 * IChatService
 *
 * @author XiaohangHu
 */
public interface IChatService {

	public void sendMessage(String userId, String targetUserId, String message);

	public void sendMessage(Serializable[] userIds, String message);

	void login(Serializable userId);

	void exit(Serializable userId);

}
