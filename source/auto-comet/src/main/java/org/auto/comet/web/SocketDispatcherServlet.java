package org.auto.comet.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.auto.comet.ConcurrentConnectionManager;
import org.auto.comet.ConcurrentPushSocket;
import org.auto.comet.ObjectFactory;
import org.auto.comet.Protocol;
import org.auto.comet.PushSocket;
import org.auto.comet.SocketHandler;
import org.auto.comet.ConnectionManager;
import org.auto.comet.config.CometConfigMetadata;
import org.auto.comet.support.JsonProtocolUtils;
import org.auto.comet.support.ObjectFactoryBuilder;
import org.auto.comet.xml.XmlConfigResourceHandler;
import org.auto.web.resource.WebResourceScanMachine;
import org.auto.web.util.RequestUtils;

/**
 * <p>
 * 连接转发servlet
 * </p>
 * 该类用于处理所有的连接请求
 * 
 * 
 * @author XiaohangHu
 * */
public class SocketDispatcherServlet extends AbstractDispatcherServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = -3671690949937300581L;

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	public static final String INIT_PARAMETER_CONFIG_LOCATION = "dispatcherConfigLocation";

	/** 默认为1分钟 */
	private long pushTimeout = 60000l;

	private UrlHandlerMapping urlHandlerMapping;
	private ConnectionManager socketManager;

	public final void init() throws ServletException {
		getServletContext().log(
				"Initializing " + getClass().getSimpleName() + " '"
						+ getServletName() + "'");
		super.init();
		CometConfigMetadata cometConfig = readCometConfig();
		initHandlerMapping(cometConfig);
		initSocketManager(cometConfig);
		if (this.logger.isInfoEnabled()) {
			this.logger.info("SocketDispatcherServlet '" + getServletName()
					+ "': initialization started");
		}
	}

	/**
	 * 开始定时任务
	 * */
	public void startTimer(ConnectionManager socketManager) {
		Timer timer = new Timer(true);
		long period = pushTimeout / 2l;
		timer.schedule(new CheckPushTimeoutTimerTask(socketManager),
				pushTimeout, period);
	}

	protected CometConfigMetadata readCometConfig() {
		ServletConfig config = getServletConfig();
		String dispatcherConfigLocation = config
				.getInitParameter(INIT_PARAMETER_CONFIG_LOCATION);
		if (StringUtils.isBlank(dispatcherConfigLocation)) {
			dispatcherConfigLocation = getDefaultDispatcherConfigLocation();
		}
		WebResourceScanMachine webResourceScanMachine = new WebResourceScanMachine(
				this.getServletContext());
		CometConfigMetadata cometConfig = new CometConfigMetadata();

		// 扫描将配置元数据放入cometConfig中
		webResourceScanMachine.scanLocations(dispatcherConfigLocation,
				new XmlConfigResourceHandler(cometConfig));

		return cometConfig;
	}

	protected void initHandlerMapping(CometConfigMetadata cometConfig) {
		ObjectFactory objectFactory = ObjectFactoryBuilder.creatObjectFactory(
				cometConfig, getServletContext());

		UrlHandlerMappingBuilder mappingBuilder = new UrlHandlerMappingBuilder(
				objectFactory);

		urlHandlerMapping = mappingBuilder.buildHandlerMapping(cometConfig);

	}

	protected void initSocketManager(CometConfigMetadata cometConfig)
			throws ServletException {
		ConcurrentConnectionManager socketManager = new ConcurrentConnectionManager(
				pushTimeout);
		startTimer(socketManager);
		Integer timeout = cometConfig.getTimeout();
		if (null != timeout) {
			long asyncTimeout = (long) (60000l * timeout);
			socketManager.setAsyncTimeout(asyncTimeout);
		}
		this.socketManager = socketManager;
	}

	public String getDefaultDispatcherConfigLocation() {
		return "/WEB-INF/dispatcher-servlet.xml";
	}

	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		ConnectionManager socketManager = getSocketManager();

		String synchronizValue = getSynchronizValue(request);
		if (null == synchronizValue) {// 同步值为空则为接收消息
			receiveMessage(socketManager, request, response);
		} else if (Protocol.CONNECTION_VALUE.equals(synchronizValue)) {// 创建链接请求
			creatConnection(socketManager, request, response);
		} else if (Protocol.DISCONNECT_VALUE.equals(synchronizValue)) {// 断开链接请求
			disconnect(socketManager, request);
		}
	}

	/**
	 * 接收消息
	 * */
	private static void receiveMessage(ConnectionManager socketManager,
			HttpServletRequest request, HttpServletResponse response) {
		String connectionId = getConnectionId(request);
		socketManager.receiveMessage(connectionId, request, response);
	}

	private void creatConnection(ConnectionManager socketManager,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		SocketHandler service = getSocketHandler(request);
		if (null == service) {
			String uri = RequestUtils.getServletPath(request);
			throw new DispatchException("Cant find comet handler by [" + uri
					+ "]. Did you register it?");
		}

		PushSocket socket = new ConcurrentPushSocket();
		boolean accept = service.accept(socket, request);
		String commend = null;
		if (accept) {// 如果接受连接请求则创建连接
			socketManager.creatConnection(socket);
			commend = JsonProtocolUtils.getConnectionCommend(socket.getId());
		} else {// 如果拒绝连接请求
			commend = JsonProtocolUtils.getConnectionCommend(null);
		}
		PrintWriter write = response.getWriter();
		// 返回生成的链接id
		write.write(commend);
		write.flush();
	}

	/**
	 * 断开链接
	 * */
	private void disconnect(ConnectionManager socketManager,
			HttpServletRequest request) {
		SocketHandler handler = getSocketHandler(request);
		if (null == handler) {
			String uri = RequestUtils.getServletPath(request);
			throw new DispatchException("Cant find comet handler by [" + uri
					+ "]. Did you registered it?");
		}
		String connectionId = getConnectionId(request);
		if (StringUtils.isBlank(connectionId)) {
			if (logger.isWarnEnabled()) {
				logger.warn("Null connectionId from[ip:"
						+ request.getRemoteAddr() + "]");
			}
			return;
		}
		socketManager.disconnect(connectionId, handler, request);
	}

	protected SocketHandler getSocketHandler(HttpServletRequest request) {
		String uri = RequestUtils.getServletPath(request);
		return urlHandlerMapping.getHandler(uri);
	}

	private static String getSynchronizValue(HttpServletRequest request) {
		// StringUtils.trim();
		return request.getParameter(Protocol.SYNCHRONIZE_KEY);
	}

	private static String getConnectionId(HttpServletRequest request) {
		return request.getParameter(Protocol.CONNECTIONID_KEY);
	}

	private ConnectionManager getSocketManager() {
		if (null == socketManager) {
			throw new DispatchException("Cant find socketManager!");
		}
		return socketManager;
	}

}

class CheckPushTimeoutTimerTask extends TimerTask {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionManager socketManager;

	CheckPushTimeoutTimerTask(ConnectionManager socketManager) {
		this.socketManager = socketManager;
	}

	@Override
	public void run() {
		// 异常处理，防止守护线程死亡！
		try {
			socketManager.checkPushTimeout();
		} catch (Throwable e) {
			logger.error("Push timeout Exception!", e);
		}
	}
}
