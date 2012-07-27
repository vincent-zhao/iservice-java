# 介绍

### iservice

iservice是一个基于zookeeper的配置管理中心。功能主要如下：

* 统一管理各种配置。
* 应用可以通过iservice提供的客户端来获取配置，同时监听配置的更改。

关于iservice的具体介绍见这里

### iservice-java

iservice-java是iservice的java客户端，用来获取iservice上的配置和监听配置更改，同时做好配置本地化等工作，具体的api下面介绍。

iservice-java主要功能如下：

* 订阅zookeeper上某个节点，以此可以获得这个节点下的所有信息。
* 监听订阅节点，订阅节点发生变化时被通知。


# iservice-java高级特性

* iservice-java本地化所有获取的配置信息。
  
  以防止意外情况下无法从zk获取配置时，正常启动应用。iservice-java本身保证了本地文件获取的配置的一致性（针对多进程应用）。

* iservice-java保证子树的一致性。

  即某个订阅节点发生变化后，其底下的子树会重新从zk上拉取，保证所有新拉取的数据同时起效。
  
* iservice-java察觉到本地化文件被外部修改后，忽略整个配置的更新。

  在更新配置树的时候，如果发现整个子树中某个本地化的文件被人工或者其他程序修改过，则会忽略整个配置子树的更新，以此来保留人工的配置以及配置一致性。
  
* 配置信息都是从内存获取

  用iservice-java获取配置信息都是从内存获取。因为iservice-java发现被订阅的节点发生变化便会自动本地化新数据，成功后再载入内存。所以频繁调用api中的get接口是从内存获取数据，速度很快。
  
* 定时与zk对比

  因为zk本身watch一次触发或者频繁更新的缘故，不能保证每次变更都通知到客户端，所以iservice-java本身每隔一段时间会主动对zk上的数据很本地的数据进行比较，如果发现变更，则会执行更新操作，更新动作不完全依赖zk的watch功能。
  

# iservice-java 面对异常场景

iservice是高可靠的配置中心，在面对一些异常情况的时候（例如断网），其表现如下：

* 应用启动时，zk无法连接

  如果有之前残留的本地文件，则读取本地文件，如果没有则无法获得任何信息。 应用保持运行状态，某一时刻连接上zk后，从zk获取最新的信息。之后一切正常
  
* 运行过程中，zk挂掉

  应用没有影响，继续运行。zk重新启动后，应用重新连接上zk，之后正常运行。
  
* 运行过程中，出现断网现象

  1. 如果断网时间较短，在设置的zk的session timeout时间以内，一切恢复正常。

  2. 如果断网时间超过设置的zk的seesion timeout，则无法再连接上zk，可以一直使用过期数据进行操作。iservice-java会触发一个“过期”事件， 用户需要监听在这个事件上。


# iservice-java api说明

* 初始化Iservice对象：

```java
//创建IService对象，参数分别是：zk集群地址，根路径，用户名，密码，本地缓存路径
IService iservice = new IService("127.0.0.1:2181", "/", "", "", "./tmp");

/**
 * 这里的MyHandler是Handler的一个实现，Handler是我定义的一个抽象类。
 * Handler的实现类用来注册在具体事件上，当发生某些事件时会回调Handler中的callback方法，下面会具体介绍。
 */
MyHandler mh = new MyHandler();

//setEventHandle方法用于在特有事件(特有时间在下面列出)上设置监听对象，下面例子表示，如果zk连接成功，调用mh的callback方法。
iservice.setEventHandle(Constants.CONNECT_EVENT, mh);

//这步是很重要的，表示启动iservice。这个方法最好是在所有setEventHandle之后
iservice.init();

//特有事件包括：
Constants.CONNECT_EVENT         //zk连接成功触发的事件
Constants.DISCONNECT_EVENT      //zk是去连接触发的事件
Constants.EXPIRED_EVENT         //zk session过期触发的事件
Constants.ZK_PATH_ERROR_EVENT   //zk 根目录找不到触发的事件
```



* Handler：

```java
//Handler定义
  public abstract class Handler {
    /**
     * something not abstract
     */
     
     //@param {Object} content callback时候回调的内容，不一样的事件callback回调内容不一样
     public abstract void callback(Object content);
     
  }
```

* 创建针对某个具体配置对象（具体关注配置的节点）

```java
//下面操作生成了一个Config对象，这个Config对象指向zookeeper中的root/app1节点，获得并且监听这个节点下的所有配置信息。
Config config = iservice.createConfig("/root/app1");

//为Config对象设置回调对象，当具体配置发生变化时调用对象的callback方法，除了配置发生变化会触发时间，还有一些其他事件下面说明
config.setEventHandle(Constants.DATA_CHANGE_EVENT, new SomeHandle());

/**
 * 通过config的get方法可以获取app1节点下的某个配置信息。get的返回是一个Data接口的实现类。
 * 下列代码中的DefaultData是默认返回数据格式，其不对zk取出的数据做任何处理，调用其get方法直接返回字符串。
 * 更多的数据类型等待开发，例如解析ini类型文件，json类型文件等
 */
DefaultData data = (DefaultData)config.get("key1");

//打印配置信息内容
System.out.println(data.getData());

//事件包括：
Constants.GET_DATA_ERROR_EVENT //获取数据出现错误
Constants.DATA_CHANGE_EVENT    //数据发生变化（触发时，callback的参数为String类型，表示变化后的值）
Constants.DUMP_FAIL_EVENT      //数据本地化失败（因为本地文件被人工修改过）
Constants.DUMP_ERROR_EVENT     //数据本地化发生错误
```

# 关于iservice-java

* iservice-java包中有全面的单元测试（test目录下），测试的配置在test/conf下。
* iservice-java包中提供了2个demo（配置文件在demo/conf下），模拟客户端使用iservice-java和zk树定时修改，以此可以查看iservice-java是如何工作的。
* iservice-java还在持续开发中，如果有问题和建议可以联系我：yixuan.zzq@taobao.com
