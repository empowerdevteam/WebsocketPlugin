package com.homecontrol;

public class SocketConfig {
    static  long retry_polling_interval = 5000;
    static  long retry_timeout_min = 1440 ;// 24*60 minutes
    static  int socket_close_code = 1000 ;
    static  String debug_message = "CordovaWebsocket";

}
