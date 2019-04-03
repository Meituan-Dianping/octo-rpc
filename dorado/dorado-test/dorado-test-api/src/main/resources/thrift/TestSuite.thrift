namespace java com.meituan.dorado.test.thrift.apisuite

struct SubMessage {
    1:optional i64 id;
    2:optional string value;
}

struct Message {
    1:optional i64 id;
    2:optional string value;
    3:optional list<SubMessage> subMessages;
}

exception ExceptionA {
    1: string message
}

exception ExceptionB {
    1: string message
}

service TestSuite {
    void testVoid()
    string testString(1: string str)
    i64 testLong(2: i64 n)

    // 复杂参数
    Message testMessage(1:Message message);
    SubMessage testSubMessage(1:SubMessage message);
    map<Message, SubMessage> testMessageMap(3:map<Message, SubMessage> messages);
    list<Message> testMessageList(1:list<Message> messages);

    // 多参数
    SubMessage testStrMessage(1:string str, 2:SubMessage message);
    // 参数ID非顺序 且不连续
    map<string, string> multiParam(2:byte param1, 1:i32 param2, 3:i64 param3, 12:double param4);

    //协议异常
    void testMockProtocolException()
    void testTimeout()
    //业务异常
    string testReturnNull(); //
    string testNPE()
    string testIDLException() throws(1:ExceptionA exceptionA)
    string testMultiIDLException() throws(1:ExceptionA exceptionA, 3:ExceptionB exceptionB)
    i32 testBaseTypeReturnException() throws(1:ExceptionA exceptionA)
}
