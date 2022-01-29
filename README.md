# Speedy-API

All entity in one api resource manager

![example workflow](https://github.com/SilentSamurai/Speedy-API/actions/workflows/main.yml/badge.svg)
<BR>
![example workflow](https://github.com/SilentSamurai/Speedy-API/actions/workflows/release.yml/badge.svg)

CREATE and release keys

```shell

gpg --keyserver http://keyserver.ubuntu.com:11371 --send-keys DA525CBBDB246AEEB31F973ACAB49069B091BC8F 
gpg --export -a "souravbumbdas25@gmail.com" > public.key 
gpg --export-secret-keys -a DA525CBBDB246AEEB31F973ACAB49069B091BC8F >private.key 
gpg --list-secret-keys souravbumbdas25@gmail.com 
gpg --import private.key


mvn --no-transfer-progress --batch-mode clean deploy -P=ossrh

mvn -N versions:update-child-modules

```
