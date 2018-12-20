namespace java com.meituan.dorado.test.thrift.apitwitter

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