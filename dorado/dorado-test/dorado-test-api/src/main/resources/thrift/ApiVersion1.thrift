namespace java com.meituan.dorado.test.thrift.exception.api

struct Result1 {
    1: required string result;
    2: required i32 id;
    3: required string message;
}

service ApiVersion1 {
    Result1 send(1:string message, 2:string param)
}