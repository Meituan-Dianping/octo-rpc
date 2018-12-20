namespace cpp echo
namespace py echo
namespace java echo

enum TweetType {
TWEET,
RETWEET = 2,
DM = 0xa,
REPLY
}

struct test{
   1: required string arg,
   2: required double arg2,
   3: required list<string> arg3,
   4: required map<string, string> arg4,
   5: required bool arg5,
   6: required set<i64> arg6,
   7: required i32 arg7,
   8: required TweetType arg8,
}

service Echo
{
  string echo(1: string arg,2:test arg2);
}

