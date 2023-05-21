



https://hub.docker.com/_/ubuntu/tags?page=1&name=22

download the image.
```sh
docker pull ubuntu:22.04
```

run the container
```sh
docker run --name ubuntu22 -itd ubuntu:22.04 /bin/bash
```

enter the container.
```sh
docker exec -it ubuntu22 /bin/bash
```

create paths.
```sh
cd root
mkdir lhc-dev
mkdir .djl.ai
```


copy local jdk to the container.
```sh
docker cp jdk-17.0.5 ubuntu22:/root/lhc-dev
```


copy local djl to the container so that the container do not need to download when setup.
```sh
docker cp 1.13.1-20221220-cpu-linux-x86_64 ubuntu22:/root/.djl.ai/pytorch
```

create the image.
```sh
docker commit ubuntu22 ubuntu-22-jdk-17-djl-0.21-cpu:v1.0
```

