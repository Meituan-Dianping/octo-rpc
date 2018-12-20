namespace java com.meituan.dorado.test.thrift.exception.api2

struct EchoResult {
    1: string result;
    2: i32 id;
    3: string message;
}

service Echo {
    EchoResult echo(1:string message, 2:string param)
}