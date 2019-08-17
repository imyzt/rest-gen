# rest-gen
根据规则自动生成Spring MVC 控制器方法

# 项目背景
近期公司有个项目需要写大量的查询接口, 每次都是在Controller里面 `@GetMapping("xxx")...`的开写,着实很烦.  
正好最近在编写[crud-intellij-plugin](https://github.com/imyzt/crud-intellij-plugin)项目的Mybatis-Plus增强,
学了点Intellij-Plugin的开发,所以就干脆写了一个Controller Endpoint的生成器.然后就有了这个repo.

# 安装

1. 选择最新版本下载 [rest-gen-releases](https://github.com/imyzt/rest-gen/releases)  

2. Intellij IDEA 2019安装  

![ezgif-1-9a2b843f02c4.gif](https://i.loli.net/2019/08/17/VICx5N67ndkbfLv.gif)

# 使用
### 规则

```
methodName(argType$argName,argType#argName).requestMethod
```

1. methodName: 方法名称，将用作方法名和Rest Endpoint
2. argType: 参数类型， 基本类型与字符串会被自动转换，非基本类型原样数据
3. $: rest参数
4. #: form参数
5. requestMethod: 请求方法， 支持get/post/put/delete

### 生成

![生成视频](https://i.loli.net/2019/08/17/9F4GrAEL8jxuhUl.gif)


# 参与贡献方式

插件开发只要求掌握Java即可，下面提供了几个自认为不错的入门教程  
[plugins-develop](https://github.com/judasn/IntelliJ-IDEA-Tutorial/blob/master/plugins-develop.md)  
[JetBrains Plugins](https://younghz.github.io/jetbrains-plugins)  
[AndroidStudio插件开发（Hello World篇）系列教程](https://blog.csdn.net/huachao1001/article/details/53856916)

# 开源协议

[Apache 2.0 License](https://github.com/imyzt/rest-gen/blob/master/LICENSE)