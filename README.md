

# Overview

`edge-computing-testbed` is a testbed for task offloading in edge computing. It uses containers to emulate the edge node. It supports schedulers based on deep reinforcement learning.

# Installation

## Java

The testbed needs to install Java 17+.

## Kubernetes

We recommend installing the K8S via the KubeKey.

https://www.kubesphere.io/docs/v3.3/quick-start/all-in-one-on-linux/

It also will install docker.

## Docker Image

build the docker image based on `docker/build-image.md` for components in the testbed. The components include `edge-node`, `edge-controller` and `edge-exeperiment`.

## MySQL

Execute `kubectl apply -f k8s/mysql.yaml` to install MySQL.

Execute `sql/create_table.sql` to create database and tables in MySQL.

## Nacos

Execute `kubectl apply -f k8s/nacos.yaml` to install Nacos.

Add all the configurations in `nacos/`.

## Hadoop

Install Hadoop 3.3.4 using Pseudo-Distributed Installation via the official document.

https://hadoop.apache.org/

we also provide the installation document in `hadoop/hadoop-installation.md`.

# Architecture

The testbed consists of three layers and middleware. These three layers include the DRL layer, control layer, and execution layer. The middleware includes Service Registry, Services Configuration, Database, and Distributed File System(DFS).

![image-20230517163523585](https://lhc-note.oss-cn-guangzhou.aliyuncs.com/images/image-20230517163523585.png)

# Usage

Start experiment:

```shell
 start-experiment.sh
```

Stop experiment:

```shell
 stop-experiment.sh
```