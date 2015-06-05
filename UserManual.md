# 快速开始 #

AtutoComet1.0

  * **需要的资源**
    1. 在你的web工程中需要包含 [auto-comet.jar](http://code.google.com/p/auto-comet/downloads/list)。如果你想与Spring整合，还需要包含[auto-comet-spring.jar](http://code.google.com/p/auto-comet/downloads/list)。
    1. 第三方依赖：commons-logging-1.1.1.jar,commons-io-1.4.jar,commons-lang-2.4.jar,asm-1.5.3.jar,commons-beanutils-commons-beanutils.jar
    1. web页面中需要引入[comet.js](http://code.google.com/p/auto-comet/downloads/list)



  * **Comet分发器配置**
> SocketDispatcherServlet是AutoComet的核心控制器。你需要在web.xml中添加配置：
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

> 资源路径dispatcherConfigLocation支持模式路径，请参考：[资源路径的配置](ResourceLocation.md)

  * **Comet服务配置**
> 每一个comet标签配置了一个comet服务，comet服务以url到handler的映射方式提供。dispatcher.comet.xml配置:
```
<?xml version="1.0" encoding="UTF-8"?>
<auto-comet xmlns="http://www.auto.org/schema/comet"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 

xmlns:spring="http://www.auto.org/schema/comet/spring"
	xsi:schemaLocation="http://www.auto.org/schema/comet http://www.auto.org/schema/comet/auto-comet-

1.0.xsd
	http://www.auto.org/schema/comet/spring http://www.auto.org/schema/comet/auto-comet-spring-

1.0.xsd">
	<!--连接超时配置，单位为分钟。 -->
	<timeout value="120" />

	<!--配置一个comet服务，你也可以将它理解为一个通道。 -->
	<!--request:请求uri -->
	<!--handler:socket处理器，socket处理器必须实现org.auto.comet.SocketHandler接口 -->
	<comet request="/chatRoom.comet"
		handler="org.auto.comet.example.chat.service.ChatRoomSocketHandler" />

</auto-comet>
```
> 如果你想[与Spring整合](AutoCometSpring.md)，配置会有点不一样。

  * **实现SocketHandler接口**
> 你需要实现SocketHandler接口来管理你与客户端的socket连接。
> SocketHandler中申明了2个方法：

> 接受连接请求方法：`boolean accept(Socket socket, HttpServletRequest request);`

> 断开连接请求方法：`void quit(Socket socket, HttpServletRequest request);`


> 接受连接请求方法，可以处理一个客户端(浏览器)发送的连接请求。处理器可以通过传入的socket完成给客户端发送消息等操作。你可以从request中获取客户端连接请求时发送的参数。返回的boolean值表示是否愿意接受该连接请求。

> 断开接口与接受接口类似。

> 一个典型的SocketHandler接口的实现如下：
```
public class ChatRoomSocketHandler implements SocketHandler {

	private ChatRoomService chatRoomService;

	private Map<Serializable, Socket> socketMapping = new HashMap<Serializable, Socket>();

	public synchronized boolean accept(Socket socket, HttpServletRequest request) {
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

	public synchronized void quit(Socket socket, HttpServletRequest request) {
		String userId = request.getParameter("userId");
		socketMapping.remove(userId);
		this.chatRoomService.loginOut(userId);
	}

	private synchronized void romveSocket(Socket socket) {
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

  * **使用socket**
> 可以通过Socket接口给客户端推送消息。Socket接口提供了如下的方法：
```
	/**
	 * 发送消息
	 *
	 */
	void send(String message);

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
	 * 获得缓存的数据
	 *
	 * 当发生异常时，可以尝试获取没有发送成功的数据
	 *
	 */
	List<String> getCachedData();
```

  * **客户端编码**
> 页面需要引入javascript文件：[comet.js](http://code.google.com/p/auto-comet/downloads/list)。
> comet.js提供了面向对象风格的API。
  * 创建一个comet连接：
```
	var comet = new Auto.Comet({
		url : "/cometChat/chatRoom.comet",  //连接地址
		async : true,                                        //是否异步接收数据
		accept : function(data) {                      //接收到数据的操作
			//...
		}
	});
```

  * 打开一个comet连接：
```

	comet.connection({
		userId : id
	}, function() {
		alert("连接成功！");
	}, function() {
		alert("连接失败！");
	});
```
> connection方法可以接收4个参数，参数可选：
```
	/**
	* 开始链接
	*
	* @param userParam
	*            连接时传递给服务器端的参数
	* @param success
	*            连接成功处理方法
	* @param failure
	*            连接失败处理方法
	*
	*/
	connection : function(userParam, success, failure, caller) {
		//...
	}
```
  * 断开一个comet连接，断开连接的时候也可以传递http参数:
```
	comet.disconnect({
		userId : id
	});
```

  * **其他**

> [与Spring整合](AutoCometSpring.md)

> [提升并发性能](AutoCometSpringConcurrent.md)

> [推送协议](AutoCometPushProtocol.md)

> [演示工程](CometDemo.md)