namespace java com.meituan.dorado.test.thrift.api
service HelloService
{  
    string sayHello(1:string username)
    string sayBye(1:string username)
}
