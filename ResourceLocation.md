# 资源路径的配置 #

  * **多个资源路径**

> 多个路径间用逗号分隔。
```
/WEB-INF/**/*.comet.xml,classpath:/**/*.comet.xml,file:D:\comet\dispatcher.comet.xml
```

  * **一个资源路径**
```
classpath:/**/*.comet.xml
```
> 一个路径分为2部分，协议`"classpath"`和模式`"/**/*.comet.xml"`，之间用":"分隔。

> "classpath:"代表路径协议为classpath。会从classpath中查找对应资源。

> "file":代表路径协议为文件系统。

> 如果省略路径协议，则使用默认的协议。
> 如果默认路径协议为"webroot"，则`"webroot:/WEB-INF/**/*.comet.xml"`可简写为为`"/WEB-INF/**/*.comet.xml"`。

  * **支持的路径协议**
> classpath,file,webroot(如果为servlet容器)