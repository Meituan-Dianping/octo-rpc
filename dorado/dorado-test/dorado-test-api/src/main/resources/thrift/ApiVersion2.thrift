namespace java com.meituan.dorado.test.thrift.exception.api

struct Result2 {
    1: required string result;
    2: required i32 id;
}

service ApiVersion2 {
    Result2 send(1:string message, 2:string param)
}