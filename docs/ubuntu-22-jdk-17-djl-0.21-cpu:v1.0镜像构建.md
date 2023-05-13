



https://hub.docker.com/_/ubuntu/tags?page=1&name=22

```sh
docker pull ubuntu:22.04
```



```sh
docker run --name ubuntu22 -itd ubuntu:22.04 /bin/bash
```



```sh
docker exec -it ubuntu22 /bin/bash
```



```sh
cd root
mkdir lhc-dev
```



```sh
docker cp jdk-17.0.5 ubuntu22:/root/lhc-dev
```



```sh
docker cp 1.13.1-20221220-cpu-linux-x86_64 ubuntu22:/root/.djl.ai/pytorch
```



```sh
docker commit ubuntu22 ubuntu-22-jdk-17-djl-0.21-cpu:v1.0
```

