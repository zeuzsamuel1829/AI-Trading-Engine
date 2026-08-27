#property strict
#property version   "1.0"

input string BridgeURL = "https://probable-space-rotary-phone-jrq4gxjwvx4g3pwv4-8080.app.github.dev/mt5/heartbeat";
input string BridgeToken = "DEMO-ZEUS-2026";
input int HeartbeatSeconds = 10;

datetime lastHeartbeat = 0;

string JsonEscape(string value)
{
   StringReplace(value, "\\", "\\\\");
   StringReplace(value, "\"", "\\\"");
   return value;
}

bool SendHeartbeat()
{
   string symbol = _Symbol;

   double bid = SymbolInfoDouble(symbol, SYMBOL_BID);
   double ask = SymbolInfoDouble(symbol, SYMBOL_ASK);

   string json =
      "{"
      "\"symbol\":\"" + JsonEscape(symbol) + "\","
      "\"bid\":" + DoubleToString(bid, _Digits) + ","
      "\"ask\":" + DoubleToString(ask, _Digits)
      "}";

   char post[];
   char result[];
   string headers =
      "Content-Type: application/json\r\n"
      "X-Bridge-Token: " + BridgeToken + "\r\n";

   StringToCharArray(json, post, 0, WHOLE_ARRAY, CP_UTF8);

   string responseHeaders;

   ResetLastError();

   int timeout = 5000;

   int code = WebRequest(
      "POST",
      BridgeURL,
      headers,
      timeout,
      post,
      result,
      responseHeaders
   );

   if(code == 200)
   {
      Print("ZEUS: heartbeat OK | ", symbol,
            " BID=", DoubleToString(bid, _Digits),
            " ASK=", DoubleToString(ask, _Digits));
      return true;
   }

   Print("ZEUS: heartbeat ERROR HTTP=", code,
         " MT5_ERROR=", GetLastError());

   return false;
}

int OnInit()
{
   Print("=================================");
   Print(" ZEUS MT5 DEMO EA");
   Print(" Orders: DISABLED");
   Print(" Heartbeat: ACTIVE");
   Print("=================================");

   EventSetTimer(HeartbeatSeconds);

   return(INIT_SUCCEEDED);
}

void OnDeinit(const int reason)
{
   EventKillTimer();
}

void OnTimer()
{
   SendHeartbeat();
}
