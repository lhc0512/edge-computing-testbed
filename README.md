



# 技术

- Java 17
- Maven 3.6.3
- DJL 0.21.0
- Spring Boot 2.7.5
- Spring Cloud 2021.0.4
    - Nacos (配置中心，注册中心)
    - Ribbon、Loadbalancer (RPC)
- Hadoop 3.3.4
- MySQL 8.0.31
- MyBatis-Plus 3.5.2
- Docker 23.0.3
- Kubernetes 1.22.12



# 学习资料

- [Docker、Kubernetes](https://www.bilibili.com/video/BV13Q4y1C7hS/?spm_id_from=333.337.search-card.all.click)
- [SpringCloud](https://www.bilibili.com/video/BV18E411x7eT/?spm_id_from=333.337.search-card.all.click)
- [SpringBoot](https://www.bilibili.com/video/BV19K4y1L7MT/?spm_id_from=333.337.search-card.all.click&vd_source=2aad8136eecb2edf8db4cde67f60d208)
- [Hadoop的HDFS](https://www.bilibili.com/video/BV1Qp4y1n7EN/?spm_id_from=333.337.search-card.all.click&vd_source=2aad8136eecb2edf8db4cde67f60d208)
- [MyBatis](https://www.bilibili.com/video/BV1VP4y1c7j7/?spm_id_from=333.337.search-card.all.click&vd_source=2aad8136eecb2edf8db4cde67f60d208)
- [MyBatis-Plus](https://www.bilibili.com/video/BV12R4y157Be/?spm_id_from=333.337.search-card.all.click&vd_source=2aad8136eecb2edf8db4cde67f60d208)
- [DJL](https://d2l-zh.djl.ai/)

# 快速开始

## 安装Java 17

## 安装mysql 8.0

基于Kubernetes安装可以参考`edge-computing-v3/k8s/mysql.yaml`，使用`kubectl apply -f mysql.yaml` 安装。

执行`sql/create_table.sql`，创建库和表

## 安装nacos 2.x

基于Kubernetes安装可以参考`edge-computing-v3/k8s/nacos.yaml`，使用`kubectl apply -f nacos.yaml` 安装。
安装参考官方文档：

https://nacos.io/zh-cn/docs/v2/quickstart/quick-start.html

添加`nacos/DEFAULT_GROUP`中所有配置

## 安装hadoop 3.3.4

安装单节点伪分布式，参考官方文档：

https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html

## 安装Docker、K8S【可选】

## 启动Java微服务

Main函数运行

设置不同sever port（7001、7002）和application name ，通过修改启动时参数，如`--server.port=7001 --spring.application.name=edge-node-1`

启动最少的2个`edge-node` 进行测试。修改边缘节点个数，需要在nacos中修改`edgeComputing.edgeNodeNumber`参数，

启动`edge-controller`

启动`edge-experiment` 跑实验。

默认的任务卸载算法为Random。

![image-20230219150130262](https://lhc-note.oss-cn-guangzhou.aliyuncs.com/images/image-20230219150130262.png)

若安装Docker和K8S，可以使用shell脚本运行。

运行`start-experiment.sh`启动实验。

运行`stop-experiment.sh`关闭实验。

