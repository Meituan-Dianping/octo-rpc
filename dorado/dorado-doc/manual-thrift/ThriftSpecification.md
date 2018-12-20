# 1. 说明
本文档大部分内容翻译自文章: [Thrift:The missing Guide](http://diwakergupta.github.io/thrift-missing-guide/)。

第一部分主要翻译自: http://wiki.apache.org/thrift/ThriftFeatures

## 1.1 关键特性
IDL  /  namespace /

基本类型 / 常量&枚举 / 容器 /

结构体 / 结构体的演化(升级) /

服务 / 服务继承 /

异步调用 / 自定义异常 /

### 1.2 不支持的特性
- 不支持多态 / 重载.

- 没有异构容器: 容器中元素的类型必须一样.

- 参数 & 返回值不能为 null(强烈建议不要将 null 赋予业务意义).

- 容器中元素不能为 null, 在插入容器前需要做检查， 另外建议将 struct 中 string 元素的默认值设置为 ""

- thrift 0.8 接口返回值为基本类型时，无法返回自定义异常。

# 2. 语法参考
## 2.1 Types
Thrift类型系统包括预定义基本类型，用户自定义结构体，容器类型，异常和服务定义

### 2.1.1 基本类型
```
bool：布尔类型(true or value)，占一个字节

byte：有符号字节

i16:16位有符号整型

i32:32位有符号整型

i64:64位有符号整型

double：64位浮点数

string：未知编码或者二进制的字符串

binary：二进制数据
```
> 注意，thrift不支持无符号整型，因为很多目标语言不存在无符号整型，如java。

### 2.1.2 容器类型
Thrift容器与类型密切相关，它与当前流行编程语言提供的容器类型相对应，采用java泛型风格表示的。Thrift提供了3种容器类型：

- list<t1>：一系列t1类型的元素组成的有序表，元素可以重复

- set<t1>：一系列t1类型的元素组成的无序表，元素唯一

- map<t1,t2>：key/value对（key的类型是t1且key唯一，value类型是t2）。

容器中的元素类型可以是除了service以外的任何合法thrift类型（包括结构体和异常）。

### 2.1.3 结构体和自定义异常
Thrift结构体在概念上同C语言结构体类型：一种将相关属性聚集（封装）在一起的方式。在面向对象语言中，thrift结构体被转换成类。

异常在语法和功能上类似于结构体，只不过异常使用关键字exception而不是struct关键字声明。但它在语义上不同于结构体——当定义一个RPC服务时，开发者可能需要声明一个远程方法抛出一个异常。

结构体和异常的声明将在下一节介绍。

### 2.1.4 服务
服务的定义方法在语法上等同于面向对象语言中定义接口。Thrift编译器会产生实现这些接口的client和server桩。具体参见下一节。

### 2.1.5 类型定义
Thrift支持C/C++风格的typedef:
```
typedef i32 MyInteger
typedef Tweet ReTweet
```
- 说明：
   - 末尾没有逗号
   - struct可以使用typedef

### 2.1.6 枚举类型
```
enum TweetType {

TWEET,

RETWEET = 2,

DM = 0xa,

REPLY

}

struct Tweet {

1: required i32 userId;

2: required string userName;

3: required string text;

4: optional Location loc;

5: optional TweetType tweetType = TweetType.TWEET

16: optional string language = "english"

}
```
- 说明：

   - a.  编译器默认从0开始赋值

   - b.  可以赋予某个常量某个整数

   - c.  允许常量是十六进制整数

   - d.  末尾没有逗号

   - e.  给常量赋缺省值时，使用常量的全称

- 注意，不同于protocol buffer，thrift不支持枚举类嵌套，枚举常量必须是32位的正整数

## 2.2 注释
支持shell注释风格，C++/Java语言中单行或者多行注释风格

```
# This is a valid comment.
/*
* This is a multi-line comment.
* Just like in C.
*/

// C++/Java style single-line comments work just as well.
```
## 2.3 命名空间
Thrift中的命名空间同C++中的namespace和java中的package类似，它们均提供了一种组织（隔离）代码的方式。因为每种语言均有自己的命名空间定义方式（如python中有module），thrift允许开发者针对特定语言定义namespace：
```
namespace cpp com.example.project
namespace java com.example.project
```
- 说明：

   - a. C++ 转换成成namespace com { namespace example { namespace project {

   - b. Java 转换成package com.example.project

## 2.4 文件包含
Thrift允许thrift文件包含，用户需要使用thrift文件名作为前缀访问被包含的对象，如：
```
include "tweet.thrift"

...

struct TweetSearchResult {

1: list<tweet.Tweet> tweets;

}
```
- 说明：

   - a. thrift文件名要用双引号包含，末尾没有逗号或者分号

   - b. 注意tweet前缀

## 2.5 常量
Thrift允许用户定义常量，复杂的类型和结构体可使用JSON形式表示。
```
const i32 INT_CONST = 1234;    // a
const map<string,string> MAP_CONST = {"hello": "world", "goodnight": "moon"}
```
- 说明：
   - a. 分号是可选的，可有可无；支持十六进制赋值。

## 2.6 定义结构体
结构体由一系列域组成，每个域有唯一整数标识符，类型，名字和可选的缺省参数组成。如：

```
struct Tweet {

1: required i32 userId;

2: required string userName;

3: required string text;

4: optional Location loc;

16: optional string language = "english"

}

struct Location {

1: required double latitude;

2: required double longitude;

}
 
exception MyException { //定义自定义异常
    1:string message;
```

- 说明：

   - a.  每个域有一个唯一的，正整数标识符

   - b.  每个域可以标识为required或者optional（也可以不注明, 默认 为default，具体含义可参考 Thrift 中 optional required 和 default Field 在 Java 中的含义）

   - c.  结构体可以包含其他结构体

   - d.  域可以有缺省值

   - e.  一个thrift中可定义多个结构体，并存在引用关系

规范的struct定义中的每个域均会使用required或者optional关键字进行标识。如果required标识的域没有赋值，thrift将提示错误。如果optional标识的域没有赋值，该域将不会被序列化传输。如果某个optional标识域有缺省值而用户没有重新赋值，则该域的值一直为缺省值。

与service不同，结构体不支持继承，即，一个结构体不能继承另一个结构体。

## 2.7 定义服务
在流行的序列化/反序列化框架（如protocol buffer）中，thrift是少有的提供多语言间RPC服务的框架。

Thrift编译器会根据选择的目标语言为server产生服务接口代码，为client产生桩代码。
```
//“Twitter”与“{”之间需要有空格！！！
service Twitter {

// 方法定义方式类似于C语言中的方式，它有一个返回值，一系列参数和可选的异常

// 列表. 注意，参数列表和异常列表定义方式与结构体中域定义方式一致.

void ping(),

bool postTweet(1:Tweet tweet);

TweetSearchResult searchTweets(1:string query);

// ”oneway”标识符表示client发出请求后不必等待回复（非阻塞）直接进行下面的操作，

// ”oneway”方法的返回值必须是void

oneway void zip()

void test() throws (1:GenericException genericException);//声明抛出自定义异常
}
```
- 说明：

   - a． 函数定义可以使用逗号或者分号标识结束

   - b． 参数可以是基本类型或者结构体，参数是只读的（const），不可以作为返回值！！！

   - c． 返回值可以是基本类型或者结构体

   - d． 返回值可以是void

注意，函数中参数列表的定义方式与struct完全一样

Service支持继承，一个service可使用extends关键字继承另一个service

# 3. 应用举例
## 3.1 定义一个IDL

```
enum TweetType {
    TWEET,
    RETWEET = 2,
    DM = 0xa,
    REPLY
}

const i32 DEFAULT_AGE = 18;

struct Location {
    1: required double latitude;
    2: required double longitude;
}

struct Tweet {
    1: required i32 userId;
    2: required string userName;
    3: required string text;
    4: optional Location loc;
    5: optional TweetType tweetType = TweetType.TWEET;
    16: optional i32 age = DEFAULT_AGE;
}

typedef list<Tweet> TweetList

struct TweetSearchResult {
    1: TweetList tweets;
}

exception TwitterUnavailable {
    1: string message;
}

service Twitter {

    //Base Type
    bool testBool(1:bool b);
    byte testByte(1:byte b);
    i16 testI16(1:i16 i);
    i32 testI32(1:i32 i);
    i64 testI64(1:i64 i);
    double testDouble(1:double d);
    binary testBinary(1:binary b);
    string testString(1:string s);

    //Containers
    list<string> testList(1:list<string> l);
    set<string> testSet(1:set<string> s);
    map<string, string> testMap(1:map<string, string> m);

    //Other
    void testVoid();
    string testReturnNull();
    TweetSearchResult testStruct(1:string query);
    string testException(1:Tweet tweet) throws (1:TwitterUnavailable unavailable);
}
```

## 3.2 通过IDL文件编译生成Java源码
在https://thrift.apache.org/tutorial/ 下载安装Thrift编译器 (Dorado扩展协议模块中使用的是0.9.3版本)
有两种方式可以生成Java代码
1. 命令方式
```
thrift --gen <language> <Thrift filename>
```
2. Maven 插件方式
```
<plugin>
    <groupId>org.apache.thrift.tools</groupId>
    <artifactId>maven-thrift-plugin</artifactId>
    <version>0.1.11</version>
    <configuration>
        <!-- 你的编译器位置 -->
        <thriftExecutable>/usr/local/bin/thrift</thriftExecutable>
    </configuration>
    <executions>
        <execution>
            <id>thrift-sources</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 3.3 Java语言
### 3.3.1 产生的文件
![ThriftFileExample](../img/ThriftFileExample.tiff)

### 3.3.2 类型对应
- bool: boolean

- byte: byte

- i16: short

- i32: int

- i64: long

- double: double

- string: String

- list<t1>: List<t1>

- set<t1>: Set<t1>

- map<t1,t2>: Map<t1, t2>

- Enum
Thrift直接将枚举类型映射成java的枚举类型。用户可以使用geValue方法获取枚举常量的值。此外，编译器会产生一个findByValue方法获取枚举对应的数值。

- 常量
Thrift把所有的常量放在一个叫Constants的public类中，每个常量修饰符是public static final。



# 3. Thrift兼容性
## 3.1 兼容情况
- 新增加optional字段

- 不改字段ID的情况下修改字段名

- 新增加方法

- 给方法增加可选参数、调整参数顺序

- 修改包名，不建议这么做（包名变更Thrift协议是兼容的，但框架中是会做校验的）

- 修改服务名，不建议这么做

- 服务端和调用端的Thrift版本不同，对通信无影响

## 3.2 不兼容情况
- 新增加required字段

- 删除required字段

- 修改字段ID，即使不改字段名

- 修改方法名称

- 删除方法参数、修改参数id

- 删除方法

- 编译源码版本和运行版本最好保持一致，高版本对低版本也存在不兼容情况

