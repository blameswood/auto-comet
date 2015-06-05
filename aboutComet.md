web.xml配置：
```
	<servlet>
		<servlet-name>cometDispatcher</servlet-name>

		<!--comet分发器 -->
		<servlet-class>org.auto.comet.web.SocketDispatcherServlet</servlet-class>

		<init-param>
			<!--comet服务配置文件路径，默认路径为 "/WEB-INF/dispatcher.comet.xml" -->
			<param-name>dispatcherConfigLocation</param-name>
			<param-value>/WEB-INF/dispatcher.comet.xml</param-value>
		</init-param>

		<!--让comet服务在servlet容器启动时初始化，可以尽早发现配置错误 -->
		<load-on-startup>2</load-on-startup>

		<!--必须将DispatcherServlet配置为异步servlet -->
		<async-supported>true</async-supported>
	</servlet>
	
	<servlet-mapping>
		<servlet-name>cometDispatcher</servlet-name>
		<url-pattern>*.comet</url-pattern>
	</servlet-mapping>
```

dispatcherConfigLocation支持逗号分隔的匹配格式路径,例如：

"/WEB-INF//**.comet.xml,classpath:/****/**.comet.xml,file:D:/comet/dispatcher.comet.xml"

"classpath:"代表路径协议为classpath。classpath协议路径，会从classpath中查找对应资源。
"file":代表路径协议为文件系统。
而默认的路径协议为"webroot"。所以"/WEB-INF//**.comet.xml"并不用写为"webroot:/WEB-INF/****/**.comet.xml"。

目前只支持上述三种路径协议。

dispatcher.comet.xml配置

```
<?xml version="1.0" encoding="UTF-8"?>
<auto-comet xmlns="http://www.auto.org/schema/comet"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 

xmlns:spring="http://www.auto.org/schema/comet/spring"
	xsi:schemaLocation="http://www.auto.org/schema/comet http://www.auto.org/schema/comet/auto-comet-

1.0.xsd
	http://www.auto.org/schema/comet/spring http://www.auto.org/schema/comet/auto-comet-spring-

1.0.xsd">


	<!--配置一个comet服务，你也可以将它理解为一个通道。 -->
	<!--request:请求uri -->
	<!--handler:socket处理器，socket处理器必须实现org.auto.comet.SocketHandler接口 -->
	<comet request="/chatRoom.comet"
		handler="org.auto.comet.example.chat.service.ChatRoomSocketHandler" />

</auto-comet>
```

与spring整合的dispatcher.spring.comet.xml配置

```
<?xml version="1.0" encoding="UTF-8"?>
<auto-comet xmlns="http://www.auto.org/schema/comet"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 

xmlns:spring="http://www.auto.org/schema/comet/spring"
	xsi:schemaLocation="http://www.auto.org/schema/comet http://www.auto.org/schema/comet/auto-comet-

1.0.xsd
	http://www.auto.org/schema/comet/spring http://www.auto.org/schema/comet/auto-comet-spring-

1.0.xsd">

	<!--autoComet可以用指定objectFactory获取bean -->
	<!--指定为objectFactory为org.auto.comet.spring.ObjectFactory，则comet会从spring容器中获取bean -->
	<property name="objectFactory" value="org.auto.comet.spring.ObjectFactory" />

	<!--配置一个comet服务，你也可以将它理解为一个通道。 -->
	<!--request:请求uri -->
	<!--handler:socket处理器，socket处理器必须实现org.auto.comet.SocketHandler接口 -->
	<!--与spring整合后handler的配置为bean的名字 -->
	<comet request="/chat.comet" handler="chatService" />
	<comet request="/chatRoom.comet" handler="chatRoomSocketHandler" />

</auto-comet>
```

**SocketHandler接口**

SocketHandler中申明了2个方法。
接受连接请求方法：boolean accept(Socket socket, HttpServletRequest request);
断开连接请求方法：void quit(Socket socket, HttpServletRequest request);


接受连接请求方法，可以处理一个客户端(浏览器)发送的连接请求。处理器可以通过传入的socket完成给客户端发送消息

等操作。可以从request中获取，客户端连接请求时发送的参数。返回值表示是否愿意接受该连接请求。

断开接口与接受接口类似。

一个典型的SocketHandler接口的实现如下：

```
public class ChatRoomSocketHandler implements SocketHandler {


	private Map<Serializable, Socket> socketMapping = new HashMap<Serializable, Socket>();

	public boolean accept(Socket socket, HttpServletRequest request) {
		String userId = request.getParameter("userId");

		if (socketMapping.get(userId) != null) {
			return false;
		}
		socket.setErrorHandler(new ErrorHandler() {
			@Override
			public void error(Socket socket, PushException e) {
				romveSocket(socket);
				throw e;
			}
		});
		socketMapping.put(userId, socket);
		return true;
		// return false;
	}

	public void quit(Socket socket, HttpServletRequest request) {
		String userId = request.getParameter("userId");
		socketMapping.remove(userId);
		this.chatRoomService.loginOut(userId);
	}

	private void romveSocket(Socket socket) {
		for (Entry<Serializable, Socket> entry : socketMapping.entrySet()) {
			Socket value = entry.getValue();
			if (value.equals(socket)) {
				Serializable key = entry.getKey();
				socketMapping.remove(key);
			}
		}
	}

	//...
}
```


可以通过Socket接口给客户端推送消息。Socket接口提供了如下的方法：
```
	/**
	 * 发送消息
	 *
	 */
	void sendMessage(String message);

	/**
	 * 关闭连接
	 *
	 */
	void close();

	/**
	 * 设置错误处理器
	 *
	 */
	void setErrorHandler(ErrorHandler errorHandler);

	/**
	 * 添加监听器
	 *
	 */
	void addListener(SocketListener listener);

	/**
	 * 获得用户缓存的消息
	 *
	 * 当发生异常时，可以尝试获取没有发送成功的消息
	 *
	 */
	List<String> getUserMessages();
```