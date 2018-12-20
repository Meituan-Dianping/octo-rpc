namespace java com.meituan.dorado.test.thrift.api2

exception MyException {
1: string message
}

service TestService{
string testMock(1: string str)
i64 testLong(2: i64 n)
//协议异常
void testProtocolMisMatch()
//网络异常
void testTransportException()
void testTimeout()
//业务异常
string testReturnNull(); //
string testNull() // NPE
string testException() throws(1:MyException myException)
i32 testBaseTypeException() throws(1:MyException myException)
}
