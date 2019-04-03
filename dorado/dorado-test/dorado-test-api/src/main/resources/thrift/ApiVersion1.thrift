namespace java com.meituan.dorado.test.thrift.exception.api1

struct Result {
    1: string result;
    2: i32 id;
    3: string message;
}

service ApiVersion1 {
    Result send(1:string message, 2:string param)
}