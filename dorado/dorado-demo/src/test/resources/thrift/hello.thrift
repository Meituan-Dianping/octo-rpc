namespace java com.meituan.dorado.demo.thrift.api
service HelloService
{  
    string sayHello(1:string username)
    string sayBye(1:string username)
}
