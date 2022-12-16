# Spring-MultiRequestBody
本项目在 [chujianyun/Spring-MultiRequestBody](https://github.com/chujianyun/Spring-MultiRequestBody) 的基础上进行修改！

## 项目描述
1. 本项目基于 `SpringBoot3.0` 和 `JDK17`，如果是低版本使用只需要将 `servlet` 的包由 `jakarta` 改为 `javax` 即可。
2. 支持 `@Validated` 注解进行参数校验。
3. 支持与 `@RequestBody` 注解同时使用。
4. `JSON` 解析由 `FastJson` 调整为 `Jackson`。
5. 其他功能与原项目一致。

## 代码说明
* 核心类为 `MultiRequestBody`、`MultiRequestBodyArgumentResolver`、`WebMvcConfig`，其中 `MultiRequestBodyArgumentResolver` 继承了 `AbstractMessageConverterMethodArgumentResolver`，参考了 `RequestResponseBodyMethodProcessor`（也就是 `@RequestBody` 注解参数解析器）的写法。
* `RepeatableFilter`、`RepeatedlyRequestWrapper`、`FilterConfig` 是为了支持与 `@RequestBody` 注解同时使用，如果不需要，不注入该过滤器或者删除相关代码即可。
* 代码中部分工具类来自 `Hutool`，如果有需求可自行修改或替换。
* 关于 `JSON` 的处理使用 `Jackson` 实现，如果有需求可自行修改或替换。