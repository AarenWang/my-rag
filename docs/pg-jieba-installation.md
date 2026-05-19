# pg_jieba 安装记录

本文记录在当前 macOS 本机 PostgreSQL 中安装 `pg_jieba` 的过程。

## 1. 环境信息

```text
PostgreSQL: 17.9 (Homebrew)
PostgreSQL bin: /opt/homebrew/opt/postgresql@17/bin
PostgreSQL lib: /opt/homebrew/lib/postgresql@17
PostgreSQL share: /opt/homebrew/share/postgresql@17
数据库用户: pg
已启用数据库: postgres, my-rag
系统架构: arm64
```

验证 PostgreSQL：

```sh
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d postgres -c "select version(), current_database(), current_user;"
```

注意：本机 PostgreSQL 没有当前系统用户名对应的 role，因此需要使用 `-U pg` 连接。

## 2. 获取源码

Homebrew 没有可直接安装的 `pg_jieba` formula，因此使用源码编译。

```sh
git clone --depth 1 https://github.com/jaiminpan/pg_jieba.git /private/tmp/pg_jieba
cd /private/tmp/pg_jieba
git submodule update --init --recursive
```

`pg_jieba` 依赖 `libjieba` 子模块，必须初始化子模块后才能编译。

## 3. 配置编译

项目使用 CMake。由于本机 `/usr/local/bin/cmake` 是 `x86_64`，而 Homebrew PostgreSQL 17 是 `arm64`，必须显式指定 `CMAKE_OSX_ARCHITECTURES=arm64`，否则会生成不可加载的 `x86_64` 扩展。

```sh
cd /private/tmp/pg_jieba
mkdir -p build
cd build

cmake \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DPostgreSQL_LIBRARY=/opt/homebrew/lib/postgresql@17/libpq.dylib \
  -DPostgreSQL_INCLUDE_DIR=/opt/homebrew/include/postgresql@17 \
  -DPostgreSQL_TYPE_INCLUDE_DIR=/opt/homebrew/include/postgresql@17/server \
  -DCMAKE_C_FLAGS="-I/opt/homebrew/opt/gettext/include -I/opt/homebrew/opt/krb5/include -I/opt/homebrew/opt/openssl@3/include -I/opt/homebrew/opt/readline/include -I/opt/homebrew/opt/lz4/include -I/opt/homebrew/opt/zstd/include -I/opt/homebrew/opt/icu4c@78/include" \
  -DCMAKE_CXX_FLAGS="-I/opt/homebrew/opt/gettext/include -I/opt/homebrew/opt/krb5/include -I/opt/homebrew/opt/openssl@3/include -I/opt/homebrew/opt/readline/include -I/opt/homebrew/opt/lz4/include -I/opt/homebrew/opt/zstd/include -I/opt/homebrew/opt/icu4c@78/include" \
  ..
```

这里手动补充 `gettext` 等 include 路径，是因为编译 PostgreSQL 17 server 头文件时需要 `libintl.h`。

## 4. 编译和安装

```sh
make -j4
make install
```

安装后文件位置：

```text
/opt/homebrew/lib/postgresql@17/pg_jieba.so
/opt/homebrew/share/postgresql@17/extension/pg_jieba.control
/opt/homebrew/share/postgresql@17/extension/pg_jieba--1.1.1.sql
/opt/homebrew/share/postgresql@17/tsearch_data/jieba_base.dict
/opt/homebrew/share/postgresql@17/tsearch_data/jieba_hmm.model
/opt/homebrew/share/postgresql@17/tsearch_data/jieba_user.dict
/opt/homebrew/share/postgresql@17/tsearch_data/jieba.stop
/opt/homebrew/share/postgresql@17/tsearch_data/jieba.idf
```

在 macOS 的当前 PostgreSQL 17 安装中，扩展动态库按 `.dylib` 查找。`make install` 生成的是 `pg_jieba.so`，因此需要额外复制一份：

```sh
cp /private/tmp/pg_jieba/build/pg_jieba.so /opt/homebrew/lib/postgresql@17/pg_jieba.dylib
```

确认架构：

```sh
file /opt/homebrew/lib/postgresql@17/pg_jieba.dylib
```

期望结果包含：

```text
Mach-O 64-bit bundle arm64
```

## 5. 在数据库启用扩展

PostgreSQL 扩展需要按数据库启用。安装扩展文件后，仍然要在每个需要使用 `pg_jieba` 的数据库里执行 `CREATE EXTENSION`。

当前已在 `postgres` 和业务库 `my-rag` 中启用。

注意：项目配置文件里的默认数据库名是 `my_rag`，但当前本机实际存在并使用的是 `my-rag`。如果后续改回 `my_rag`，也需要在 `my_rag` 库中重新执行 `CREATE EXTENSION IF NOT EXISTS pg_jieba;`。

在 `postgres` 库启用：

```sh
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d postgres -v ON_ERROR_STOP=1 -c "CREATE EXTENSION IF NOT EXISTS pg_jieba;"
```

在 `my-rag` 库启用：

```sh
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d my-rag -v ON_ERROR_STOP=1 -c "CREATE EXTENSION IF NOT EXISTS pg_jieba;"
```

确认扩展版本：

```sh
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d postgres -c "select extname, extversion from pg_extension where extname = 'pg_jieba';"
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d my-rag -c "select extname, extversion from pg_extension where extname = 'pg_jieba';"
```

当前两个库的结果均为：

```text
 extname  | extversion
----------+------------
 pg_jieba | 1.1.1
```

## 6. 分词验证

```sh
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d postgres -c "select to_tsvector('jiebacfg', '我来到北京清华大学');"
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d my-rag -c "select to_tsvector('jiebacfg', '我来到北京清华大学');"
```

当前两个库的结果均为：

```text
          to_tsvector
--------------------------------
 '北京':3 '来到':2 '清华大学':4
```

## 7. 常见问题

### 7.1 `role "wangrenjun" does not exist`

默认不带 `-U` 时，`psql` 会使用当前系统用户名连接。本机 PostgreSQL 没有该 role，所以需要显式使用：

```sh
/opt/homebrew/opt/postgresql@17/bin/psql -U pg -d postgres
```

### 7.2 `libintl.h file not found`

编译 PostgreSQL server 头文件时缺少 gettext include 路径。配置 CMake 时补充：

```text
-I/opt/homebrew/opt/gettext/include
```

### 7.3 生成了 x86_64 动态库

如果 `file build/pg_jieba.so` 显示 `x86_64`，说明 CMake 或构建环境选择了错误架构。清理 build 目录后重新配置：

```sh
rm -rf /private/tmp/pg_jieba/build
mkdir -p /private/tmp/pg_jieba/build
cd /private/tmp/pg_jieba/build
cmake -DCMAKE_OSX_ARCHITECTURES=arm64 ...
make -j4
```

### 7.4 `could not access file "$libdir/pg_jieba"`

PostgreSQL 在当前 macOS 安装中查找 `.dylib`，但 `make install` 只安装了 `.so`。复制一份 `.dylib` 后重试：

```sh
cp /private/tmp/pg_jieba/build/pg_jieba.so /opt/homebrew/lib/postgresql@17/pg_jieba.dylib
```
