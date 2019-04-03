namespace java com.meituan.dorado.test.thrift.exception.api2

struct Result {
    1: string result;
    2: i32 id;
}

service ApiVersion1 {
    Result send(1:string message, 2:string param)
}