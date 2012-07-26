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
  
  以防止意外情况下无法从zk获取配置时，正常启动应用。

* iservice-java保证子树的一致性。

  即某个订阅节点发生变化后，其底下的子树会重新从zk上拉取，保证所有新拉取的数据同时起效。