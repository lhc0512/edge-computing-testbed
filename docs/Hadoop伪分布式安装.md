





参考官方文档

https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html

# Prerequisites

## Linux

## Java

https://cwiki.apache.org/confluence/display/HADOOP/Hadoop+Java+Versions

- Apache Hadoop 3.3 and upper supports Java 8 and Java 11 (runtime only)

- - Please compile Hadoop with Java 8. Compiling Hadoop with Java 11 is not supported

- Apache Hadoop from 3.0.x to 3.2.x now supports only Java 8
- Apache Hadoop from 2.7.x to 2.10.x support both Java 7 and 8

```shell
(base) java -version
java version "1.8.0_202"
Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
Java HotSpot(TM) 64-Bit Server VM (build 25.202-b08, mixed mode)
```

## ssh, sshd, pdsh

1. `ssh` must be installed and `sshd` must be running to use the Hadoop scripts that manage remote Hadoop daemons if the optional start and stop scripts are to be used.
2. Additionally, it is recommmended that `pdsh` also be installed for better ssh resource management.

```shell
sudo yum install ssh sshd pdsh
```



ssh 免密登录自己

```shell
ssh localhost
```

# Download

mirror

https://mirrors.tuna.tsinghua.edu.cn/apache/hadoop/core/hadoop-3.3.4/

# Pseudo-Distributed Installation

## Hadoop configuration

编辑 `.bash_profile`

```shell
export HADOOP_HOME=/home/hongcai/lhc-dev/hadoop-3.3.4
export PATH=$PATH:$HADOOP_HOME/bin
export PATH=$PATH:$HADOOP_HOME/sbin
source ~/.bash_profile
```

编辑 `/home/hongcai/lhc_dev/hadoop-3.3.4/etc/hadoop/hadoop-env.sh`

```shell
export JAVA_HOME=/home/hongcai/lhc-dev/jdk1.8.0_202
```

## HDFS configuration

`core-site.xml`

注意为host，se-lab-3990x。

```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://se-lab-3990x2:9000</value>
    </property>
    <property>
        <name>hadoop.http.staticuser.user</name>
        <value>hongcai</value>
    </property>
</configuration>
```

`hdfs-site.xml`



```xml
<configuration>
  <property>
    <name>dfs.replication</name>
    <value>1</value>
  </property>
  
  <!--name format 存储位置 -->
  <property>
    <name>dfs.namenode.name.dir</name>
    <value>/home/hongcai/lhc-dev/hadoop-3.3.4/dfs/name</value>
  </property>
  
  <property>
    <name>dfs.datanode.data.dir</name>
    <value>/home/hongcai/lhc-dev/hadoop-3.3.4/dfs/data</value>
  </property>
 
  <property>
    <name>dfs.permissions</name>
    <value>false</value>
  </property>
</configuration>
```



## YARN configuration

`mapred-site.xml`

```xml
<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
</configuration>
```

`yarn-site.xml`

```xml
<configuration>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
</configuration>
```

## HDFS format

```shell
hdfs namenode -format
```

## start HDFS

start dfs

```shell
(base) start-dfs.sh
Starting namenodes on [localhost]
Starting datanodes
Starting secondary namenodes [se-lab-3990x]
```

jps

```shell
(base) jps
58226 NameNode
59319 SecondaryNameNode
58860 DataNode
59901 Jps
```

Web UI

http://se-lab-3990x2:9870/dfshealth.html#tab-overview

## start YARN

start yarn

```shell
(base) start-yarn.sh
Starting resourcemanager
Starting nodemanagers
```

jps

```shell
(base) jps
105298 Jps
100023 DataNode
99670 NameNode
104219 ResourceManager
104555 NodeManager
102186 SecondaryNameNode
```

Web UI

http://se-lab-3990x:8088/cluster

